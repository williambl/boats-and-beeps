package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.table.BoatUpgradeTableGuiDescription
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

class BoatUpgradeTableScreen(description: BoatUpgradeTableGuiDescription?, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<BoatUpgradeTableGuiDescription>(description, player, title) {
}