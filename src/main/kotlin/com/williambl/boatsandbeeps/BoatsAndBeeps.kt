package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Material
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry

val upgradedBoatEntityType = Registry.register(Registry.ENTITY_TYPE, Identifier("boats-and-beeps:upgraded_boat"),
    FabricEntityTypeBuilder.create<UpgradedBoatEntity>(SpawnGroup.MISC) { type, world -> UpgradedBoatEntity(world) }
        .dimensions(EntityDimensions.changing(1.375f, 0.5625f))
        .trackedUpdateRate(10)
        .build()
)

val upgradedBoatItems = BoatEntity.Type.values().map { type ->
    type to Registry.register(
        Registry.ITEM,
        Identifier("boats-and-beeps:${type.getName()}_boat"),
        UpgradedBoatItem(type, Item.Settings().maxCount(1).group(ItemGroup.TRANSPORTATION))
    )
}.toMap()

val boatUpgradeTableScreenHandlerType = ScreenHandlerRegistry.registerSimple(Identifier("boats-and-beeps:boat_upgrade_table")) { syncId, inventory ->
    BoatUpgradeTableGuiDescription(
        syncId,
        inventory,
        ScreenHandlerContext.EMPTY
    )
}

val boatUpgradeTable = Registry.register(Registry.BLOCK, Identifier("boats-and-beeps:boat_upgrade_table"), BoatUpgradeTableBlock(AbstractBlock.Settings.of(Material.WOOD)))
val boatUpgradeTableItem = Registry.register(Registry.ITEM, Identifier("boats-and-beeps:boat_upgrade_table"), BlockItem(boatUpgradeTable, Item.Settings().group(ItemGroup.DECORATIONS)))

fun init() {
    ServerPlayNetworking.registerGlobalReceiver(Identifier("boats-and-beeps:sync_part")) { server, player, handler, buf, sender ->
        val syncId = buf.readVarInt()
        val value = buf.readVarInt()
        server.execute {
            val screenHandler = player.currentScreenHandler
            if (screenHandler.syncId == syncId) {
                screenHandler.setProperty(0, value)
            }
        }
    }
    ServerPlayNetworking.registerGlobalReceiver(Identifier("boats-and-beeps:interact")) { server, player, handler, buf, sender ->
        val boatId = buf.readVarInt()
        val hand = buf.readEnumConstant(Hand::class.java)
        val hitPos = Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
        val partNumber = buf.readVarInt()
        server.execute {
            (player.world.getEntityById(boatId) as UpgradedBoatEntity).interactAtPart(player, hand, hitPos, partNumber)
        }
    }

}

fun UpgradedBoatEntity.getAsNbt(): NbtCompound {
    return writePartsAndUpgrades(parts, upgrades)
}

fun writePartsAndUpgrades(parts: Int, upgrades: List<Map<BoatUpgradeSlot, BoatUpgradeInstance>>): NbtCompound =
    NbtCompound().apply {
        putInt("Parts", parts)
        put("Upgrades", NbtList().also { list ->
            upgrades.forEach { u ->
                list.add(NbtCompound().apply {
                    u.forEach { (key, value) ->
                        put(
                            key.name,
                            NbtCompound().apply {
                                putString("Type", value.type.getId().toString())
                                value.data?.let { put("Data", it) }
                            }
                        )
                    }
                })
            }
        })
    }

fun readPartsAndUpgrades(nbt: NbtCompound): Pair<Int, List<Map<BoatUpgradeSlot, BoatUpgradeInstance>>> = Pair(
    nbt.getInt("Parts").coerceAtLeast(1),
    nbt.getList("Upgrades", 10).map {
        mutableMapOf<BoatUpgradeSlot, BoatUpgradeInstance>().also { upgradesMap ->
            if (it is NbtCompound) {
                for (slot in BoatUpgradeSlot.values()) {
                    if (it.contains(slot.name)) {
                        val upgrade = it.getCompound(slot.name)
                        upgradesMap[slot] = BoatUpgradeInstance(UPGRADES_REGISTRY.get(Identifier(upgrade.getString("Type"))) ?: continue, if (upgrade.contains("Data")) upgrade.getCompound("Data") else null)
                    }
                }
            }
        }
    }
)

