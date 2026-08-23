package com.enginecore.core;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block &amp; Redstone Tick Batching module.
 * <p>
 * Large non-redstone update floods (explosions, leaf/sand decay cascades, mass block breaks) can
 * trigger the same block position's {@code Level#updateNeighborsAt} many times within a single game
 * tick. EngineCore queues the (position, source block) pair and coalesces repeats within the same
 * tick into a single flush - later writers to the same position simply overwrite the queued source
 * block, since only the final state at flush time is observationally relevant to neighbors.
 * <p>
 * <b>Redstone safety:</b> that coalescing assumption is only true for order-insensitive blocks.
 * Genuine redstone/mechanical components depend on exact per-tick update ordering and count (0-tick
 * pistons, observer pulses, comparator/repeater latching, quasi-connectivity), so every call to
 * {@link #queue} is checked against {@link RedstoneCriticalBlocks#isCritical} for the source block,
 * the block currently at the target position, AND all six of that position's neighbors - if any of
 * those is a critical component, batching is refused and the caller must run vanilla behavior
 * immediately. Checking the neighbors (not just the position itself) matters because an
 * {@link net.minecraft.world.level.block.ObserverBlock} sitting next to - not on - the changing
 * position still needs to see every individual notification, not just "at least one happened".
 * <p>
 * Threading: vanilla ticks every dimension sequentially on a single server tick thread, so the
 * per-level {@link LinkedHashMap} queue is only ever touched from that one thread and needs no
 * internal locking. The outer {@link ConcurrentHashMap} keyed by level exists purely so unrelated
 * dimensions never contend on a shared structure and so queue creation is safe even if a caller
 * ever queues from an unexpected thread (fails safe by just not batching - see {@link #queue}).
 */
public final class RedstoneUpdateBatcher {

    private static final Map<Level, LinkedHashMap<Long, Block>> PENDING = new ConcurrentHashMap<>();

    /** Reentrancy guard: true only while {@link #flush} is replaying queued updates on this thread. */
    private static final ThreadLocal<Boolean> FLUSHING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private RedstoneUpdateBatcher() {}

    /**
     * Attempts to queue a neighbor update instead of letting it run immediately.
     *
     * @return true if the caller should suppress the vanilla update (it has been queued and will
     *         run at end-of-tick), false if the caller must run vanilla behavior immediately
     *         (batching disabled, a flush already in progress, or a critical redstone/mechanical
     *         component is involved - see class docs).
     */
    public static boolean queue(Level level, BlockPos pos, Block sourceBlock) {
        if (!EngineCoreConfig.enableRedstoneBatching || FLUSHING.get()) {
            return false;
        }

        if (touchesCriticalComponent(level, pos, sourceBlock)) {
            return false;
        }

        LinkedHashMap<Long, Block> pending = PENDING.computeIfAbsent(level, l -> new LinkedHashMap<>());

        if (pending.size() >= EngineCoreConfig.redstoneMaxBatchedUpdates) {
            // Safety valve: never let a single tick's backlog grow unbounded. Flush now, then
            // queue this update fresh into the newly-emptied map.
            flush(level);
            pending = PENDING.computeIfAbsent(level, l -> new LinkedHashMap<>());
        }

        pending.put(pos.asLong(), sourceBlock);
        return true;
    }

    /**
     * True if {@code sourceBlock}, the block currently occupying {@code pos}, or any of that
     * position's six neighbors is a redstone/mechanical component that must never have its update
     * ordering disturbed. Uses {@link MutablePosPool} for the neighbor scan so this check - which
     * runs on every single neighbor-update call in the game, batching enabled or not - allocates
     * nothing itself.
     */
    private static boolean touchesCriticalComponent(Level level, BlockPos pos, Block sourceBlock) {
        if (RedstoneCriticalBlocks.isCritical(sourceBlock)) {
            return true;
        }
        if (RedstoneCriticalBlocks.isCritical(level.getBlockState(pos).getBlock())) {
            return true;
        }

        BlockPos.MutableBlockPos cursor = MutablePosPool.acquire();
        try {
            for (Direction direction : Direction.values()) {
                cursor.setWithOffset(pos, direction);
                if (RedstoneCriticalBlocks.isCritical(level.getBlockState(cursor).getBlock())) {
                    return true;
                }
            }
            return false;
        } finally {
            MutablePosPool.release(cursor);
        }
    }

    /** Replays and clears every queued update for {@code level}. Safe to call with nothing queued. */
    public static void flush(Level level) {
        LinkedHashMap<Long, Block> pending = PENDING.remove(level);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        FLUSHING.set(Boolean.TRUE);
        try {
            for (Map.Entry<Long, Block> entry : pending.entrySet()) {
                level.updateNeighborsAt(BlockPos.of(entry.getKey()), entry.getValue());
            }
        } finally {
            FLUSHING.set(Boolean.FALSE);
        }
    }

    /** Drops any queued updates for a level without replaying them - used only on level unload. */
    public static void discard(Level level) {
        PENDING.remove(level);
    }
}
