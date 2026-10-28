package com.poseidon.tacz_tb_compat.service;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Read-mostly view that exposes several item handlers as one contiguous slot
// space. TaCZ only calls getStackInSlot on it during ammo checks; insert/extract
// are delegated defensively so the view stays safe if anything mutates through
// it (extraction lands on StorageAccessWrapper and persists to backpack NBT).
public class MergedAmmoView implements IItemHandler {

    private final List<IItemHandler> handlers;
    private final int[] offsets;
    private final int totalSlots;

    public MergedAmmoView(@Nullable IItemHandler first, List<IItemHandler> extra) {
        this.handlers = new ArrayList<>(extra.size() + 1);
        if(first != null) {
            this.handlers.add(first);
        }
        this.handlers.addAll(extra);

        this.offsets = new int[this.handlers.size()];
        int total = 0;
        for(int i = 0; i < this.handlers.size(); i++) {
            this.offsets[i] = total;
            total += this.handlers.get(i).getSlots();
        }
        this.totalSlots = total;
    }

    @Override
    public int getSlots() {
        return this.totalSlots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        int[] loc = locate(slot);
        return loc == null ? ItemStack.EMPTY : this.handlers.get(loc[0]).getStackInSlot(loc[1]);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        int[] loc = locate(slot);
        return loc == null ? stack : this.handlers.get(loc[0]).insertItem(loc[1], stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        int[] loc = locate(slot);
        return loc == null ? ItemStack.EMPTY : this.handlers.get(loc[0]).extractItem(loc[1], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int[] loc = locate(slot);
        return loc == null ? 0 : this.handlers.get(loc[0]).getSlotLimit(loc[1]);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        int[] loc = locate(slot);
        return loc != null && this.handlers.get(loc[0]).isItemValid(loc[1], stack);
    }

    private int @Nullable [] locate(int slot) {
        if(slot < 0 || slot >= this.totalSlots) {
            return null;
        }
        for(int i = this.offsets.length - 1; i >= 0; i--) {
            if(slot >= this.offsets[i]) {
                return new int[]{i, slot - this.offsets[i]};
            }
        }
        return null;
    }
}
