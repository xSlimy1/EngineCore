package com.enginecore.core;

/**
 * Implemented by {@link MixinEntity}. Exposes this tick's pooled ground-stability check (see
 * {@link com.enginecore.core.GroundStabilityCheck}) to other mixins - specifically {@link MixinMob},
 * which uses it to force a full AI tick when the ground under a throttled mob changed, rather than
 * duplicating the same pooled lookup a second time.
 */
public interface IGroundStabilityAware {

    /** True if the block this entity is standing on changed since the previous tick's check. */
    boolean engineCore$hasGroundChangedThisTick();
}
