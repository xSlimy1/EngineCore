package com.enginecore.core;

/**
 * Implemented by {@link MixinGoalSelector}. Lets {@link MixinMob} push this tick's throttle
 * decision down into each of a mob's two {@code GoalSelector} instances ({@code goalSelector} and
 * {@code targetSelector}) without needing a separate accessor-mixin round trip.
 */
public interface IGoalSelectorThrottle {

    /** interval &lt;= 1 means "never skip" - i.e. re-evaluate every tick as vanilla does today. */
    void engineCore$setSkipInterval(int interval);
}
