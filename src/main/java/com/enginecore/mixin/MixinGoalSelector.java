package com.enginecore.mixin;

import com.enginecore.core.IGoalSelectorThrottle;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

/**
 * Entity AI &amp; Dynamic Tick Throttling module - fine-grained companion to {@link MixinMob}.
 * <p>
 * {@code MixinMob} decides, once per tick per mob, how often this selector's {@code tick()} should
 * actually run ({@link #engineCore$setSkipInterval}); this class enforces that decision at the
 * selector level with one crucial extra safety check: a throttled tick is skipped ONLY when the
 * selector currently has no running goal. If any goal is active (fighting, fleeing, breeding,
 * eating...), its per-tick update must not stutter, so we always run in that case regardless of the
 * configured interval. This is what makes "idle" throttling in the spec's wording literal rather
 * than approximate.
 */
@Mixin(value = GoalSelector.class, priority = 1000)
public abstract class MixinGoalSelector implements IGoalSelectorThrottle {

    @Shadow
    public abstract Stream<WrappedGoal> getRunningGoals();

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

        // Never throttle while a goal is actively running - only genuinely idle selectors skip.
        if (this.getRunningGoals().findAny().isPresent()) {
            return;
        }

        engineCore$tickCounter++;
        if (engineCore$tickCounter % interval != 0) {
            ci.cancel();
        }
    }
}

