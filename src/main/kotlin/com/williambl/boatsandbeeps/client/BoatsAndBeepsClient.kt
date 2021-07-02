package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.boatUpgradeTableScreenHandlerType
import com.williambl.boatsandbeeps.table.BoatUpgradeTableGuiDescription
import com.williambl.boatsandbeeps.upgradedBoatEntityType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.apache.http.impl.io.IdentityInputStream


fun init() {
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
}

