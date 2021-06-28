package com.williambl.boatsandbeeps

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.data.Insets
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.BiConsumer


class BoatUpgradeTableGuiDescription(syncId: Int, playerInventory: PlayerInventory, val context: ScreenHandlerContext) :
    SyncedGuiDescription(boatUpgradeTableScreenHandlerType, syncId, playerInventory, getBlockInventory(context, 2), getBlockPropertyDelegate(context)) {
        init {
            val root = WGridPanel()
            setRootPanel(root)
            root.setSize(140, 140)
            root.insets = Insets.ROOT_PANEL
            val itemSlot = WItemSlot.of(blockInventory, 0)
            root.add(itemSlot, 4, 1)
            root.add(this.createPlayerInventoryPanel(), 0, 3)
            root.validate(this)
        }

    override fun close(playerEntity: PlayerEntity?) {
        super.close(playerEntity)
        context.run { _, _ ->
            dropInventory(playerEntity, blockInventory)
        }
    }
}