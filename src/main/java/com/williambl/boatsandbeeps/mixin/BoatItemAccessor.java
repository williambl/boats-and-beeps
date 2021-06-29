package com.williambl.boatsandbeeps.mixin;

import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.BoatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BoatItem.class)
public interface BoatItemAccessor {
    @Accessor
    BoatEntity.Type getType();
}
