package com.poseidon.tacz_tb_compat.mixin;

import com.poseidon.tacz_tb_compat.service.BackpackAmmoService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;

// Per-round reload guns (pump shotguns, GLs, launchers) drive their animation
// from client-side scripts that call context:hasAmmoToConsume() on
// GunAnimationStateContext. That check scans only the camera entity's own
// inventory, so backpack ammo is invisible to it: the anim retreats from the
// load loop and freezes mid-motion while the server keeps feeding from the
// backpack. Same RETURN-OR pattern as the server-side mixins — adds the
// backpack scan so the client sees the same answer the server computed.
// String target keeps the client-only class unresolvable on a dedicated
// server, where it is never loaded and this mixin never applies.
@Mixin(targets = "com.tacz.guns.client.animation.statemachine.GunAnimationStateContext", remap = false)
public abstract class GunAnimationStateContextMixin {

    @Shadow(remap = false)
    private ItemStack currentGunItem;

    @Shadow(remap = false)
    private <T> Optional<T> processCameraEntity(Function<Entity, T> function) {
        return Optional.empty();
    }

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true)
    private void tacztb$animHasBackpackAmmo(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            return;
        }
        boolean found = processCameraEntity(entity ->
                entity instanceof LivingEntity living
                        && BackpackAmmoService.hasBackpackAmmo(living, this.currentGunItem))
                .orElse(false);
        if(found) {
            cir.setReturnValue(true);
        }
    }
}
