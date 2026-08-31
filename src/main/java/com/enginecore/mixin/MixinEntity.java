package com.enginecore.mixin;

import com.enginecore.core.IGroundStabilityAware;

import com.enginecore.config.EngineCoreConfig;
import com.enginecore.core.GroundStabilityCheck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 1000)
public abstract class MixinEntity implements IGroundStabilityAware {

    @Unique
    private long engineCore$lastGroundPos = Long.MIN_VALUE;

    @Unique
    private Block engineCore$lastGroundBlock = null;

    @Unique
    private boolean engineCore$groundChangedThisTick = false;

    @Inject(method = "checkSupportingBlock", at = @At("HEAD"), require = 0)
    private void engineCore$trackGroundStability(boolean onGround, Vec3 movement, CallbackInfo ci) {
        if (!EngineCoreConfig.enableGcOptimizer) {
            engineCore$groundChangedThisTick = false;
            return;
        }

        Entity self = (Entity) (Object) this;
        GroundStabilityCheck.Result result =
                GroundStabilityCheck.check(self, engineCore$lastGroundPos, engineCore$lastGroundBlock);

        engineCore$lastGroundPos = result.packedPos();
        engineCore$lastGroundBlock = result.block();
        engineCore$groundChangedThisTick = result.changed();
    }

    @Override
    public boolean engineCore$hasGroundChangedThisTick() {
        return engineCore$groundChangedThisTick;
    }
}

