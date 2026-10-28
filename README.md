# [TaCZ] Traveler's Backpack Compatibility Addon

Lets **TaCZ (Timeless and Classics Zero)** guns detect and consume ammunition
stored inside **Traveler's Backpacks** — both the backpack worn on the player's
back and backpacks carried in the normal inventory.

Vanilla TaCZ only reads ammo through the player's item-handler capability
(hotbar + armor + offhand). Backpack contents are invisible to it, so a gun
reports "no ammo" even when a backpack is full of matching magazines. This mod
bridges the two so reloading just works.

## Requirements

| Dependency         | Version              |
|--------------------|----------------------|
| Minecraft          | 1.20.1               |
| Forge              | 47+                  |
| TaCZ               | 1.1.5 – 1.1.8+       |
| Traveler's Backpack| 9.1.x                |

## How it works

The mod hooks TaCZ's ammo flow with 4 mixins (inspired by the
[TaCZ × Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/tacz-sophisticated-backpacks-compatibility-addon)
addon):

**Ammo checks** — `canReload`, `hasInventoryAmmo`, `hasAmmoToConsume`

The `getCapability(ITEM_HANDLER)` call is redirected to a merged view of the
normal inventory **plus** every backpack's storage, so TaCZ sees backpack ammo
during reload/shoot checks.

**Ammo consumption** — `consumeAmmoFromPlayer`

TaCZ drains the normal inventory first. The remainder is then extracted from
backpacks using TaCZ's own `findAndExtractInventoryAmmo` (handles both `IAmmo`
stacks and `IAmmoBox` internal counts), and the returned total is topped up.

**Persistence** — extraction goes through Traveler's Backpack's
`StorageAccessWrapper`, so unsortable/locked slots are respected and changes
are written back to the backpack's NBT automatically. After consuming, all
slots are force-flushed because `IAmmoBox` count changes do not mark the
storage handler dirty on their own.

Guns that use virtual/dummy ammo (or skip ammo checks entirely, e.g. creative)
are never touched.

## Configuration

Server config: `config/tacz_tb_compat-server.toml` (or per-world
`saves/<world>/serverconfig/`)

```toml
enabled = true            # master switch
wornBackpack = true       # ammo from the backpack worn on the back
inventoryBackpacks = true # ammo from backpacks inside the player inventory
```

## Building

```sh
cd 1.20.1-forge
gradlew build
```

Output jar: `1.20.1-forge/build/libs/tacz_tb_compat-1.0.0.jar`

Compile-only mod jars live in `1.20.1-forge/libs/`:

- `tacz-1.20.1-1.1.8-hotfix2.jar`
- `travelersbackpack-forge-1.20.1-9.1.58.jar`

To support a different TaCZ version, drop its jar into `libs/` and update the
filename in `build.gradle`. All hooked methods (`canReload`,
`hasInventoryAmmo`, `hasAmmoToConsume`, `consumeAmmoFromPlayer`,
`findAndExtractInventoryAmmo`) are signature-identical across TaCZ
1.1.5 → 1.1.8-hotfix2.

## Layout

```
1.20.1-forge/src/main/java/com/poseidon/tacz_tb_compat/
├── TaczTbCompat.java                     # @Mod entrypoint, registers server config
├── config/TbCompatConfig.java            # ForgeConfigSpec (SERVER)
├── service/BackpackAmmoService.java      # finds backpacks, merged query, consumption
├── service/MergedAmmoView.java           # IItemHandler concat of inventory + backpacks
└── mixin/
    ├── AbstractGunItemMixin.java         # redirects in canReload + hasInventoryAmmo
    └── ModernKineticGunScriptAPIMixin.java # redirect in hasAmmoToConsume + consumption top-up
```

## Credits

- [TaCZ](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero-tacz) by MCModderAnchor
- [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack) by Tiviacz1337
- Design inspired by [TACZ-addon](https://github.com/Mafuyu404/TACZ-addon) (MIT)
