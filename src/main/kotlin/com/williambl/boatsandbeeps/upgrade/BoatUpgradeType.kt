package com.williambl.boatsandbeeps.upgrade

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import com.williambl.boatsandbeeps.tater
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.AbstractBannerBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.SkullBlock
import net.minecraft.client.util.ParticleUtil
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.WallStandingBlockItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.intprovider.UniformIntProvider
import net.minecraft.util.registry.Registry
import net.minecraft.world.Heightmap

data class BoatUpgradeType(
    val slots: List<BoatUpgradeSlot>,
    val blockstate: (UpgradedBoatEntity, NbtCompound?) -> BlockState = { _, _ -> Blocks.AIR.defaultState },
    val interactMethod: (UpgradedBoatEntity, PlayerEntity, Hand, NbtCompound?) -> ActionResult = { _, _, _, _ -> ActionResult.PASS },
    val interactSpecificallyMethod: (UpgradedBoatEntity, PlayerEntity, Hand, Int, BoatUpgradeSlot, NbtCompound?) -> ActionResult = { _, _, _, _, _, _ -> ActionResult.PASS },
    val tickMethod: (UpgradedBoatEntity, Vec3d, NbtCompound?) -> Unit = { _, _, _ -> },
    val getExtraDataFromItem: (ItemStack) -> NbtCompound? = ItemStack::getTag
) {
    fun getId(): Identifier = UPGRADES_REGISTRY.getId(this) ?: throw NullPointerException()
    fun create(): BoatUpgrade = BoatUpgrade(this)

    companion object {
        val SEAT = Registry.register(
            UPGRADES_REGISTRY, Identifier("boats-and-beeps:seat"), BoatUpgradeType(
                slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK)
            )
        )

        val CHEST = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:chest"), BoatUpgradeType(
            slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK),
            blockstate = { _, _ -> Blocks.CHEST.defaultState },
            interactSpecificallyMethod = { boat, player, hand, part, slot, data ->
                player.openHandledScreen(object : NamedScreenHandlerFactory {
                    override fun createMenu(
                        syncId: Int,
                        inv: PlayerInventory,
                        player: PlayerEntity
                    ): ScreenHandler =
                        GenericContainerScreenHandler.createGeneric9x3(syncId, inv, boat.inventories[part][slot])

                    override fun getDisplayName(): Text = TranslatableText("container.chest")
                })
                ActionResult.SUCCESS
            }
        ))

        val FURNACE = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:furnace"), BoatUpgradeType(
            slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK),
            blockstate = { boat, data ->
                Blocks.FURNACE.defaultState.let {
                    if (boat.isLit) it.with(
                        Properties.LIT,
                        true
                    ) else it
                }
            },
            interactMethod = { boat, player, hand, data ->
                val fuelAmount = FuelRegistry.INSTANCE.get(player.getStackInHand(hand).item) ?: 0
                if (fuelAmount > 0) {
                    if (!player.isCreative) {
                        player.getStackInHand(hand).decrement(1)
                    }
                    boat.fuel = fuelAmount
                    return@BoatUpgradeType ActionResult.SUCCESS
                }
                return@BoatUpgradeType ActionResult.PASS
            }, tickMethod = { boat, upgradePos, data ->
                if (boat.fuel > 0) {
                    boat.fuel--
                    val newVel =
                        boat.velocity.add(boat.rotationVector.multiply(1.0, 0.0, 1.0).normalize().multiply(0.01))
                    if (newVel.lengthSquared() <= 64) {
                        boat.velocity = newVel
                    }
                    if (boat.world.random.nextInt(4) == 0) {
                        boat.world.addParticle(
                            ParticleTypes.LARGE_SMOKE,
                            upgradePos.x,
                            upgradePos.y + 0.8,
                            upgradePos.z,
                            0.0,
                            0.0,
                            0.0
                        )
                    }
                }
            }
        ))
        val BANNERS = Registry.BLOCK.asSequence()
            .filterIsInstance(AbstractBannerBlock::class.java)
            .map { it to Registry.BLOCK.getId(it) }
            .map { (banner, id) -> banner to Registry.register(
                UPGRADES_REGISTRY, Identifier("boats-and-beeps:${id.toString().replace(":", ".")}"), BoatUpgradeType(
                    slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK),
                    blockstate = { _, _ -> banner.defaultState })
            )
            }
            .toMap()

        val SKULLS = Registry.ITEM.asSequence()
            .filterIsInstance<WallStandingBlockItem>()
            .filter { it.block is SkullBlock }
            .map { it to Registry.ITEM.getId(it) }
            .map { (skull, id) -> skull to Registry.register(UPGRADES_REGISTRY,
                Identifier("boats-and-beeps:${id.toString().replace(":", ".")}"),
                BoatUpgradeType(
                    slots = listOf(BoatUpgradeSlot.BOW),
                    blockstate = { _, _ -> skull.block.defaultState },
                    getExtraDataFromItem = { stack -> stack.tag?.apply { put("BlockEntityTag", this.copy()) } }))
            }
            .toMap()

        val TATER = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:tater"), BoatUpgradeType(
            slots = listOf(BoatUpgradeSlot.BOW),
            blockstate = { _, _ -> tater.defaultState }
        ))

        val LIGHTNING_ROD = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:lightning_rod"), BoatUpgradeType(
            slots = listOf(BoatUpgradeSlot.AFT, BoatUpgradeSlot.PORT, BoatUpgradeSlot.STARBOARD),
            blockstate = { _, _ -> Blocks.LIGHTNING_ROD.defaultState },
            tickMethod = { boat, pos, data ->
                if (boat.world.isClient) {
                    if (boat.world.isThundering && boat.world.random.nextInt(200) <= boat.world.time % 200L && pos.getY().toInt() == boat.world.getTopY(
                            Heightmap.Type.WORLD_SURFACE,
                            pos.getX().toInt(),
                            pos.getZ().toInt()
                        ) - 1
                    ) {
                        ParticleUtil.spawnParticle(
                            Direction.UP.axis,
                            boat.world,
                            BlockPos(pos),
                            0.125,
                            ParticleTypes.ELECTRIC_SPARK,
                            UniformIntProvider.create(1, 2)
                        )
                    }

                } else {
                    if (boat.world.isThundering) {
                        if (boat.world.random.nextDouble() < 0.001) {
                            boat.world.spawnEntity(EntityType.LIGHTNING_BOLT.create(boat.world)?.apply {
                                setPosition(pos)
                                setCosmetic(true)
                            })
                            if (boat.isLogicalSideForUpdatingMovement) {
                                boat.velocity = boat.velocity.multiply(10.0)
                            } else {
                                val player = boat.primaryPassenger
                                if (player is ServerPlayerEntity) {
                                    ServerPlayNetworking.send(player, Identifier("boats-and-beeps:multiply_velocity"),
                                        PacketByteBufs.create().also {
                                            it.writeDouble(2.0).writeDouble(2.0).writeDouble(2.0)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ))

        val ITEM_TO_UPGRADE_TYPE: BiMap<Item, BoatUpgradeType> = HashBiMap.create(mutableMapOf(
            Items.CHEST to CHEST,
            Items.FURNACE to FURNACE,
            Items.POTATO to TATER,
            Items.LIGHTNING_ROD to LIGHTNING_ROD
        ).apply {
            putAll(BANNERS.mapKeys { it.key.asItem() })
            putAll(SKULLS)
        })

    }
}