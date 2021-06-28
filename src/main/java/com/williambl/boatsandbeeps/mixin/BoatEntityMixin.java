package com.williambl.boatsandbeeps.mixin;

import com.williambl.boatsandbeeps.UpgradedBoatEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BoatEntity.class)
public class BoatEntityMixin {
    @Redirect(method = "pushAwayFrom", at=@At(value = "CONSTANT", args = "classValue=net/minecraft/entity/vehicle/BoatEntity"))
    boolean boatsAndBeeps$dontPushAwayMultipartBoats(Object obj, Class<?> clazz) {
        return obj instanceof BoatEntity && !(obj instanceof UpgradedBoatEntity);
    }
}
