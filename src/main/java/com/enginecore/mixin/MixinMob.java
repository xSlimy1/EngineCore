package com.enginecore.mixin;

import com.enginecore.core.IGoalSelectorThrottle;
import com.enginecore.core.IGroundStabilityAware;

import com.enginecore.config.EngineCoreConfig;
import com.enginecore.core.TickThrottleManager;
import com.enginecore.core.TickThrottleManager.ThrottleDecision;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity AI &amp; Dynamic Tick Throttling module - primary injection point.
 * <p>
 * Targets {@code Mob#serverAiStep()}, vanilla's per-tick AI-decision method (goal/target selector
 * ticking, navigation, and move/look/jump controls). This is intentionally NOT the same method as
 * {@code LivingEntity#tick()}/{@code aiStep()} physics handling - gravity, motion and collision
 * resolution live elsewhere and are never touched here, so a throttled mob still falls, floats and
 * collides correctly; only its *decision-making* is throttled.
 * <p>
 * Priority 1000 keeps this mixin's transform applied late enough to compose cleanly with
 * Sodium/Embeddium (client rendering - no method overlap), FerriteCore (memory layout - no overlap)
 * and ModernFix (loading-time - no overlap), while still running deterministically relative to any
 * other server-side AI mixins that might be present.
 * <p>
 * <b>Mapping note:</b> {@code Mob#serverAiStep()} (declared {@code protected final void}) and the
 * {@code final GoalSelector goalSelector}/{@code targetSelector} fields below were confirmed against
 * the 1.20.1 Mojmap method/field listing for {@code Mob} before writing this mixin.
 */
@Mixin(value = Mob.class, priority = 1000)
public abstract class MixinMob {

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Unique
    private int engineCore$aiTickCounter = 0;

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void engineCore$throttleServerAiStep(CallbackInfo ci) {
        if (!EngineCoreConfig.enableAiThrottle) {
            return;
        }

        Mob self = (Mob) (Object) this;
        engineCore$aiTickCounter++;

        ThrottleDecision decision = TickThrottleManager.decide(self, engineCore$aiTickCounter);

        // Always propagate the goal-selector throttle, even on ticks where the AI step itself is
        // NOT being cancelled - this is what lets MID/FAR-band mobs keep smooth navigation/movement
        // while still skipping expensive idle goal re-evaluation underneath it.
        ((IGoalSelectorThrottle) (Object) this.goalSelector).engineCore$setSkipInterval(decision.goalSkipInterval());
        ((IGoalSelectorThrottle) (Object) this.targetSelector).engineCore$setSkipInterval(decision.goalSkipInterval());

        // Correctness safety net: MixinEntity already computed this tick's pooled ground-stability
        // check as part of Entity#checkSupportingBlock (see that class). If the block underneath a
        // mob that was about to be throttled just changed - broken, placed, pushed by a piston -
        // force a full tick immediately instead of waiting out the skip interval, so a throttled
        // mob's world-support state can never silently desync from what the world actually looks
        // like for more than a single tick.
        if (EngineCoreConfig.enableGcOptimizer && decision.skipAiStep()) {
            boolean groundChanged = ((IGroundStabilityAware) (Object) this).engineCore$hasGroundChangedThisTick();
            if (groundChanged) {
                decision = new ThrottleDecision(decision.goalSkipInterval(), false);
            }
        }

        if (decision.skipAiStep()) {
            ci.cancel();
        }
    }
}

