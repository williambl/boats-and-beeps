package com.williambl.boatsandbeeps.boat

import com.williambl.boatsandbeeps.DistHelper
import com.williambl.multipartentities.CollidingPartEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.ItemStack
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

class BoatPartEntity(parent: UpgradedBoatEntity, width: Float, height: Float) :
    CollidingPartEntity<UpgradedBoatEntity>(parent, width, height) {

    fun partTick(i: Int) {
        this.lastRenderX = this.x
        this.lastRenderY = this.y
        this.lastRenderZ = this.z
        this.prevX = this.x
        this.prevY = this.y
        this.prevZ = this.z
        this.setPosition(parent.pos.subtract(parent.rotationVector.normalize().multiply(i*2.0, 0.0, i*2.0)))

        val list = world.getOtherEntities(
            this,
            this.boundingBox.expand(0.20000000298023224, -0.009999999776482582, 0.20000000298023224), // mmmm decompiled floats
            EntityPredicates.canBePushedBy(this)
        )
        if (list.isNotEmpty()) {
            val bl = !world.isClient && this.primaryPassenger !is PlayerEntity
            for (j in list.indices) {
                val entity = list[j]
                if (!entity.hasPassenger(this)) {
                    if (bl && passengerList.size < 2 && !entity.hasVehicle() && entity.width < this.width && entity is LivingEntity && entity !is WaterCreatureEntity && entity !is PlayerEntity) {
                        entity.startRiding(this)
                    } else {
                        pushAwayFrom(entity)
                    }
                }
            }
        }

    }

    override fun move(movementType: MovementType?, movement: Vec3d?) {
        parent.move(movementType, movement)
    }

    override fun isCollidable(): Boolean {
        return true
    }

    override fun collidesWith(other: Entity): Boolean {
        return (other.isCollidable || other.isPushable) && !parent.isConnectedThroughVehicle(other)
    }

    override fun isPushable(): Boolean {
        return true
    }

    override fun pushAwayFrom(entity: Entity) {
        if ((entity is BoatEntity && entity !is UpgradedBoatEntity) || entity is BoatPartEntity) {
            if (entity.boundingBox.minY < this.boundingBox.maxY) {
                super.pushAwayFrom(entity);
            }
        } else if (entity.boundingBox.minY <= this.boundingBox.minY) {
            super.pushAwayFrom(entity);
        }
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        return parent.damage(source, amount)
    }

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        return parent.interact(player, hand)
    }

    override fun interactAt(player: PlayerEntity, hitPos: Vec3d, hand: Hand): ActionResult {
        val partNumber = parent.partEntities.indexOf(this)
        if (player.world.isClient) {
            DistHelper.sendInteractAtPArtClientToServer(parent, hand, hitPos, partNumber)
        }
        return parent.interactAtPart(player, hand, hitPos, partNumber)
    }

    override fun getPickBlockStack(): ItemStack? {
        return parent.pickBlockStack
    }
}