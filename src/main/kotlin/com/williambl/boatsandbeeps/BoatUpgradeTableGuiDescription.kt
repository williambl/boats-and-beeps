package com.williambl.boatsandbeeps

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.WSprite
import io.github.cottonmc.cotton.gui.widget.data.Insets
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.Identifier


class BoatUpgradeTableGuiDescription(syncId: Int, playerInventory: PlayerInventory, val context: ScreenHandlerContext) :
    SyncedGuiDescription(boatUpgradeTableScreenHandlerType, syncId, playerInventory, getBlockInventory(context, 7), getBlockPropertyDelegate(context)) {
        init {
            val root = WGridPanel()
            setRootPanel(root)
            root.setSize(140, 200)
            root.insets = Insets.ROOT_PANEL
            val boatSlot = WItemSlot.of(blockInventory, 0)
            root.add(boatSlot, 1, 2)

            val upgradesPanel = WPlainPanel()
            upgradesPanel.setSize(80, 54)

            val upgradeSlots = BoatUpgradeSlot.values().mapIndexed { idx, slot ->
                slot to WItemSlot.of(blockInventory, idx+1)
            }.toMap()

            upgradesPanel.add(WSprite(boatSprite), 0, 2, 80, 50)

            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.AFT], 0, 18)
            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.BACK], 18, 18)
            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.PORT], 29, 36)
            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.STARBOARD], 29, 0)
            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.FRONT], 41, 18)
            upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.BOW], 59, 18)

            root.add(upgradesPanel, 4, 1)

            root.add(this.createPlayerInventoryPanel(), 0, 6)

            root.validate(this)
        }

    override fun close(playerEntity: PlayerEntity?) {
        super.close(playerEntity)
        context.run { _, _ ->
            dropInventory(playerEntity, blockInventory)
        }
    }

    companion object {
        val boatSprite = Identifier("boats-and-beeps:textures/gui/boat_background.png")
    }
}