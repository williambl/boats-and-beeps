package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.SimpleRegistry

val upgradesRegistry: SimpleRegistry<BoatUpgrade> = FabricRegistryBuilder.createSimple(BoatUpgrade::class.java, Identifier("boats-and-beeps:boat_upgrades")).buildAndRegister()

data class BoatUpgrade(val slot: List<BoatUpgradeSlot>, val name: String, val blockstate: BlockState = Blocks.AIR.defaultState, val tickMethod: (UpgradedBoatEntity) -> Unit = {}) {
    companion object {
        val SEAT = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:seat"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "seat"))
        val CHEST = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:chest"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "chest", Blocks.CHEST.defaultState))
        val FURNACE = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:furnace"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "furnace", Blocks.FURNACE.defaultState) { boat ->
            if (boat.fuel > 0) {
                boat.velocity = boat.velocity.add(boat.rotationVector.multiply(1.0, 0.0, 1.0).normalize().multiply(0.02))
            }
        })
        val BANNER = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:banner"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "banner", Blocks.BLACK_BANNER.defaultState))
    }

    fun getId(): Identifier = upgradesRegistry.getId(this) ?: throw NullPointerException()
}
