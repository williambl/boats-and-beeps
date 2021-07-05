package com.williambl.boatsandbeeps.boat

import com.williambl.boatsandbeeps.mixin.BoatItemAccessor
import com.williambl.boatsandbeeps.readPartsAndUpgrades
import com.williambl.boatsandbeeps.upgrade.BoatUpgrade
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeSlot
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeType
import com.williambl.boatsandbeeps.upgradedBoatItems
import com.williambl.boatsandbeeps.writePartsAndUpgrades
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.BoatItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
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
            stacks.add(defaultStack)
        }
    }

    companion object {
        val RIDERS: Predicate<Entity> = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::collides)

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