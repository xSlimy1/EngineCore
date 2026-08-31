package com.enginecore.mixin;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    @Shadow private int age;

    /**
     * Reduces the excessive neighbor scanning rate for ground item merging.
     * Only allows merging searches every 10 ticks (twice a second) instead of every tick.
     */
    @Inject(method = "mergeWithNeighbours", at = @At("HEAD"), cancellable = true)
    private void enginecore$throttleItemMerge(CallbackInfo ci) {
        if (EngineCoreConfig.enableItemMergeOptimizer && (this.age % 10 != 0)) {
            ci.cancel();
        }
    }
}
