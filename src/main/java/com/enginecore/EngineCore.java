package com.enginecore;

import com.enginecore.client.gui.EngineCoreConfigScreen;
import com.enginecore.config.EngineCoreConfig;
import com.enginecore.core.RedstoneUpdateBatcher;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EngineCore.MODID)
public final class EngineCore {

    public static final String MODID = "enginecore";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public EngineCore() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EngineCoreConfig::bake);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EngineCoreConfig.SPEC);

        // GUI-Registrierung für den Client (sicher vor Dedicated-Server-Abstürzen)
        if (FMLEnvironment.dist.isClient()) {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new EngineCoreConfigScreen(screen))
            );
        }

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("EngineCore initialized - AI throttling, GC optimization and redstone batching are active.");
    }

    @SubscribeEvent
    public void onLevelTickEnd(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        RedstoneUpdateBatcher.flush((net.minecraft.world.level.Level) event.level);
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        RedstoneUpdateBatcher.discard((net.minecraft.world.level.Level) event.getLevel());
    }
}
