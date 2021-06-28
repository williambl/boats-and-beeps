package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.BoatItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

val upgradedBoatEntityType = Registry.register(Registry.ENTITY_TYPE, Identifier("boats-and-beeps:upgraded_boat"),
    FabricEntityTypeBuilder.create<UpgradedBoatEntity>(SpawnGroup.MISC) { type, world -> UpgradedBoatEntity(world) }
        .dimensions(EntityDimensions.changing(1.375f, 0.5625f))
        .trackedUpdateRate(10)
        .build()
)

val upgradedBoatItems = BoatEntity.Type.values().map { type ->
    Registry.register(
        Registry.ITEM,
        Identifier("boats-and-beeps:${type.getName()}_boat"),
        UpgradedBoatItem(type, Item.Settings().maxCount(1).group(ItemGroup.TRANSPORTATION))
    )
}

fun init() {
}

fun UpgradedBoatEntity.getAsNbt(): NbtCompound {
    return writeUpgradesAndParts(parts, upgrades)
}

fun writeUpgradesAndParts(parts: Int, upgrades: List<Map<BoatUpgradeSlot, BoatUpgrade>>): NbtCompound =
    NbtCompound().apply {
        putInt("Parts", parts)
        put("Upgrades", NbtList().also { list ->
            upgrades.forEach { u ->
                list.add(NbtCompound().apply {
                    u.forEach { (key, value) ->
                        putString(
                            key.name,
                            value.getId().toString()
                        )
                    }
                })
            }
        })
    }

fun readUpgradesAndParts(nbt: NbtCompound): Pair<Int, List<Map<BoatUpgradeSlot, BoatUpgrade>>> = Pair(
    nbt.getInt("Parts").coerceAtLeast(1),
    nbt.getList("Upgrades", 10).map {
        mutableMapOf<BoatUpgradeSlot, BoatUpgrade>().also { upgradesMap ->
            if (it is NbtCompound) {
                for (slot in BoatUpgradeSlot.values()) {
                    if (it.contains(slot.name)) {
                        upgradesMap[slot] = upgradesRegistry.get(Identifier(it.getString(slot.name))) ?: continue
                    }
                }
            }
        }
    }
)

