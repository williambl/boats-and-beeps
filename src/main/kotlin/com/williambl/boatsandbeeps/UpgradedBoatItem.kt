package com.williambl.boatsandbeeps

import com.williambl.boatsandbeeps.mixin.BoatItemAccessor
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.BoatItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.stat.Stats
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
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
                val partsAndUpgrades = readUpgradesAndParts(itemStack.getOrCreateSubTag("BoatData"))
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

    override fun getDefaultStack(): ItemStack {
        return super.getDefaultStack().also {
            it.orCreateTag.put("BoatData", writeUpgradesAndParts(1, List(1) { mapOf(BoatUpgradeSlot.FRONT to BoatUpgrade.SEAT, BoatUpgradeSlot.BACK to BoatUpgrade.SEAT) }))
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
                    .also { it.orCreateTag.put("BoatData", writeUpgradesAndParts(1, List(1) { mapOf(BoatUpgradeSlot.FRONT to BoatUpgrade.SEAT, BoatUpgradeSlot.BACK to BoatUpgrade.SEAT) })) }
            }
            return ItemStack.EMPTY
        }
    }
}