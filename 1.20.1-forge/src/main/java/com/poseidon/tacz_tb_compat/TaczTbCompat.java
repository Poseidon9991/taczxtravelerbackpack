package com.poseidon.tacz_tb_compat;

import com.mojang.logging.LogUtils;
import com.poseidon.tacz_tb_compat.config.TbCompatConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TaczTbCompat.MOD_ID)
public class TaczTbCompat {
    public static final String MOD_ID = "tacz_tb_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczTbCompat(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, TbCompatConfig.SPEC);
    }
}
