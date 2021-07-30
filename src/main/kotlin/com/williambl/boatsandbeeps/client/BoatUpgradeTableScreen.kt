package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.table.BoatUpgradeTableGuiDescription
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class BoatUpgradeTableScreen(val boatTableDesc: BoatUpgradeTableGuiDescription, player: PlayerEntity?, title: Text?) :
    CottonInventoryScreen<BoatUpgradeTableGuiDescription>(boatTableDesc, player, title) {

    override fun init() {
        super.init()
        boatTableDesc.entityView.setBackgroundPainter(BackgroundPainter.createNinePatch(Identifier("boats-and-beeps:textures/gui/entity_view_background.png")))
    }
}