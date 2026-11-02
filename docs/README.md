# [TaCZ] Traveler's Backpack Compat — Developer Wiki

Local architecture wiki. Small addon: ~6 source files, so docs stay flat.

## Pages

| Page | Covers |
|---|---|
| [architecture.md](architecture.md) | The three mixin hooks, ammo search order |
| [backpack-detection.md](backpack-detection.md) | `CapabilityUtils`, `BackpackWrapper`, the IAmmoBox flush rule |
| [config-debug.md](config-debug.md) | Config keys, version/dependency notes |

## Quick orientation

- **Mod id:** `tacz_tb_compat`, entry `TaczTbCompat.java`, version **1.0.3**
- **Platform:** Minecraft 1.20.1 / Forge 47.4.0 / Java 17
- **Goal:** let TaCZ guns see + consume ammo inside Traveler's Backpacks —
  worn (capability or Curios slot) or carried in the player inventory
- **Depends on:** TaCZ `[1.1.5,)` + `travelersbackpack` `[9.1.0,)` (both
  mandatory)
- **Build:** `cd 1.20.1-forge && gradlew build` →
  `build/libs/tacz_tb_compat-<version>.jar`
- **Repo:** github.com/Poseidon9991/taczxtravelerbackpack

## Sister project

[TaCZ × Fracture Point compat](https://github.com/Poseidon9991/taczxfracturepointbackpack)
— identical three-mixin design; only backpack discovery differs
(`CapabilityUtils`/`BackpackWrapper` here vs `BackpackItem` +
`ITEM_HANDLER` capability there).
