package com.enginecore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EngineCoreConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_AI_THROTTLE;
    public static final ModConfigSpec.BooleanValue ENABLE_GC_OPTIMIZER;
    public static final ModConfigSpec.BooleanValue ENABLE_REDSTONE_BATCHING;
    public static final ModConfigSpec.BooleanValue ENABLE_ITEM_MERGE_OPTIMIZER;
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMAL_OPTIMIZER;
    public static final ModConfigSpec.BooleanValue ENABLE_LIGHT_DAMPENER;

    public static final ModConfigSpec.DoubleValue AI_NEAR_DISTANCE;
    public static final ModConfigSpec.DoubleValue AI_MID_DISTANCE;
    public static final ModConfigSpec.DoubleValue AI_FAR_DISTANCE;
    public static final ModConfigSpec.IntValue AI_MID_SKIP_INTERVAL;
    public static final ModConfigSpec.IntValue AI_FAR_SKIP_INTERVAL;
    public static final ModConfigSpec.IntValue AI_VERY_FAR_SKIP_INTERVAL;
    public static final ModConfigSpec.BooleanValue AI_EXEMPT_CUSTOM_NAMED;
    public static final ModConfigSpec.BooleanValue AI_EXEMPT_VEHICLE_PASSENGERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AI_BLACKLIST;

    public static final ModConfigSpec.IntValue REDSTONE_MAX_BATCHED_UPDATES;
    public static final ModConfigSpec.IntValue MUTABLE_POS_POOL_SIZE;

    // Hot-path mirrors
    public static volatile boolean enableAiThrottle = true;
    public static volatile boolean enableGcOptimizer = true;
    public static volatile boolean enableRedstoneBatching = false;
    public static volatile boolean enableItemMergeOptimizer = true;
    public static volatile boolean enableAnimalOptimizer = true;
    public static volatile boolean enableLightDampener = true;

    public static volatile double aiNearDistance = 16.0D;
    public static volatile double aiMidDistance = 32.0D;
    public static volatile double aiFarDistance = 48.0D;
    public static volatile int aiMidSkipInterval = 2;
    public static volatile int aiFarSkipInterval = 4;
    public static volatile int aiVeryFarSkipInterval = 8;
    public static volatile boolean aiExemptCustomNamed = true;
    public static volatile boolean aiExemptVehiclePassengers = true;
    private static volatile Set<String> aiBlacklistSet = Collections.emptySet();

    public static volatile int redstoneMaxBatchedUpdates = 512;
    public static volatile int mutablePosPoolSize = 8;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("EngineCore Master Switches").push("general");

        ENABLE_AI_THROTTLE = builder
                .comment("Enable dynamic tick-frequency scaling for distant/passive/idle mob AI.")
                .define("enableAiThrottle", true);

        ENABLE_GC_OPTIMIZER = builder
                .comment("Enable pooled BlockPos/AABB scratch caches to reduce GC pressure in hot loops.")
                .define("enableGcOptimizer", true);

        ENABLE_REDSTONE_BATCHING = builder
                .comment("Enable coalescing of redundant neighbor-update calls within the same game tick.")
                .define("enableRedstoneBatching", false);

        ENABLE_ITEM_MERGE_OPTIMIZER = builder
                .comment("Throttle redundant item-entity merging scans on the ground to reduce drop lag.")
                .define("enableItemMergeOptimizer", true);

        ENABLE_ANIMAL_OPTIMIZER = builder
                .comment("Optimize idle farm animal goal processing when not actively breeding.")
                .define("enableAnimalOptimizer", true);

        ENABLE_LIGHT_DAMPENER = builder
                .comment("Reduces redundant lighting engine recalculations on rapid block toggles.")
                .define("enableLightDampener", true);

        builder.pop();

        builder.comment("Entity AI & Dynamic Tick Throttling").push("aiThrottle");

        AI_NEAR_DISTANCE = builder
                .comment("Radius (blocks) within which mobs always tick at full AI rate.")
                .defineInRange("nearDistance", 16.0D, 0.0D, 256.0D);

        AI_MID_DISTANCE = builder
                .comment("Radius (blocks) for the medium throttle band.")
                .defineInRange("midDistance", 32.0D, 0.0D, 256.0D);

        AI_FAR_DISTANCE = builder
                .comment("Radius (blocks) beyond which mobs enter the heaviest throttle band.")
                .defineInRange("farDistance", 48.0D, 0.0D, 512.0D);

        AI_MID_SKIP_INTERVAL = builder
                .comment("Only 1 in N idle goal re-evaluations run per tick in MID band.")
                .defineInRange("midSkipInterval", 2, 1, 100);

        AI_FAR_SKIP_INTERVAL = builder
                .comment("Only 1 in N idle goal re-evaluations run per tick in FAR band.")
                .defineInRange("farSkipInterval", 4, 1, 100);

        AI_VERY_FAR_SKIP_INTERVAL = builder
                .comment("Only 1 in N full AI steps run per tick in VERY_FAR band.")
                .defineInRange("veryFarSkipInterval", 8, 1, 200);

        AI_EXEMPT_CUSTOM_NAMED = builder
                .comment("Never throttle mobs with a custom name.")
                .define("exemptCustomNamed", true);

        AI_EXEMPT_VEHICLE_PASSENGERS = builder
                .comment("Never throttle mobs riding an active vehicle.")
                .define("exemptVehiclePassengers", true);

        AI_BLACKLIST = builder
                .comment("List of entity registry names that will NEVER be throttled by EngineCore.")
                .defineListAllowEmpty("blacklist", List.of("minecraft:warden", "minecraft:wither"), o -> o instanceof String);

        builder.pop();

        builder.comment("Block & Redstone Tick Batching").push("redstone");

        REDSTONE_MAX_BATCHED_UPDATES = builder
                .comment("Safety valve: maximum queued updates per tick.")
                .defineInRange("maxBatchedUpdates", 512, 16, 65536);

        builder.pop();

        builder.comment("Memory Allocation & GC Optimizer").push("memory");

        MUTABLE_POS_POOL_SIZE = builder
                .comment("Number of pooled MutableBlockPos instances kept per thread.")
                .defineInRange("mutablePosPoolSize", 8, 1, 64);

        builder.pop();

        SPEC = builder.build();
    }

    private EngineCoreConfig() {}

    public static boolean isBlacklisted(String registryName) {
        return aiBlacklistSet.contains(registryName);
    }

    public static void bake(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        enableAiThrottle = ENABLE_AI_THROTTLE.get();
        enableGcOptimizer = ENABLE_GC_OPTIMIZER.get();
        enableRedstoneBatching = ENABLE_REDSTONE_BATCHING.get();
        enableItemMergeOptimizer = ENABLE_ITEM_MERGE_OPTIMIZER.get();
        enableAnimalOptimizer = ENABLE_ANIMAL_OPTIMIZER.get();
        enableLightDampener = ENABLE_LIGHT_DAMPENER.get();

        aiNearDistance = AI_NEAR_DISTANCE.get();
        aiMidDistance = Math.max(aiNearDistance, AI_MID_DISTANCE.get());
        aiFarDistance = Math.max(aiMidDistance, AI_FAR_DISTANCE.get());

        aiMidSkipInterval = AI_MID_SKIP_INTERVAL.get();
        aiFarSkipInterval = AI_FAR_SKIP_INTERVAL.get();
        aiVeryFarSkipInterval = AI_VERY_FAR_SKIP_INTERVAL.get();
        aiExemptCustomNamed = AI_EXEMPT_CUSTOM_NAMED.get();
        aiExemptVehiclePassengers = AI_EXEMPT_VEHICLE_PASSENGERS.get();
        aiBlacklistSet = new HashSet<>(AI_BLACKLIST.get());

        redstoneMaxBatchedUpdates = REDSTONE_MAX_BATCHED_UPDATES.get();
        mutablePosPoolSize = MUTABLE_POS_POOL_SIZE.get();
    }
}
