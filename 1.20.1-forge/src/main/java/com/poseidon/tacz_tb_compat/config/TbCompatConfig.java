package com.poseidon.tacz_tb_compat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TbCompatConfig {
    private TbCompatConfig() {
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for the Traveler's Backpack ammo bridge.",
                    "When true, TaCZ guns can see and consume ammo stored in Traveler's Backpacks.")
            .define("enabled", true);

    public static final ForgeConfigSpec.BooleanValue WORN_BACKPACK = BUILDER
            .comment("Allow guns to use ammo from the Traveler's Backpack worn on the player's back.")
            .define("wornBackpack", true);

    public static final ForgeConfigSpec.BooleanValue INVENTORY_BACKPACKS = BUILDER
            .comment("Allow guns to use ammo from Traveler's Backpacks carried inside the player inventory.")
            .define("inventoryBackpacks", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean isEnabled() {
        return !SPEC.isLoaded() || ENABLED.get();
    }

    public static boolean wornEnabled() {
        return !SPEC.isLoaded() || WORN_BACKPACK.get();
    }

    public static boolean inventoryEnabled() {
        return !SPEC.isLoaded() || INVENTORY_BACKPACKS.get();
    }
}
