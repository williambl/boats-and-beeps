package com.williambl.boatsandbeeps.boat

import com.williambl.boatsandbeeps.mixin.BoatItemAccessor
import com.williambl.boatsandbeeps.readPartsAndUpgrades
import com.williambl.boatsandbeeps.setLore
import com.williambl.boatsandbeeps.upgrade.BoatUpgrade
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeSlot
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeType
import com.williambl.boatsandbeeps.upgradedBoatItems
import com.williambl.boatsandbeeps.writePartsAndUpgrades
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BannerPattern
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.*
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import java.util.*
import java.util.function.Predicate

class UpgradedBoatItem(val type: BoatEntity.Type, settings: Settings) : Item(settings) {
    val defaultStacks: List<ItemStack> by lazy { ALL_DEFAULT_STACKS.filter { it.item == this } }

    override fun use(world: World, user: PlayerEntity, hand: Hand?): TypedActionResult<ItemStack>? {
        val itemStack = user.getStackInHand(hand)
        val hitResult: HitResult = raycast(world, user, RaycastContext.FluidHandling.ANY)
        return if (hitResult.type == HitResult.Type.MISS) {
            TypedActionResult.pass(itemStack)
        } else {
            val looking = user.getRotationVec(1.0f)
            val colliding =
                world.getOtherEntities(user, user.boundingBox.stretch(looking.multiply(5.0)).expand(1.0), RIDERS)
            if (colliding.isNotEmpty()) {
                val userPos = user.eyePos
                for (entity in colliding) {
                    val entityBox = entity.boundingBox.expand(entity.targetingMargin.toDouble())
                    if (entityBox.contains(userPos)) {
                        return TypedActionResult.pass(itemStack)
                    }
                }
            }
            if (hitResult.type == HitResult.Type.BLOCK) {
                val partsAndUpgrades = readPartsAndUpgrades(itemStack.getOrCreateSubTag("BoatData"))
                val entity = UpgradedBoatEntity(world, hitResult.pos, partsAndUpgrades.first, partsAndUpgrades.second)
                entity.boatType = this.type
                entity.yaw = user.yaw
                if (!world.isSpaceEmpty(entity, entity.boundingBox.expand(-0.1))) {
                    TypedActionResult.fail(itemStack)
                } else {
                    if (!world.isClient) {
                        world.spawnEntity(entity)
                        world.emitGameEvent(user, GameEvent.ENTITY_PLACE, BlockPos(hitResult.pos))
                        if (!user.abilities.creativeMode) {
                            itemStack.decrement(1)
                        }
                    }
                    user.incrementStat(Stats.USED.getOrCreateStat(this))
                    TypedActionResult.success(itemStack, world.isClient())
                }
            } else {
                TypedActionResult.pass(itemStack)
            }
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        val (parts, upgrades) = readPartsAndUpgrades(stack.getOrCreateSubTag("BoatData"))
        tooltip.add(TranslatableText("tooltip.boats-and-beeps.parts", parts))
        tooltip.addAll(upgrades.flatMapIndexed { idx, it ->
            if (it.isEmpty()) listOf() else listOf(
                TranslatableText("tooltip.boats-and-beeps.upgrades_for_part", idx+1).formatted(Formatting.UNDERLINE),
                *(it.map { (slot, upgrade) ->
                    TranslatableText(
                        "tooltip.boats-and-beeps.upgrades_for_slot",
                        TranslatableText("slot.boats-and-beeps.${slot.name.lowercase(Locale.ROOT)}").formatted(Formatting.BOLD),
                        upgrade.type.getName().shallowCopy().formatted(Formatting.RED)
                    )
                }.toTypedArray())
            )
        })
    }

    override fun getDefaultStack(): ItemStack {
        return super.getDefaultStack().also {
            it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, List(1) { mapOf(
                BoatUpgradeSlot.FRONT to BoatUpgrade(
                    BoatUpgradeType.SEAT), BoatUpgradeSlot.BACK to BoatUpgrade(BoatUpgradeType.SEAT)
            ) }))
        }
    }

    override fun appendStacks(group: ItemGroup, stacks: DefaultedList<ItemStack>) {
        if (isIn(group)) {
            stacks.addAll(defaultStacks)
        }
    }

    companion object {
        val RIDERS: Predicate<Entity> = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::collides)

        val ALL_DEFAULT_STACKS: List<ItemStack> by lazy { listOf(
            upgradedBoatItems[BoatEntity.Type.OAK]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, List(1) { mapOf(
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(), BoatUpgradeSlot.BACK to BoatUpgradeType.CHEST.create()
                ) }))
                it.setCustomName(TranslatableText("item.boats-and-beeps.chest_boat"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.chest_boat.lore")))
            },
            upgradedBoatItems[BoatEntity.Type.SPRUCE]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(2, listOf(mapOf(
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.FURNACE.create(), BoatUpgradeSlot.BACK to BoatUpgradeType.FURNACE.create()
                ),
                    mapOf(
                        BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(), BoatUpgradeSlot.BACK to BoatUpgradeType.SEAT.create()
                    ))))
                it.setCustomName(TranslatableText("item.boats-and-beeps.furnace_boat"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.furnace_boat.lore")))
            },
            upgradedBoatItems[BoatEntity.Type.DARK_OAK]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, listOf(mapOf(
                    BoatUpgradeSlot.BOW to (BoatUpgradeType.SKULLS[Items.SKELETON_SKULL as WallStandingBlockItem] ?: throw IllegalStateException("Skulls don't exist???") ).create(),
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(),
                    BoatUpgradeSlot.BACK to (BoatUpgradeType.BANNERS[Blocks.BLACK_BANNER] ?: throw IllegalStateException("Banners don't exist???")).let { type ->
                        BoatUpgrade(type,
                            type.getDataFromItem(Items.BLACK_BANNER.defaultStack.apply {
                                getOrCreateSubTag("BlockEntityTag").put(
                                    "Patterns",
                                    BannerPattern.Patterns().add(BannerPattern.SKULL, DyeColor.WHITE).toNbt()
                                )
                            })
                        )
                    }
                ))))
                it.setCustomName(TranslatableText("item.boats-and-beeps.pirate_ship"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.pirate_ship.lore")))
            },
            upgradedBoatItems[BoatEntity.Type.BIRCH]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, List(1) { mapOf(
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(),
                    BoatUpgradeSlot.PORT to BoatUpgradeType.LIGHTNING_ROD.create(),
                    BoatUpgradeSlot.STARBOARD to BoatUpgradeType.LIGHTNING_ROD.create(),
                    BoatUpgradeSlot.BACK to (BoatUpgradeType.BANNERS[Blocks.RED_BANNER] ?: throw IllegalStateException("Banners don't exist???")).let { type ->
                        BoatUpgrade(type,
                            type.getDataFromItem(Items.RED_BANNER.defaultStack.apply {
                                getOrCreateSubTag("BlockEntityTag").put(
                                    "Patterns",
                                    BannerPattern.Patterns().add(BannerPattern.STRIPE_CENTER, DyeColor.WHITE).add(BannerPattern.TRIANGLE_BOTTOM, DyeColor.WHITE).toNbt()
                                )
                            })
                        )
                    },
                    BoatUpgradeSlot.AFT to BoatUpgradeType.SEA_LANTERN.create()
                ) }))
                it.setCustomName(TranslatableText("item.boats-and-beeps.speedy_boat"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.speedy_boat.lore")))
            },
            upgradedBoatItems[BoatEntity.Type.JUNGLE]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, List(1) { mapOf(
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(), BoatUpgradeSlot.BACK to (BoatUpgradeType.BANNERS[Blocks.RED_BANNER] ?: throw IllegalStateException("Banners don't exist???")).let { type ->
                        BoatUpgrade(type,
                            type.getDataFromItem(Items.RED_BANNER.defaultStack.apply {
                                getOrCreateSubTag("BlockEntityTag").put(
                                    "Patterns",
                                    BannerPattern.Patterns().add(BannerPattern.STRIPE_CENTER, DyeColor.WHITE).add(BannerPattern.TRIANGLE_BOTTOM, DyeColor.WHITE).toNbt()
                                )
                            })
                        )
                    },
                    BoatUpgradeSlot.AFT to BoatUpgradeType.BLUE_ICE.create()
                ) }))
                it.setCustomName(TranslatableText("item.boats-and-beeps.speedy_land_boat"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.speedy_land_boat.lore")))
            },
            upgradedBoatItems[BoatEntity.Type.ACACIA]?.defaultStack?.also {
                it.orCreateTag.put("BoatData", writePartsAndUpgrades(2, listOf(mapOf(
                    BoatUpgradeSlot.BOW to (BoatUpgradeType.SKULLS[Items.DRAGON_HEAD] ?: throw IllegalStateException("Heads don't exist???")).create(),
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(),
                    BoatUpgradeSlot.BACK to BoatUpgradeType.SEAT.create()
                ),
                mapOf(
                    BoatUpgradeSlot.FRONT to BoatUpgradeType.SEAT.create(),
                    BoatUpgradeSlot.PORT to BoatUpgradeType.END_ROD.create(),
                    BoatUpgradeSlot.STARBOARD to BoatUpgradeType.END_ROD.create(),
                    BoatUpgradeSlot.BACK to BoatUpgradeType.SEAT.create()
                ))))
                it.setCustomName(TranslatableText("item.boats-and-beeps.flying_boat"))
                it.setLore(listOf(TranslatableText("item.boats-and-beeps.flying_boat.lore")))
            }
            ).filterNotNull() }

        fun boatToUpgradedBoat(stack: ItemStack): ItemStack {
            if (stack.item is BoatItem) {
                val type = (stack.item as BoatItemAccessor).type
                val nbt = stack.tag
                return ItemStack(upgradedBoatItems[type], stack.count)
                    .also { it.tag = nbt }
                    .also { it.orCreateTag.put("BoatData", writePartsAndUpgrades(1, List(1) { mapOf(
                        BoatUpgradeSlot.FRONT to BoatUpgrade(
                            BoatUpgradeType.SEAT), BoatUpgradeSlot.BACK to BoatUpgrade(BoatUpgradeType.SEAT)) })) }
            }
            return ItemStack.EMPTY
        }
    }
}