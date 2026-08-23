package com.enginecore.mixin;

import com.enginecore.config.EngineCoreConfig;
import com.enginecore.core.AabbScratchCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Memory Allocation &amp; GC Optimizer module.
 * <p>
 * {@code ServerLevel#tick(BooleanSupplier)} is the single per-dimension entry point that drives
 * entity, block-entity and chunk ticking for that level each game tick. We use it purely as a tick
 * boundary: at {@code HEAD} we clear this thread's {@link AabbScratchCache}, so cached bounding
 * boxes from the previous tick can never leak into the one that's about to run (block state can
 * change every tick, so a stale cached AABB would be a correctness bug, not just a missed
 * optimization). Everything in between two resets is free to populate and reuse the cache.
 * <p>
 * <b>Mapping note:</b> written by hand against 1.20.1 Mojang mappings; verify {@code tick}'s exact
 * descriptor (it is overloaded - the one taking a single {@code BooleanSupplier}) against your local
 * MCPConfig/Mojmap export before shipping to production.
 */
@Mixin(value = ServerLevel.class, priority = 1000)
public abstract class MixinServerLevel {

    @Inject(method = "tick", at = @At("HEAD"))
    private void engineCore$resetPerTickCaches(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (EngineCoreConfig.enableGcOptimizer) {
            AabbScratchCache.clear();
        }
    }
}
