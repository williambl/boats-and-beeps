package com.williambl.boatsandbeeps

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Insets
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier


class BoatUpgradeTableGuiDescription(syncId: Int, playerInventory: PlayerInventory, val context: ScreenHandlerContext) :
    SyncedGuiDescription(boatUpgradeTableScreenHandlerType, syncId, playerInventory, getBlockInventory(context, 8), getBlockPropertyDelegate(context)) {

    val root: WPlainPanel = WPlainPanel()
    val boatSlot: WItemSlot
    val upgradesPanel: WPlainPanel
    val upgradeSlots: Map<BoatUpgradeSlot, WItemSlot>
    val entityView: WEntityView
    val partsPanel: WPlainPanel

    init {
        setRootPanel(root)
        root.setSize(140, 200)
        root.insets = Insets.ROOT_PANEL
        boatSlot = WItemSlot.of(blockInventory, 0)
        root.add(boatSlot, 18, 18)

        upgradesPanel = WPlainPanel()
        upgradesPanel.setSize(80, 54)

        upgradeSlots = BoatUpgradeSlot.values().mapIndexed { idx, slot ->
            slot to WItemSlot.of(blockInventory, idx+1)
        }.toMap()

        upgradesPanel.add(WSprite(boatSprite), 0, 2, 80, 50)

        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.AFT], 0, 18)
        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.BACK], 18, 18)
        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.PORT], 29, 36)
        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.STARBOARD], 29, 0)
        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.FRONT], 41, 18)
        upgradesPanel.add(upgradeSlots[BoatUpgradeSlot.BOW], 59, 18)

        root.add(upgradesPanel, 77, 18)

        entityView = WEntityView(BoatEntity(EntityType.BOAT, world))
        root.add(entityView, 72, 72, 90, 36)

        partsPanel = WPlainPanel()
        partsPanel.setSize(72, 45)
        partsPanel.add(WLabel("Part: 1"), 0, 0)
        partsPanel.add(WButton(LiteralText("<")), 0, 9)
        partsPanel.add(WButton(LiteralText(">")), 18, 9)
        partsPanel.add(WLabel("Add Part:").setVerticalAlignment(VerticalAlignment.CENTER), 0, 30)
        partsPanel.add(WItemSlot.of(blockInventory, 7), 54, 30)

        root.add(partsPanel, 0, 54)

        root.add(this.createPlayerInventoryPanel(), 0, 108)

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