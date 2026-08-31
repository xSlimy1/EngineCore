package com.enginecore.mixin;

import com.enginecore.core.IGoalSelectorThrottle;
import com.enginecore.config.EngineCoreConfig;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Entity AI & Dynamic Tick Throttling module.
 */
@Mixin(value = GoalSelector.class, priority = 1000)
public abstract class MixinGoalSelector implements IGoalSelectorThrottle {

    @Shadow @Final
    private Set<WrappedGoal> availableGoals;

    @Unique
    private volatile int engineCore$skipInterval = 1;

    @Unique
    private int engineCore$tickCounter = 0;

    @Override
    @Unique
    public void engineCore$setSkipInterval(int interval) {
        this.engineCore$skipInterval = Math.max(1, interval);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void engineCore$throttleTick(CallbackInfo ci) {
        if (!EngineCoreConfig.enableAiThrottle) {
            return;
        }

        int interval = this.engineCore$skipInterval;
        if (interval <= 1) {
            return;
        }

        // Never throttle while any goal is running
        for (WrappedGoal goal : this.availableGoals) {
            if (goal.isRunning()) {
                return;
            }
        }

        engineCore$tickCounter++;
        if (engineCore$tickCounter % interval != 0) {
            ci.cancel();
        }
    }
}
