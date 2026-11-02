# Config & Debugging

## Config — `TbCompatConfig` (SERVER)

`tacz_tb_compat-server.toml`:

| Key | Default | Effect |
|---|---|---|
| `enabled` | true | Master switch for the whole bridge |
| `wornBackpack` | true | Include the worn backpack (capability or Curios) |
| `inventoryBackpacks` | true | Include inventory-carried backpacks |

All getters return `true` while the spec isn't loaded (early-calls safe).

## Dependency notes

| Dep | Range | Why |
|---|---|---|
| `tacz` | `[1.1.5,)` | Mixin targets + `IGun`/`IAmmo`/`IAmmoBox`/`ModernKineticGunScriptAPI` — verified identical 1.1.5 → 1.1.8-hotfix2 |
| `travelersbackpack` | `[9.1.0,)` | `CapabilityUtils`, `BackpackWrapper`, `TravelersBackpackItem`, `STORAGE_ID` |

Both **mandatory** — the mod does nothing useful without either.

## Known failure modes

| Symptom | First suspect |
|---|---|
| Reload doesn't see backpack ammo | `enabled`/`wornBackpack` off, or backpack not wrapped (`CapabilityUtils` returning null → check curios vs capability worn state) |
| Ammo box contents restore on relog | `BackpackAccess.flush` not reached — only runs when `findAndExtractInventoryAmmo` extracts > 0 |
| HUD count wrong | `countBackpackAmmo` — creative boxes force 9999 like TaCZ's own path |
| Crash on startup with another backpack mod | Shouldn't happen by design (RETURN injects, no redirects) — if it does, report the target class |

## Build

- `cd 1.20.1-forge && gradlew build` (Java 17, ForgeGradle 6, `-Xmx3G`)
- libs (not committed): `tacz-1.20.1-1.1.8-hotfix2.jar`,
  `travelersbackpack-forge-1.20.1-9.1.58.jar`
- Current release: **1.0.3** (tag `v1.0.3`)
