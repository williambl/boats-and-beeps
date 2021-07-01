package com.williambl.boatsandbeeps.upgrade

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.SimpleRegistry

val UPGRADES_REGISTRY: SimpleRegistry<BoatUpgradeType> = FabricRegistryBuilder.createSimple(BoatUpgradeType::class.java, Identifier("boats-and-beeps:boat_upgrades")).buildAndRegister()

fun createUpgrade(stack: ItemStack): BoatUpgrade? {
    val type = BoatUpgradeType.ITEM_TO_UPGRADE_TYPE[stack.item] ?: return null
    val data = type.getExtraDataFromItem.invoke(stack)
    return BoatUpgrade(type, data)
}

data class BoatUpgrade(val type: BoatUpgradeType, val data: NbtCompound? = null) {
    fun getBlockstate(boat: UpgradedBoatEntity): BlockState = type.blockstate(boat, data)

    fun interact(boat: UpgradedBoatEntity, player: PlayerEntity, hand: Hand): ActionResult =
        type.interactMethod(boat, player, hand, data)

    fun interactSpecifically(boat: UpgradedBoatEntity, player: PlayerEntity, hand: Hand, part: Int, slot: BoatUpgradeSlot): ActionResult =
        type.interactSpecificallyMethod(boat, player, hand, part, slot, data)

    fun tick(boat: UpgradedBoatEntity, position: Vec3d) = type.tickMethod(boat, position, data)
}