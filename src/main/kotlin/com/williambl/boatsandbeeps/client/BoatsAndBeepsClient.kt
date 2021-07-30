package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.*
import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import com.williambl.boatsandbeeps.table.BoatUpgradeTableGuiDescription
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier


fun init() {
    BlockRenderLayerMap.INSTANCE.putBlock(boatUpgradeTable, RenderLayer.getCutoutMipped())
    EntityRendererRegistry.INSTANCE.register(upgradedBoatEntityType) { context -> UpgradedBoatRenderer(context) }
    ScreenRegistry.register(boatUpgradeTableScreenHandlerType) { gui: BoatUpgradeTableGuiDescription, inventory: PlayerInventory, title: Text ->
        BoatUpgradeTableScreen(
            gui,
            inventory.player,
            title
        )
    }

    ClientPlayNetworking.registerGlobalReceiver(Identifier("boats-and-beeps:multiply_velocity")) { client, handler, buf, sender ->
        val (x, y, z) = Triple(buf.readDouble(), buf.readDouble(), buf.readDouble())
        client.execute {
            client.player?.vehicle?.let { vehicle ->
                if (vehicle.isLogicalSideForUpdatingMovement) {
                    vehicle.velocity = vehicle.velocity.multiply(x, y, z)
                }
            }
        }
    }

    ClientPlayNetworking.registerGlobalReceiver(Identifier("boats-and-beeps:spawn")) { client, handler, buf, sender ->
        val spawnPacket = EntitySpawnS2CPacket(buf)
        val id = spawnPacket.id
        val nbt = buf.readNbt()

        client.execute {
            handler.onEntitySpawn(spawnPacket)
            val boat = handler.world.getEntityById(id)
            if (boat is UpgradedBoatEntity && nbt != null) {
                val extraData = readPartsAndUpgrades(nbt)
                boat.parts = extraData.first
                val parts = boat.getParts()
                for (i in parts.indices) {
                    parts[i].id = i + id
                }
                boat.upgrades = extraData.second
            }
        }
    }

    upgradedBoatItems.forEach {
        BuiltinItemRendererRegistry.INSTANCE.register(it.value, UpgradedBoatItemRenderer(it.value.type))
    }
}

