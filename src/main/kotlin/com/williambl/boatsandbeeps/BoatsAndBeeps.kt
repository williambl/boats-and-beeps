package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

val upgradedBoatEntityType = Registry.register(Registry.ENTITY_TYPE, Identifier("boats-and-beeps:upgraded_boat"),
    FabricEntityTypeBuilder.create<UpgradedBoatEntity>(SpawnGroup.MISC) { type, world -> UpgradedBoatEntity(type, world) }
        .dimensions(EntityDimensions.changing(1.375f, 0.5625f))
        .trackedUpdateRate(10)
        .build()
)

fun init() {
}

