package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.SimpleRegistry

val upgradesRegistry: SimpleRegistry<BoatUpgrade> = FabricRegistryBuilder.createSimple(BoatUpgrade::class.java, Identifier("boats-and-beeps:boat_upgrades")).buildAndRegister()

data class BoatUpgrade(
    val slot: List<BoatUpgradeSlot>,
    val name: String,
    val blockstate: BlockState = Blocks.AIR.defaultState,
    val interactMethod: (UpgradedBoatEntity, PlayerEntity, Hand) -> ActionResult = { _, _, _ -> ActionResult.PASS },
    val tickMethod: (UpgradedBoatEntity) -> Unit = {}
) {
    fun getId(): Identifier = upgradesRegistry.getId(this) ?: throw NullPointerException()

    companion object {
        val SEAT = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:seat"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "seat"))
        val CHEST = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:chest"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "chest", Blocks.CHEST.defaultState))
        val FURNACE = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:furnace"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "furnace", Blocks.FURNACE.defaultState, { boat, player, hand ->
            val fuelAmount = FuelRegistry.INSTANCE.get(player.getStackInHand(hand).item) ?: 0
            if (fuelAmount > 0) {
                if (!player.isCreative) {
                    player.getStackInHand(hand).decrement(1)
                }
                boat.fuel = fuelAmount
                return@BoatUpgrade ActionResult.SUCCESS
            }
            return@BoatUpgrade ActionResult.PASS
        }, { boat ->
            if (boat.fuel > 0) {
                boat.fuel--
                val newVel = boat.velocity.add(boat.rotationVector.multiply(1.0, 0.0, 1.0).normalize().multiply(0.01))
                if (newVel.lengthSquared() <= 64) {
                    boat.velocity = newVel
                }
            }
        }))
        val BANNER = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:banner"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "banner", Blocks.BLACK_BANNER.defaultState))
    }
}
