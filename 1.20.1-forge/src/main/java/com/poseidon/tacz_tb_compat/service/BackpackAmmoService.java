package com.poseidon.tacz_tb_compat.service;

import com.poseidon.tacz_tb_compat.config.TbCompatConfig;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

// Bridge between TaCZ's ammo flow and Traveler's Backpack storage.
//
// Query path (canReload / hasInventoryAmmo / hasAmmoToConsume): our mixins
// inject at RETURN and flip a "false" result to "true" when a backpack holds
// compatible ammo. This composes with other compat mods, unlike @Redirect on
// the capability lookup which is exclusive.
//
// Consume path (consumeAmmoFromPlayer): TaCZ drains the normal inventory first;
// our mixin then extracts the remaining amount from backpacks and bumps the
// returned count so reload math stays correct.
public final class BackpackAmmoService {

    private BackpackAmmoService() {
    }

    // Mirrors the slot scan inside TaCZ's canReload / hasAmmoToConsume lambdas:
    // any IAmmo stack matching the gun, or any IAmmoBox matching the gun.
    //
    // Read-only and safe on the client — LocalPlayerReload gates the reload
    // packet on a client-side canReload check, so the scan must work there or
    // reload never starts. Traveler's Backpack syncs the worn backpack (with
    // contents) to clients, and inventory stacks are already local.
    public static boolean hasBackpackAmmo(LivingEntity entity, ItemStack gunStack) {
        if(!(entity instanceof Player player) || !TbCompatConfig.isEnabled()) {
            return false;
        }
        for(BackpackAccess access : collectAccess(player)) {
            IItemHandler storage = access.storage();
            for(int i = 0; i < storage.getSlots(); i++) {
                ItemStack stack = storage.getStackInSlot(i);
                if(stack.isEmpty()) {
                    continue;
                }
                if(stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunStack, stack)) {
                    return true;
                }
                if(stack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunStack, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Guns that never extract real items (dummy/virtual ammo, or inventory-feed
    // guns when the shooter skips ammo checks) must not touch backpack storage.
    public static boolean usesVirtualAmmo(AbstractGunItem gun, ItemStack gunStack, LivingEntity shooter) {
        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        boolean needsAmmoCheck = operator != null && operator.needCheckAmmo();
        return (gun.useInventoryAmmo(gunStack) && !needsAmmoCheck) || gun.useDummyAmmo(gunStack);
    }

    public static int consumeFromBackpacks(Player player, AbstractGunItem gun, ItemStack gunStack, int remaining) {
        int consumed = 0;
        for(BackpackAccess access : collectAccess(player)) {
            if(remaining <= 0) {
                break;
            }
            int extracted = gun.findAndExtractInventoryAmmo(access.storage(), gunStack, remaining);
            if(extracted > 0) {
                // IAmmo stacks persist automatically through the handler's
                // onContentsChanged, but IAmmoBox edits the stack's NBT silently
                // without dirtying the handler — force a full per-slot write-back.
                access.flush();
                consumed += extracted;
                remaining -= extracted;
            }
        }
        return consumed;
    }

    private static List<BackpackAccess> collectAccess(Player player) {
        List<BackpackAccess> out = new ArrayList<>();

        if(TbCompatConfig.wornEnabled()) {
            CapabilityUtils.getCapability(player).ifPresent(cap -> {
                if(cap.hasBackpack()) {
                    BackpackWrapper wrapper = cap.getWrapper() != null ? cap.getWrapper() : BackpackWrapper.fromStack(cap.getBackpack());
                    out.add(new BackpackAccess(wrapper));
                }
            });
        }

        if(TbCompatConfig.inventoryEnabled()) {
            Inventory inventory = player.getInventory();
            for(int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if(stack.getItem() instanceof TravelersBackpackItem) {
                    out.add(new BackpackAccess(BackpackWrapper.fromStack(stack)));
                }
            }
        }

        return out;
    }

    private record BackpackAccess(BackpackWrapper wrapper) {

        IItemHandler storage() {
            return this.wrapper.getStorageForInputOutput();
        }

        void flush() {
            ItemStackHandler storage = this.wrapper.getStorage();
            for(int i = 0; i < storage.getSlots(); i++) {
                this.wrapper.setSlotChanged(i, storage.getStackInSlot(i), BackpackWrapper.STORAGE_ID);
            }
        }
    }
}
