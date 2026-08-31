package com.enginecore.core;

import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;

/**
 * Block &amp; Redstone Tick Batching module - hard safety list.
 * <p>
 * Every block here participates in same-tick, order-sensitive signal propagation: repeaters and
 * comparators ({@link DiodeBlock}) latch their output based on the exact tick a signal arrived,
 * pistons ({@link PistonBaseBlock}/{@link PistonHeadBlock}/{@link MovingPistonBlock}) rely on
 * precise update ordering for 0-tick behavior, {@link ObserverBlock} fires a pulse per distinct
 * state change rather than "at least one change happened", and {@link RedStoneWireBlock} strength
 * calculations are recomputed from neighbor state on every single notification. Coalescing repeated
 * updates to (or touching) any of these is a correctness bug, not just a missed optimization, so
 * {@link RedstoneUpdateBatcher} checks this list unconditionally and bypasses the queue entirely
 * whenever it matches - the {@code enableRedstoneBatching} toggle only controls whether the
 * "everything else" pathway (explosions, decay cascades, mass block breaks) gets batched at all.
 * <p>
 * Uses {@code instanceof} against vanilla's own base classes rather than a fixed {@code Block} set
 * so that modded blocks extending these same vanilla base classes (very common for custom redstone
 * components) are automatically covered too.
 */
public final class RedstoneCriticalBlocks {

    private RedstoneCriticalBlocks() {}

    public static boolean isCritical(Block block) {
        return block instanceof DiodeBlock
                || block instanceof PistonBaseBlock
                || block instanceof PistonHeadBlock
                || block instanceof MovingPistonBlock
                || block instanceof ObserverBlock
                || block instanceof RedStoneWireBlock
                || block instanceof RedstoneTorchBlock
                || block instanceof RedstoneLampBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof BasePressurePlateBlock
                || block instanceof TripWireBlock
                || block instanceof TripWireHookBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof TargetBlock;
    }
}
