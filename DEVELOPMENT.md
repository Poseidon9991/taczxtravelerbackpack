# Development Notes

Technical internals of the TaCZ × Traveler's Backpack ammo bridge. Read this
before modifying the mixins or porting to another version/loader.

## How TaCZ finds ammo (unmodded)

Every ammo query and mutation in TaCZ goes through the Forge item-handler
capability on the shooter:

```java
shooter.getCapability(ForgeCapabilities.ITEM_HANDLER)
```

For players this resolves to a view over `player.getInventory()` — backpacks
are never included. Four TaCZ methods matter:

| Method                                        | Class                        | Role                                    |
|-----------------------------------------------|------------------------------|-----------------------------------------|
| `canReload(LivingEntity, ItemStack)`          | `AbstractGunItem`            | reload gate — scans slots for ammo      |
| `hasInventoryAmmo(LivingEntity, ItemStack, boolean)` | `AbstractGunItem`      | inventory-feed gate — same scan         |
| `hasAmmoToConsume()`                          | `ModernKineticGunScriptAPI`  | script/reload check — same scan         |
| `consumeAmmoFromPlayer(int)`                  | `ModernKineticGunScriptAPI`  | actually extracts ammo from the handler |

The first three call `getCapability` exactly once each and scan the returned
`IItemHandler` with `getStackInSlot` + `IAmmo.isAmmoOfGun` /
`IAmmoBox.isAmmoBoxOfGun`. The fourth resolves the capability and extracts via
`findAndExtractInventoryAmmo(IItemHandler, ItemStack, int)` — a **public**
method we reuse directly for backpack extraction.

## Architecture

```
TaCZ method
    │
    ├─ query path ──────────────────────────────┐
    │  vanilla runs first, returns false        │  read-only
    │  @Inject RETURN → scan backpacks          │
    │  → setReturnValue(true) on a hit          │
    │                                           │
    └─ consume path ────────────────────────────┤
       consumeAmmoFromPlayer native run first   │  mutating
       @Inject RETURN → extract remainder       │
       from each backpack via TaCZ's own        │
       findAndExtractInventoryAmmo              │
```

### Query path — RETURN top-up (why not @Redirect)

**v1.0.0 used `@Redirect` on the `getCapability` invoke — that is a bug.** A
`@Redirect` is *exclusive*: the first mod to apply consumes the instruction
and later redirects find nothing to match (`(0/1) succeeded` →
`MixinTransformerError` at boot). With the TaCZ × Sophisticated Backpacks
addon (`taczaddon`) installed, both mods crashed each other.

`@Inject(at = @At("RETURN"))` composes: every mod can inspect and override the
return value without fighting for the instruction. So all three query methods
now:

1. let vanilla run (inventory-only answer), then
2. if it returned `false`, scan backpack storage with the *same* check TaCZ's
   lambdas use — `IAmmo.isAmmoOfGun` / `IAmmoBox.isAmmoBoxOfGun` — and flip
   the result to `true`.

`BackpackAmmoService.hasBackpackAmmo(entity, gunStack)` does that scan over
each backpack's `StorageAccessWrapper` (`getStorageForInputOutput()`), which
honors TB's unsortable-slot rules.

### Consume path — remainder top-up

`@Inject(at = @At("RETURN"), cancellable = true)` on
`consumeAmmoFromPlayer(int)`:

```java
int consumed  = cir.getReturnValueI();   // what vanilla inventory supplied
int remaining = neededAmount - consumed;
// guards: remaining>0, ServerPlayer, config on, !usesVirtualAmmo
int extra = BackpackAmmoService.consumeFromBackpacks(player, gun, gunStack, remaining);
cir.setReturnValue(consumed + extra);
```

Vanilla consumption always runs first and untouched — backpacks only pay the
shortfall, so we never double-consume. `consumeFromBackpacks` iterates the
sources (worn first, then inventory backpacks in slot order) and calls
**TaCZ's own** `findAndExtractInventoryAmmo` on each backpack storage —
meaning `IAmmo` stack extraction and `IAmmoBox` NBT draining behave exactly as
they do for the normal inventory.

### Virtual-ammo guard

Guns that never touch real items must not drain backpacks:

```java
(gun.useInventoryAmmo(stack) && !operator.needCheckAmmo()) || gun.useDummyAmmo(stack)
```

- `useInventoryAmmo` = inventory-feed ammo type (no reload; consume per shot).
  Those guns *do* take real items when `needCheckAmmo()` is true — so they get
  backpack support — but skip entirely when the shooter doesn't check ammo.
- `useDummyAmmo` = virtual pool on the gun itself, never item-backed.

Mirrors the reference implementation's `usesNativeVirtualAmmo`.

## Backpack source enumeration

`BackpackAmmoService.collectAccess(Player)` builds `BackpackAccess` records
per call (no caching — wrappers are cheap, and caching would risk stale
handlers after equip/slot moves):

1. **Worn** — `CapabilityUtils.getCapability(player)` → `ITravelersBackpack`;
   prefers `cap.getWrapper()` (the live wearable wrapper) falling back to
   `BackpackWrapper.fromStack(cap.getBackpack())`.
2. **Inventory** — scans `player.getInventory()` for
   `TravelersBackpackItem` stacks → `BackpackWrapper.fromStack(stack)`.

`fromStack` keeps the *same* `ItemStack` reference and loads its storage from
the stack's NBT — mutations land on the live stack/capability data, not a copy.

## NBT persistence

TB's storage handler persists automatically: `ItemStackHandler.onContentsChanged`
→ `BackpackWrapper.setSlotChanged` → `NbtHelper.update(stack, BACKPACK_CONTAINER, …)`
writes the slot into the backpack stack's tag.

**Edge case — ammo boxes.** `IAmmoBox` stores its round count in the box
stack's own NBT and `setAmmoCount` edits it *in place*. The handler never sees
a dirty slot, so no write-back happens. Fix: after any extraction,
`BackpackAccess.flush()` calls `setSlotChanged(i, stackInSlot(i), STORAGE_ID)`
for every slot, forcing the full storage state back into the backpack tag.
Without this, drained ammo-box counts would revert on world reload.

## Mixin conventions

- `remap = false` on `@Mixin`, `@At`, and `@Shadow` — every touched name is a
  mod-defined or Forge-defined identifier, identical in dev and production
  (`LivingEntity.getCapability` is a Forge-patched method with no SRG name).
  Without this the annotation processor fails: *"Unable to locate obfuscation
  mapping"*.
- Injected members use the `tacztb$` prefix.
- **Never `@Redirect` (or `@ModifyExpressionValue`/`@Overwrite`) on TaCZ's
  capability lookups** — other backpack-compat mods hook the same spots and
  exclusive redirects hard-crash whoever applies second. Prefer RETURN
  injects that OR-in our result.
- `defaultRequire: 1` in `tacz_tb_compat.mixins.json` — if TaCZ ever moves a
  hook point, the game crashes loudly at first use instead of silently losing
  backpack support.

## TaCZ version compatibility

All hooked methods and API calls were byte-verified (javap) to be identical
across **TaCZ 1.1.5 → 1.1.8-hotfix2**:

- `AbstractGunItem`: `canReload`, `hasInventoryAmmo`,
  `findAndExtractInventoryAmmo`, `useInventoryAmmo` — same signatures;
  exactly two `getCapability` call sites in the class.
- `ModernKineticGunScriptAPI`: `hasAmmoToConsume`, `consumeAmmoFromPlayer`,
  private fields `shooter`/`itemStack`/`abstractGunItem` — same.
- `IGun`/`IGunOperator`: `useDummyAmmo`, `needCheckAmmo`, `fromLivingEntity`.

To re-verify for a newer TaCZ, download the jar and run:

```sh
javap -classpath tacz.jar -c -p com.tacz.guns.api.item.gun.AbstractGunItem | grep getCapability
javap -classpath tacz.jar -p com.tacz.guns.item.ModernKineticGunScriptAPI | grep -E "consumeAmmoFromPlayer|hasAmmoToConsume|shooter|itemStack|abstractGunItem"
```

To compile against a different version, replace the jar in
`1.20.1-forge/libs/` and update the filename in `build.gradle`.

## Build layout

- `1.20.1-forge/` — the Gradle project (the repo root may later host a second
  loader/version subfolder)
- ForgeGradle 6 + Parchment `2023.08.20-1.20.1` + MixinGradle 0.7, Java 17
- `gradlew build` → `build/libs/tacz_tb_compat-<version>.jar` (reobf'd)
- `libs/` holds compile-only mod jars (TaCZ, Traveler's Backpack) — they are
  file dependencies, not resolved from a maven. `*.jar` is gitignored, so on a
  fresh clone download these into `1.20.1-forge/libs/`:
  - `tacz-1.20.1-1.1.8-hotfix2.jar` —
    https://modrinth.com/mod/timeless-and-classics-zero
  - `travelersbackpack-forge-1.20.1-9.1.58.jar` —
    https://modrinth.com/mod/travelersbackpack

## Gotchas

- `LazyOptional.map` returns `Optional`, **not** `LazyOptional` — use
  `lazyMap` to compose optionals (relevant if a merged handler view is ever
  reintroduced).
- `registerConfig` on `ModLoadingContext.get()` is deprecated in Forge 47 —
  take `FMLJavaModLoadingContext` in the mod constructor instead.
- Both dependencies are declared `mandatory` in `mods.toml` — this is a pure
  compat mod, so hard deps are intentional; no optional-facade needed.
- `BackpackWrapper.fromStack` writes size-initialization NBT into the stack
  on first wrap (idempotent, same as TB does on open).
- Server-side only for mutations: both the wrap and consume paths bail on
  `level().isClientSide`.
