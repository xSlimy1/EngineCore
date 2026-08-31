package com.enginecore.mixin;

import com.enginecore.config.EngineCoreConfig;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class MixinAnimal {

    @Shadow public abstract boolean isInLove();

    /**
     * Throttles ambient mating & breeding searches if the animal is not currently in love mode.
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void enginecore$throttlePassiveAnimals(CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        if (EngineCoreConfig.enableAnimalOptimizer && !this.isInLove()) {
            // Only tick base breeding/passive searches every 4th tick for idle farm animals
            if (animal.tickCount % 4 != 0 && animal.getTarget() == null) {
                ci.cancel();
            }
        }
    }
}
