package com.enginecore.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.phys.AABB;

import java.util.function.LongFunction;

/**
 * Memory Allocation &amp; GC Optimizer module.
 * <p>
 * Many collision/visibility checks in a single tick re-derive the exact same static-block AABB
 * (e.g. several entities standing near the same fence/wall/slab all query its collision shape in
 * the same tick). This cache lets callers avoid recomputing and reallocating that AABB more than
 * once per tick per position, keyed by {@link net.minecraft.core.BlockPos#asLong()} to avoid using
 * BlockPos itself (or a boxed Long) as a hash key.
 * <p>
 * Backed by a {@link ThreadLocal} fastutil map (fastutil ships as a transitive dependency of
 * Minecraft itself, so no extra dependency is introduced). Cleared once per server level tick by
 * {@code MixinServerLevel} so entries never leak stale results into the next tick - block state can
 * change tick-to-tick (pistons, redstone, explosions), so caching across ticks would be unsafe.
 */
public final class AabbScratchCache {

    private static final ThreadLocal<Long2ObjectMap<AABB>> CACHE =
            ThreadLocal.withInitial(Long2ObjectOpenHashMap::new);

    private AabbScratchCache() {}

    public static AABB getOrCompute(long packedBlockPos, LongFunction<AABB> supplier) {
        Long2ObjectMap<AABB> map = CACHE.get();
        AABB cached = map.get(packedBlockPos);
        if (cached != null) {
            return cached;
        }
        AABB computed = supplier.apply(packedBlockPos);
        map.put(packedBlockPos, computed);
        return computed;
    }

    /** Invoked once per server level tick (see {@code MixinServerLevel}); not for general use. */
    public static void clear() {
        CACHE.get().clear();
    }
}
