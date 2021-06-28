package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.SimpleRegistry

val upgradesRegistry: SimpleRegistry<BoatUpgrade> = FabricRegistryBuilder.createSimple(BoatUpgrade::class.java, Identifier("boats-and-beeps:boat_upgrades")).buildAndRegister()

data class BoatUpgrade(val slot: List<BoatUpgradeSlot>) {
    companion object {
        val SEAT = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:seat"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK)))
    }

    fun getId(): Identifier = upgradesRegistry.getId(this) ?: throw NullPointerException()
}
