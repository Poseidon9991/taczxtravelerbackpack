package com.poseidon.tacz_tb_compat.service;

import com.poseidon.tacz_tb_compat.config.TbCompatConfig;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

// Bridge between TaCZ's ammo flow and Traveler's Backpack storage.
//
// Query path (canReload / hasInventoryAmmo / hasAmmoToConsume): our mixins
// redirect the shooter.getCapability(ITEM_HANDLER) call so TaCZ sees a merged
// view of the normal inventory plus every available backpack storage.
//
// Consume path (consumeAmmoFromPlayer): TaCZ drains the normal inventory first;
// our mixin then extracts the remaining amount from backpacks and bumps the
// returned count so reload math stays correct.
public final class BackpackAmmoService {

    private BackpackAmmoService() {
    }

    // Guns that never extract real items (dummy/virtual ammo, or inventory-feed
    // guns when the shooter skips ammo checks) must not touch backpack storage.
    public static boolean usesVirtualAmmo(AbstractGunItem gun, ItemStack gunStack, LivingEntity shooter) {
        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        boolean needsAmmoCheck = operator != null && operator.needCheckAmmo();
        return (gun.useInventoryAmmo(gunStack) && !needsAmmoCheck) || gun.useDummyAmmo(gunStack);
    }

    public static LazyOptional<IItemHandler> wrapForAmmoQuery(LazyOptional<IItemHandler> original, LivingEntity entity) {
        if(!(entity instanceof Player player) || entity.level().isClientSide || !TbCompatConfig.isEnabled()) {
            return original;
        }

        List<IItemHandler> backpackHandlers = new ArrayList<>();
        for(BackpackAccess access : collectAccess(player)) {
            backpackHandlers.add(access.storage());
        }
        if(backpackHandlers.isEmpty()) {
            return original;
        }

        LazyOptional<IItemHandler> merged = original.lazyMap(vanilla -> new MergedAmmoView(vanilla, backpackHandlers));
        return merged.isPresent() ? merged : LazyOptional.of(() -> new MergedAmmoView(null, backpackHandlers));
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
