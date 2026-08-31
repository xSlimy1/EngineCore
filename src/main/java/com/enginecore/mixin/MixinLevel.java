package com.enginecore.mixin;

import com.enginecore.core.RedstoneUpdateBatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Level.class, priority = 1000)
public abstract class MixinLevel {

    @Inject(
        method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void engineCore$batchNeighborUpdate(BlockPos pos, Block sourceBlock, Orientation orientation, CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide() && RedstoneUpdateBatcher.queue(self, pos, sourceBlock)) {
            ci.cancel();
        }
    }
}
