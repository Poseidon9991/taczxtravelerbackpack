package com.poseidon.tacz_tb_compat.mixin;

import com.poseidon.tacz_tb_compat.config.TbCompatConfig;
import com.poseidon.tacz_tb_compat.service.BackpackAmmoService;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {

    @Shadow(remap = false)
    private LivingEntity shooter;

    @Shadow(remap = false)
    private ItemStack itemStack;

    @Shadow(remap = false)
    private AbstractGunItem abstractGunItem;

    @Redirect(
            method = "hasAmmoToConsume",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
                    remap = false)
    )
    private LazyOptional<IItemHandler> tacztb$hasAmmoView(LivingEntity entity, Capability<IItemHandler> capability, Direction side) {
        LazyOptional<IItemHandler> original = entity.getCapability(capability, side);
        return capability == ForgeCapabilities.ITEM_HANDLER
                ? BackpackAmmoService.wrapForAmmoQuery(original, entity)
                : original;
    }

    // TaCZ drains the player's own inventory first; if part of the request is
    // still unpaid, take the rest from Traveler's Backpack storage.
    @Inject(method = "consumeAmmoFromPlayer", at = @At("RETURN"), cancellable = true)
    private void tacztb$consumeBackpackRemainder(int neededAmount, CallbackInfoReturnable<Integer> cir) {
        int consumed = cir.getReturnValueI();
        int remaining = neededAmount - consumed;
        if(remaining <= 0) {
            return;
        }
        if(!(this.shooter instanceof Player player) || player.level().isClientSide || !TbCompatConfig.isEnabled()) {
            return;
        }
        if(BackpackAmmoService.usesVirtualAmmo(this.abstractGunItem, this.itemStack, this.shooter)) {
            return;
        }
        int extra = BackpackAmmoService.consumeFromBackpacks(player, this.abstractGunItem, this.itemStack, remaining);
        if(extra > 0) {
            cir.setReturnValue(consumed + extra);
        }
    }
}
