package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.boatUpgradeTableScreenHandlerType
import com.williambl.boatsandbeeps.table.BoatUpgradeTableGuiDescription
import com.williambl.boatsandbeeps.upgradedBoatEntityType
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text


fun init() {
    EntityRendererRegistry.INSTANCE.register(upgradedBoatEntityType) { context -> UpgradedBoatRenderer(context) }
    ScreenRegistry.register(boatUpgradeTableScreenHandlerType) { gui: BoatUpgradeTableGuiDescription, inventory: PlayerInventory, title: Text ->
        BoatUpgradeTableScreen(
            gui,
            inventory.player,
            title
        )
    }
}

