package com.williambl.boatsandbeeps.upgrade

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import com.williambl.boatsandbeeps.pineapple
import com.williambl.boatsandbeeps.tater
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.BannerBlock
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
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.intprovider.UniformIntProvider
import net.minecraft.util.registry.Registry
import net.minecraft.world.Heightmap

interface BoatUpgradeType {
    val slots: List<BoatUpgradeSlot>
    fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = Blocks.AIR.defaultState
    fun interact(boat: UpgradedBoatEntity, player: PlayerEntity, hand: Hand, data: NbtCompound?): ActionResult = ActionResult.PASS
    fun interactSpecifically(boat: UpgradedBoatEntity, player: PlayerEntity, hand: Hand, part: Int, slot: BoatUpgradeSlot, data: NbtCompound?): ActionResult = ActionResult.PASS
    fun tick(boat: UpgradedBoatEntity, upgradePos: Vec3d, data: NbtCompound?) {}
    fun getDataFromItem(stack: ItemStack): NbtCompound? = stack.tag

    fun getName(): Text = TranslatableText(Util.createTranslationKey("upgrade", getId()))
    fun getId(): Identifier = UPGRADES_REGISTRY.getId(this) ?: throw NullPointerException()
    fun create(): BoatUpgrade = BoatUpgrade(this)

    companion object {
        val SEAT = Registry.register(
            UPGRADES_REGISTRY, Identifier("boats-and-beeps:seat"), object : BoatUpgradeType {
                override val slots: List<BoatUpgradeSlot> = listOf(BoatUpgradeSlot.BACK, BoatUpgradeSlot.FRONT)
            }
        )

        val CHEST = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:chest"), object : BoatUpgradeType {
            override val slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK)
            override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = Blocks.CHEST.defaultState
            override fun interactSpecifically(
                boat: UpgradedBoatEntity,
                player: PlayerEntity,
                hand: Hand,
                part: Int,
                slot: BoatUpgradeSlot,
                data: NbtCompound?
            ): ActionResult {
                player.openHandledScreen(object : NamedScreenHandlerFactory {
                    override fun createMenu(
                        syncId: Int,
                        inv: PlayerInventory,
                        player: PlayerEntity
                    ): ScreenHandler =
                        GenericContainerScreenHandler.createGeneric9x3(syncId, inv, boat.inventories[part][slot])

                    override fun getDisplayName(): Text = TranslatableText("container.chest")
                })
                return ActionResult.SUCCESS
            }

            override fun getName(): Text = Blocks.CHEST.name
        })

        val FURNACE = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:furnace"), object : BoatUpgradeType {
            override val slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK)
            override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState =
                Blocks.FURNACE.defaultState.let {
                    if (boat.isLit) it.with(
                        Properties.LIT,
                        true
                    ) else it
                }

            override fun interactSpecifically(
                boat: UpgradedBoatEntity,
                player: PlayerEntity,
                hand: Hand,
                part: Int,
                slot: BoatUpgradeSlot,
                data: NbtCompound?
            ): ActionResult {
                val fuelAmount = FuelRegistry.INSTANCE.get(player.getStackInHand(hand).item) ?: 0
                if (fuelAmount > 0) {
                    if (!player.isCreative) {
                        player.getStackInHand(hand).decrement(1)
                    }
                    boat.fuel = fuelAmount
                    return ActionResult.SUCCESS
                }
                return ActionResult.PASS
            }

            override fun tick(boat: UpgradedBoatEntity, upgradePos: Vec3d, data: NbtCompound?) {
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

            override fun getName(): Text = Blocks.FURNACE.name
        })

        val BANNERS = Registry.BLOCK.asSequence()
            .filterIsInstance(BannerBlock::class.java)
            .map { it to Registry.BLOCK.getId(it) }
            .map { (banner, id) -> banner to Registry.register(UPGRADES_REGISTRY,
                Identifier("boats-and-beeps:${id.toString().replace(":", ".")}"),
                BannerUpgradeType(banner)
            ) }
            .toMap()

        val SKULLS = Registry.ITEM.asSequence()
            .filterIsInstance<WallStandingBlockItem>()
            .filter { it.block is SkullBlock }
            .map { it to Registry.ITEM.getId(it) }
            .map { (skull, id) -> skull to Registry.register(UPGRADES_REGISTRY,
                Identifier("boats-and-beeps:${id.toString().replace(":", ".")}"),
                SkullUpgradeType(skull)
            ) }
            .toMap()

        val TATER = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:tater"), object : BoatUpgradeType {
            override val slots = listOf(BoatUpgradeSlot.BOW)
            override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = tater.defaultState
            override fun getName(): Text = tater.name
        })

        val PINEAPPLE = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:pineapple"), object : BoatUpgradeType {
            override val slots = listOf(BoatUpgradeSlot.BOW)
            override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = pineapple.defaultState
            override fun getName(): Text = pineapple.name
        })

        val LIGHTNING_ROD = Registry.register(UPGRADES_REGISTRY, Identifier("boats-and-beeps:lightning_rod"), object : BoatUpgradeType {
            override val slots = listOf(BoatUpgradeSlot.AFT, BoatUpgradeSlot.PORT, BoatUpgradeSlot.STARBOARD)
            override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = Blocks.LIGHTNING_ROD.defaultState
            override fun tick(boat: UpgradedBoatEntity, upgradePos: Vec3d, data: NbtCompound?) {
                if (boat.world.isClient) {
                    if (boat.world.isThundering && boat.world.random.nextInt(200) <= boat.world.time % 200L && upgradePos.getY().toInt() == boat.world.getTopY(
                            Heightmap.Type.WORLD_SURFACE,
                            upgradePos.getX().toInt(),
                            upgradePos.getZ().toInt()
                        ) - 1
                    ) {
                        ParticleUtil.spawnParticle(
                            Direction.UP.axis,
                            boat.world,
                            BlockPos(upgradePos),
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

            override fun getName(): Text = Blocks.LIGHTNING_ROD.name
        })

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

class BannerUpgradeType(private val banner: BannerBlock) : BoatUpgradeType {
    override val slots = listOf(BoatUpgradeSlot.FRONT, BoatUpgradeSlot.BACK)
    override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = banner.defaultState
    override fun getName(): Text = banner.name
}

class SkullUpgradeType(private val skull: WallStandingBlockItem) : BoatUpgradeType {
    override val slots = listOf(BoatUpgradeSlot.BOW)
    override fun getBlockState(boat: UpgradedBoatEntity, data: NbtCompound?): BlockState = skull.block.defaultState
    override fun getDataFromItem(stack: ItemStack): NbtCompound? = stack.tag?.apply { put("BlockEntityTag", this.copy()) }
    override fun getName(): Text = skull.name
}