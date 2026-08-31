package com.enginecore;

import com.enginecore.client.gui.EngineCoreConfigScreen;
import com.enginecore.config.EngineCoreConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EngineCore.MOD_ID)
public class EngineCore {
    public static final String MOD_ID = "enginecore";
    public static final Logger LOGGER = LogManager.getLogger();

    public EngineCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing EngineCore for Minecraft 1.21 (NeoForge)...");

        modContainer.registerConfig(ModConfig.Type.COMMON, EngineCoreConfig.SPEC);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, 
                (container, screen) -> new EngineCoreConfigScreen(screen));
        }
    }
}
