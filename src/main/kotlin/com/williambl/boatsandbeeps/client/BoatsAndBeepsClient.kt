package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.upgradedBoatEntityType
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.BoatEntityRenderer
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun init() {
    EntityRendererRegistry.INSTANCE.register(upgradedBoatEntityType) { context -> UpgradedBoatRenderer(context) }
}

