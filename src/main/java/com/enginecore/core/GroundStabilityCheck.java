package com.enginecore.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GroundStabilityCheck {

    public record Result(long packedPos, Block block, boolean changed) {}

    private static final AABB EMPTY_AABB = new AABB(0, 0, 0, 0, 0, 0);

    private GroundStabilityCheck() {}

    public static Result check(Entity entity, long lastPackedPos, Block lastBlock) {
        if (entity == null || entity.level() == null) {
            return new Result(lastPackedPos, lastBlock, false);
        }

        Level level = entity.level();
        BlockPos currentPos = entity.getOnPos();
        long currentPackedPos = currentPos.asLong();

        BlockState state = level.getBlockState(currentPos);
        Block currentBlock = state.getBlock();

        // If block and position have not changed, return early
        if (currentPackedPos == lastPackedPos && currentBlock == lastBlock) {
            return new Result(lastPackedPos, lastBlock, false);
        }

        // Resolve collision AABB via scratch cache with empty shape guard
        AabbScratchCache.getOrCompute(currentPackedPos, key -> {
            VoxelShape shape = state.getCollisionShape(level, currentPos);
            if (shape.isEmpty()) {
                return EMPTY_AABB;
            }
            return shape.bounds();
        });

        boolean changed = (lastBlock != null && currentBlock != lastBlock);
        return new Result(currentPackedPos, currentBlock, changed);
    }
}
