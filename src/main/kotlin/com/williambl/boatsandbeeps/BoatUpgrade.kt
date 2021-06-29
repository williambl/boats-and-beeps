package com.williambl.boatsandbeeps

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.SimpleRegistry

val upgradesRegistry: SimpleRegistry<BoatUpgrade> = FabricRegistryBuilder.createSimple(BoatUpgrade::class.java, Identifier("boats-and-beeps:boat_upgrades")).buildAndRegister()

data class BoatUpgrade(
    val slot: List<BoatUpgradeSlot>,
    val name: String,
    val blockstate: (UpgradedBoatEntity) -> BlockState = { Blocks.AIR.defaultState },
    val interactMethod: (UpgradedBoatEntity, PlayerEntity, Hand) -> ActionResult = { _, _, _ -> ActionResult.PASS },
    val tickMethod: (UpgradedBoatEntity, Vec3d) -> Unit = { _, _ -> }
) {
    fun getId(): Identifier = upgradesRegistry.getId(this) ?: throw NullPointerException()

    companion object {
        val SEAT = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:seat"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "seat"))
        val CHEST = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:chest"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "chest", { Blocks.CHEST.defaultState }))
        val FURNACE = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:furnace"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "furnace", { boat -> Blocks.FURNACE.defaultState.let { if (boat.isLit) it.with(Properties.LIT, true) else it } }, { boat, player, hand ->
            val fuelAmount = FuelRegistry.INSTANCE.get(player.getStackInHand(hand).item) ?: 0
            if (fuelAmount > 0) {
                if (!player.isCreative) {
                    player.getStackInHand(hand).decrement(1)
                }
                boat.fuel = fuelAmount
                return@BoatUpgrade ActionResult.SUCCESS
            }
            return@BoatUpgrade ActionResult.PASS
        }, { boat, upgradePos ->
            if (boat.fuel > 0) {
                boat.fuel--
                val newVel = boat.velocity.add(boat.rotationVector.multiply(1.0, 0.0, 1.0).normalize().multiply(0.01))
                if (newVel.lengthSquared() <= 64) {
                    boat.velocity = newVel
                }
                if (boat.world.random.nextInt(4) == 0) {
                    boat.world.addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        upgradePos.x,
                        upgradePos.y + 0.8,
                        upgradePos.z,
                        0.0,
                        0.0,
                        0.0
                    )
                }
            }
        }))
        val BANNER = Registry.register(upgradesRegistry, Identifier("boats-and-beeps:banner"), BoatUpgrade(listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK), "banner", { Blocks.BLACK_BANNER.defaultState }))

        val ITEM_TO_UPGRADE: BiMap<Item, BoatUpgrade> = HashBiMap.create(mutableMapOf(
            Items.CHEST to CHEST,
            Items.FURNACE to FURNACE,
            Items.WHITE_BANNER to BANNER
        ))
    }
}
