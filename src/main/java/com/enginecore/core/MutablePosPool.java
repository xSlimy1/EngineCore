package com.enginecore.core;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Memory Allocation &amp; GC Optimizer module.
 * <p>
 * A per-thread pool of {@link net.minecraft.core.BlockPos.MutableBlockPos} instances. Vanilla and
 * many mods allocate a fresh {@code BlockPos} for every neighbor/collision/light check; at high
 * mob or redstone density this becomes a steady stream of small short-lived objects that drives
 * young-gen GC pause frequency up. Pooling and reusing mutable positions for scratch work (i.e.
 * work that never needs to outlive the current check) removes that allocation entirely.
 * <p>
 * Backed by {@link ThreadLocal} rather than a shared/synchronized pool: contention on a shared pool
 * under high concurrency would cost more than the allocations it saves, and the server's tick loop
 * plus any worker threads (chunk generation, pathfinding workers) each get their own independent,
 * lock-free deque. This satisfies the thread-safety requirement without any explicit locking.
 */
public final class MutablePosPool {

    private static final ThreadLocal<ArrayDeque<BlockPos.MutableBlockPos>> POOL =
            ThreadLocal.withInitial(MutablePosPool::seed);

    private MutablePosPool() {}

    private static ArrayDeque<BlockPos.MutableBlockPos> seed() {
        int size = Math.max(1, com.enginecore.config.EngineCoreConfig.mutablePosPoolSize);
        ArrayDeque<BlockPos.MutableBlockPos> deque = new ArrayDeque<>(size);
        for (int i = 0; i < size; i++) {
            deque.push(new BlockPos.MutableBlockPos());
        }
        return deque;
    }

    /** Borrow a pooled instance. Callers MUST call {@link #release(BlockPos.MutableBlockPos)} when done. */
    public static BlockPos.MutableBlockPos acquire() {
        BlockPos.MutableBlockPos pos = POOL.get().poll();
        return pos != null ? pos : new BlockPos.MutableBlockPos();
    }

    /** Return a previously-acquired instance to this thread's pool. */
    public static void release(BlockPos.MutableBlockPos pos) {
        ArrayDeque<BlockPos.MutableBlockPos> deque = POOL.get();
        int cap = com.enginecore.config.EngineCoreConfig.mutablePosPoolSize * 2;
        if (deque.size() < cap) {
            deque.push(pos);
        }
    }

    /**
     * Convenience wrapper guaranteeing release even if {@code action} throws - the preferred entry
     * point for one-off scratch usage (e.g. "check the block one below this position").
     */
    public static void withPooled(Consumer<BlockPos.MutableBlockPos> action) {
        BlockPos.MutableBlockPos pos = acquire();
        try {
            action.accept(pos);
        } finally {
            pos.set(0, 0, 0);
            release(pos);
        }
    }
}
