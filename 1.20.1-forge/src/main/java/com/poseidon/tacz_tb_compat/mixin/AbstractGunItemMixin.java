package com.poseidon.tacz_tb_compat.mixin;

import com.poseidon.tacz_tb_compat.service.BackpackAmmoService;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin {

    @Redirect(
            method = "canReload",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
                    remap = false)
    )
    private LazyOptional<IItemHandler> tacztb$canReloadAmmoView(LivingEntity shooter, Capability<IItemHandler> capability, Direction side) {
        LazyOptional<IItemHandler> original = shooter.getCapability(capability, side);
        return capability == ForgeCapabilities.ITEM_HANDLER
                ? BackpackAmmoService.wrapForAmmoQuery(original, shooter)
                : original;
    }

    @Redirect(
            method = "hasInventoryAmmo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
                    remap = false)
    )
    private LazyOptional<IItemHandler> tacztb$inventoryAmmoView(LivingEntity shooter, Capability<IItemHandler> capability, Direction side) {
        LazyOptional<IItemHandler> original = shooter.getCapability(capability, side);
        return capability == ForgeCapabilities.ITEM_HANDLER
                ? BackpackAmmoService.wrapForAmmoQuery(original, shooter)
                : original;
    }
}
