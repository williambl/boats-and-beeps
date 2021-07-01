package com.williambl.boatsandbeeps

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Insets
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier
import kotlin.math.max
import kotlin.properties.Delegates


class BoatUpgradeTableGuiDescription(syncId: Int, playerInventory: PlayerInventory, val context: ScreenHandlerContext) :
    SyncedGuiDescription(boatUpgradeTableScreenHandlerType, syncId, playerInventory, getBlockInventory(context, 8), null) {

    val root: WPlainPanel = WPlainPanel()
    val boatSlot: WItemSlot
    val upgradesPanel: WPlainPanel
    val upgradeSlots: BiMap<BoatUpgradeSlot, WItemSlot>
    val entityView: WEntityView
    val partsPanel: WPlainPanel
    val currentPartLabel: WLabel
    val prevPartButton: WButton
    val nextPartButton: WButton
    val addPartSlot: WItemSlot

    init {
        setRootPanel(root)
        root.setSize(140, 200)
        root.insets = Insets.ROOT_PANEL
        boatSlot = WItemSlot.of(blockInventory, 0).setFilter { it.item is BoatItem || it.item is UpgradedBoatItem }.also { it.addChangeListener(this::onBoatChanged) }
        root.add(boatSlot, 18, 18)

        upgradesPanel = WPlainPanel()
        upgradesPanel.setSize(80, 54)

        upgradeSlots = ImmutableBiMap.copyOf(BoatUpgradeSlot.values().mapIndexed { idx, slot ->
            slot to WItemSlot.of(blockInventory, idx+1).setFilter { stack -> canStackGoInUpgradeSlot(stack, slot) }.also { it.addChangeListener(this::onBoatUpgradeSlotChanged) }
        }.toMap())

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
        currentPartLabel = WLabel("Part: 1")
        partsPanel.add(currentPartLabel, 0, 0)
        prevPartButton = WButton(LiteralText("<")).also { it.onClick = Runnable { onPrevPartPressed() } }
        partsPanel.add(prevPartButton, 0, 9)
        nextPartButton = WButton(LiteralText(">")).also { it.onClick = Runnable { onNextPartPressed() } }
        partsPanel.add(nextPartButton, 18, 9)
        partsPanel.add(WLabel("Add Part:").setVerticalAlignment(VerticalAlignment.CENTER), 0, 30)
        addPartSlot = WItemSlot.of(blockInventory, 7).also { it.isModifiable = false }.also { it.addChangeListener(this::onPartAdded) }
        partsPanel.add(addPartSlot, 54, 30)

        root.add(partsPanel, 0, 54)

        root.add(this.createPlayerInventoryPanel(), 0, 108)

        root.validate(this)
    }

    /// STATE

    var hasBoat = false
    var currentPart: Int
        get() = currentPartProperty.get()
        set(value) = currentPartProperty.set(value)
    var partsAndUpgrades: Pair<Int, List<Map<BoatUpgradeSlot, BoatUpgrade>>>? by Delegates.observable(null) { prop, old, new -> onChangePart(currentPart) } // never set me except for when reading from the itemstack

    val currentPartProperty: Property

    init {
        setPropertyDelegate(object: PropertyDelegate {
            var currentPart: Int by Delegates.observable(0) { prop, old, new -> onChangePart(new) }

            override fun size(): Int = 1

            override fun get(index: Int): Int {
                if (index == 0) {
                    return currentPart;
                }
                // Unknown property IDs will fall back to -1
                return -1;
            }

            override fun set(index: Int, value: Int) {
                if (index == 0) {
                    currentPart = value
                    if (world.isClient) {
                        DistHelper.sendSyncPartClientToServer(syncId, currentPart)
                    }
                }
            }
        })

        currentPartProperty = Property.create(getPropertyDelegate(), 0)
        addProperty(currentPartProperty)
    }
    ///

    override fun close(playerEntity: PlayerEntity) {
        super.close(playerEntity)
        context.run { _, _ ->
            dropStack(playerEntity, blockInventory.getStack(0))
        }
    }

    fun dropStack(player: PlayerEntity, stack: ItemStack) {
        if (!player.isAlive || player is ServerPlayerEntity && player.isDisconnected) {
            player.dropItem(stack, false)
        } else {
            val playerInventory = player.inventory
            if (playerInventory.player is ServerPlayerEntity) {
                playerInventory.offerOrDrop(stack)
            }
        }
    }

    fun onBoatChanged(slot: WItemSlot, inventory: Inventory, index: Int, stack: ItemStack) {
        if (stack.isEmpty) {
            onBoatRemoved()
        } else {
            onBoatAdded(slot, inventory, index, stack)
        }
    }

    fun onBoatAdded(slot: WItemSlot, inventory: Inventory, index: Int, stack: ItemStack) {
        if (stack.item is BoatItem) {
            inventory.setStack(index, UpgradedBoatItem.boatToUpgradedBoat(stack))
        }
        hasBoat = true
        partsAndUpgrades = readPartsAndUpgrades(inventory.getStack(index).getOrCreateSubTag("BoatData"))
        currentPart = max(currentPart, 1)
        addPartSlot.isModifiable = true
    }

    fun onBoatRemoved() {
        hasBoat = false
        partsAndUpgrades = null
        currentPart = 0
        addPartSlot.isModifiable = false
    }

    fun onPrevPartPressed() {
        currentPart--
    }

    fun onNextPartPressed() {
        currentPart++
    }

    fun onChangePart(newPart: Int) {
        prevPartButton.isEnabled = newPart > 1
        nextPartButton.isEnabled = newPart < partsAndUpgrades?.first ?: 0
        currentPartLabel.text = LiteralText("Part: $newPart/${partsAndUpgrades?.first ?: 0}")
        val upgradeSlotValues = BoatUpgradeSlot.values()
        val upgrades = partsAndUpgrades?.second?.getOrNull(newPart-1)
        for (upgradeslot in upgradeSlots.keys) {
            blockInventory.setStack(
                upgradeSlotValues.indexOf(upgradeslot) + 1,
                upgrades?.let {
                    BoatUpgradeType.ITEM_TO_UPGRADE_TYPE.inverse()[it[upgradeslot]?.type]?.defaultStack?.also { s -> s.tag = it[upgradeslot]?.data }
                } ?: Items.AIR.defaultStack
            )
        }
        entityView.entity = createEntityForPart(newPart)
    }

    fun onPartAdded(slot: WItemSlot, inventory: Inventory, index: Int, stack: ItemStack) {
        inventory.setStack(index, ItemStack.EMPTY)
        when (stack.item) {
            is BoatItem -> {
                modifyBoatState(
                    parts = (partsAndUpgrades?.first ?: 0)+1,
                    upgrades = (partsAndUpgrades?.second?.toMutableList() ?: mutableListOf()).also { it.add(mapOf(BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(), BoatUpgradeSlot.BACK to BoatUpgradeType.SEAT.create())) }
                )
            }
            is UpgradedBoatItem -> {
                val otherState = readPartsAndUpgrades(stack.getOrCreateSubTag("BoatData"))
                modifyBoatState(
                    parts = (partsAndUpgrades?.first ?: 0) + otherState.first,
                    upgrades = (partsAndUpgrades?.second?.toMutableList() ?: mutableListOf()).also { it.addAll(otherState.second) }
                )
            }
        }
    }

    fun modifyBoatState(parts: Int = partsAndUpgrades?.first ?: 0, upgrades: List<Map<BoatUpgradeSlot, BoatUpgrade>> = partsAndUpgrades?.second ?: listOf()) {
        val tag = blockInventory.getStack(0).orCreateTag.copy()
        tag.remove("BoatData")
        tag.put("BoatData", writePartsAndUpgrades(parts, upgrades))
        blockInventory.getStack(0).tag = tag
        partsAndUpgrades = readPartsAndUpgrades(tag.getCompound("BoatData"))
    }

    fun canStackGoInUpgradeSlot(stack: ItemStack, slot: BoatUpgradeSlot): Boolean {
        return BoatUpgradeType.ITEM_TO_UPGRADE_TYPE[stack.item]?.slots?.contains(slot) ?: false
    }

    fun onBoatUpgradeSlotChanged(slot: WItemSlot, inventory: Inventory, index: Int, stack: ItemStack) {
        var changeToMake: Pair<BoatUpgradeSlot, BoatUpgrade>? = null
        when (val upgradeSlot = upgradeSlots.inverse()[slot]) {
            BoatUpgradeSlot.FRONT -> {
                if (stack.isEmpty) {
                    changeToMake = Pair(upgradeSlot, BoatUpgradeType.SEAT.create())
                } else {
                    createUpgrade(stack)?.let {
                        changeToMake = Pair(upgradeSlot, it)
                    }
                }
            }
            BoatUpgradeSlot.BACK -> {
                if (stack.isEmpty) {
                    changeToMake = Pair(upgradeSlot, BoatUpgradeType.SEAT.create())
                } else {
                    createUpgrade(stack)?.let {
                        changeToMake = Pair(upgradeSlot, it)
                    }
                }
            }
            else -> {
                if (upgradeSlot != null) {
                    createUpgrade(stack)?.let {
                        changeToMake = Pair(upgradeSlot, it)
                    }
                }
            }
        }

        if (changeToMake != null) {
            modifyBoatState(
                upgrades = (partsAndUpgrades?.second?.toMutableList() ?: mutableListOf()).also {
                    if (currentPart > 0) {
                        val map = it[currentPart - 1].toMutableMap()
                        map[changeToMake!!.first] = changeToMake!!.second
                        it[currentPart - 1] = map
                    }
                }
            )
        }
    }

    private fun createEntityForPart(newPart: Int): Entity? {
        val state = partsAndUpgrades
        return if (newPart == 0 || state == null || newPart > state.first){
            null
        } else {
            UpgradedBoatEntity(world, upgrades = listOf(state.second.getOrElse(newPart-1) { mapOf() })).also { it.boatType = (blockInventory.getStack(0).item as UpgradedBoatItem).type }
        }
    }

    companion object {
        val boatSprite = Identifier("boats-and-beeps:textures/gui/boat_background.png")
    }
}