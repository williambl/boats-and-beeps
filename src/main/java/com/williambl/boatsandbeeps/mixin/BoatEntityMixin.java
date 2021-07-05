package com.williambl.boatsandbeeps.mixin;

import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatEntity.class)
public class BoatEntityMixin {
    @Shadow private float velocityDecay;

    @Redirect(method = "pushAwayFrom", at=@At(value = "CONSTANT", args = "classValue=net/minecraft/entity/vehicle/BoatEntity"))
    boolean boatsAndBeeps$dontPushAwayMultipartBoats(Object obj, Class<?> clazz) {
        return obj instanceof BoatEntity && !(obj instanceof UpgradedBoatEntity);
    }

    @Inject(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;", ordinal = 1))
    void changeVelocityDecay(CallbackInfo ci) {
        //noinspection ConstantConditions
        if ((Object) this instanceof UpgradedBoatEntity uBoat) {
            this.velocityDecay *= uBoat.getVelocityDecayModifier();
        }
    }
}
