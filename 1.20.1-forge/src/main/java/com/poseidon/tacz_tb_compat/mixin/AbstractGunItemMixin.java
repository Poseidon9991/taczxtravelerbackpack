package com.poseidon.tacz_tb_compat.mixin;

import com.poseidon.tacz_tb_compat.service.BackpackAmmoService;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// RETURN injects instead of @Redirect on getCapability: redirects are exclusive
// and any other backpack-compat mod (e.g. the Sophisticated Backpacks bridge)
// would consume the instruction first and hard-crash the game. Flipping a
// "false" return to "true" composes safely with any number of mods.
@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin {

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true)
    private void tacztb$canReloadFromBackpacks(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && BackpackAmmoService.hasBackpackAmmo(shooter, gunItem)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasInventoryAmmo", at = @At("RETURN"), cancellable = true)
    private void tacztb$hasBackpackInventoryAmmo(LivingEntity shooter, ItemStack gunItem, boolean allAmmo, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && BackpackAmmoService.hasBackpackAmmo(shooter, gunItem)) {
            cir.setReturnValue(true);
        }
    }
}
