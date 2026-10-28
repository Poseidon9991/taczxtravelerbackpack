# [TaCZ] Traveler's Backpack Compatibility Addon

Use the ammo stored inside your **Traveler's Backpack** with **TaCZ** guns —
no more "empty magazine" when your backpack is full of bullets.

## What it does

Vanilla TaCZ only searches your hotbar, armor, and offhand when checking for
ammo. With this mod installed:

- Reloading and firing checks can **see** ammo inside your backpacks
- When a reload needs more ammo than your inventory has, the missing rounds
  are **pulled directly out of your backpacks**
- Works for regular ammo items **and** ammo boxes (their stored counts are
  drained correctly)
- Locked/unsortable backpack slots are respected — ammo in a slot you marked
  as unsortable is never taken

## Ammo search order

When a gun consumes ammo, it takes from your sources in this order:

1. Your normal player inventory (hotbar, armor, offhand) — TaCZ's native behavior
2. The backpack worn on your back
3. Backpacks carried inside your inventory, in slot order

Your own inventory is always used first; backpacks only cover the rest.

## Requirements

| Dependency          | Version        |
|---------------------|----------------|
| Minecraft           | 1.20.1         |
| Forge               | 47+            |
| TaCZ                | 1.1.5 – 1.1.8+ |
| Traveler's Backpack | 9.1.x          |

Install: drop the jar into your `mods` folder together with TaCZ and
Traveler's Backpack. Required on the **server**; clients should also have it
for consistent behavior.

## Configuration

File: `config/tacz_tb_compat-server.toml` (global) or
`saves/<world>/serverconfig/tacz_tb_compat-server.toml` (per world)

```toml
enabled = true            # turn the whole feature on/off
wornBackpack = true       # use ammo from the backpack worn on the back
inventoryBackpacks = true # use ammo from backpacks inside the inventory
```

Changes apply live on config reload — no restart needed on dedicated servers.

## Notes

- Creative-mode / unlimited-ammo guns are untouched — they never consume from
  backpacks
- Guns with "dummy" (virtual) ammo pools are also untouched
- Works on dedicated servers and singleplayer

## Credits

- [TaCZ](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero-tacz) by MCModderAnchor
- [Traveler's Backpack](https://www.curseforge.com/minecraft/mc-mods/travelers-backpack) by Tiviacz1337
