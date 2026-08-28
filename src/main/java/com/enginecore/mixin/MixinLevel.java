package com.enginecore.mixin;

import com.enginecore.core.RedstoneUpdateBatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block &amp; Redstone Tick Batching module.
 * <p>
 * {@code updateNeighborsAt(BlockPos, Block)} is declared directly on {@code Level} (confirmed
 * against the 1.20.1 Mojmap method listing - concretely implemented there, shared by both
 * {@code ServerLevel} and {@code ClientLevel}, not overridden per-side), which is why this mixin
 * targets {@code Level} rather than {@code ServerLevel} - injecting into a method that is only
 * ever inherited-and-not-overridden on a subclass would silently fail to apply.
 * <p>
 * Every call is handed to {@link RedstoneUpdateBatcher#queue}, which - after checking the
 * redstone-safety guard documented there - either queues the update for a single end-of-tick flush
 * (see {@code EngineCore#onLevelTickEnd}) or returns {@code false} and lets vanilla behavior run
 * immediately, unchanged. Client-side levels are explicitly excluded from batching entirely: their
 * neighbor updates only drive rendering/prediction rather than authoritative game state, so there is
 * nothing to gain from delaying them and doing so would only add a frame of input latency.
 */
@Mixin(value = Level.class, priority = 1000)
public abstract class MixinLevel {

    @Inject(method = "updateNeighborsAt", at = @At("HEAD"), cancellable = true)
    private void engineCore$batchNeighborUpdate(BlockPos pos, Block sourceBlock, CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide() && RedstoneUpdateBatcher.queue(self, pos, sourceBlock)) {
            ci.cancel();
        }
    }
}
