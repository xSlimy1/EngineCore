package com.enginecore.core;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;

public final class TickThrottleManager {

    public record ThrottleDecision(int goalSkipInterval, boolean skipAiStep) {}

    private static final ThrottleDecision FULL_TICK = new ThrottleDecision(1, false);

    private TickThrottleManager() {}

    public static ThrottleDecision decide(Mob mob, int aiTickCounter) {
        if (!EngineCoreConfig.enableAiThrottle || mob == null || mob.level() == null || mob.level().isClientSide()) {
            return FULL_TICK;
        }

        // Hard exemptions: custom name, active passengers, boss/target status
        if (EngineCoreConfig.aiExemptCustomNamed && mob.hasCustomName()) {
            return FULL_TICK;
        }
        if (EngineCoreConfig.aiExemptVehiclePassengers && (mob.isPassenger() || mob.isVehicle())) {
            return FULL_TICK;
        }
        if (!mob.canUsePortal(false) || mob.getTarget() != null) {
            return FULL_TICK;
        }

        // Configurable entity type blacklist check
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (entityId != null && EngineCoreConfig.isBlacklisted(entityId.toString())) {
            return FULL_TICK;
        }

        Player nearestPlayer = mob.level().getNearestPlayer(mob, EngineCoreConfig.aiFarDistance + 16.0D);
        if (nearestPlayer == null) {
            boolean skip = (aiTickCounter % EngineCoreConfig.aiVeryFarSkipInterval != 0);
            return new ThrottleDecision(EngineCoreConfig.aiVeryFarSkipInterval, skip);
        }

        double distance = mob.distanceTo(nearestPlayer);

        if (distance <= EngineCoreConfig.aiNearDistance) {
            return FULL_TICK;
        } else if (distance <= EngineCoreConfig.aiMidDistance) {
            return new ThrottleDecision(EngineCoreConfig.aiMidSkipInterval, false);
        } else if (distance <= EngineCoreConfig.aiFarDistance) {
            return new ThrottleDecision(EngineCoreConfig.aiFarSkipInterval, false);
        } else {
            boolean skip = (aiTickCounter % EngineCoreConfig.aiVeryFarSkipInterval != 0);
            return new ThrottleDecision(EngineCoreConfig.aiVeryFarSkipInterval, skip);
        }
    }
}
