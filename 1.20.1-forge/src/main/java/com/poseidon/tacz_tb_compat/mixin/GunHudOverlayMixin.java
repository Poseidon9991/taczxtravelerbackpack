package com.poseidon.tacz_tb_compat.mixin;

import com.poseidon.tacz_tb_compat.service.BackpackAmmoService;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunHudOverlay.class, remap = false)
public abstract class GunHudOverlayMixin {

    @Shadow(remap = false)
    private static int cacheInventoryAmmoCount;

    // After TaCZ finishes counting ammo in the player inventory, add the ammo
    // found in Traveler's Backpack storage so the HUD reserve number matches
    // what the gun can actually consume.
    @Inject(method = "handleInventoryAmmo", at = @At("RETURN"))
    private static void tacztb$countBackpackAmmo(ItemStack gunStack, Inventory inventory, CallbackInfo ci) {
        cacheInventoryAmmoCount = BackpackAmmoService.countBackpackAmmo(inventory.player, gunStack, cacheInventoryAmmoCount);
    }
}
