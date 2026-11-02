# Backpack Detection (Traveler's Backpack internals)

## `CapabilityUtils` — the right entry point

`CapabilityUtils.getBackpackWrapper(player)` covers **both** worn states:
the Curios slot when TB's curios integration is active, and the
`ITravelersBackpack` capability otherwise. Reading the capability directly
misses curios-equipped backpacks entirely — always go through the wrapper.

Fallback for edge cases: `CapabilityUtils.getWearingBackpack(player)` →
stack → `BackpackWrapper.fromStack(stack)`.

## `BackpackAccess` — wrapper + flush

```java
IItemHandler storage() { return wrapper.getStorageForInputOutput(); }
void flush() {
    ItemStackHandler s = wrapper.getStorage();
    for (int i = 0; i < s.getSlots(); i++)
        wrapper.setSlotChanged(i, s.getStackInSlot(i), BackpackWrapper.STORAGE_ID);
}
```

The flush is **load-bearing**: `findAndExtractInventoryAmmo` mutates stacks
in place. `IAmmo` counts dirty the handler naturally, but `IAmmoBox` edits
its count inside the stack's **NBT silently** — the handler's
`onContentsChanged` never fires. Without the per-slot `setSlotChanged`
write-back, drained ammo-box contents never persist (silent ammo dupe).

## Search order (`collectAccess`)

1. Player inventory (TaCZ's native drain — always first)
2. Worn backpack via `CapabilityUtils` (covers Curios + capability worn)
3. `TravelersBackpackItem` stacks in the player inventory, slot order

`BackpackWrapper` instances are cheap views over the same stack — safe to
re-resolve per call.
