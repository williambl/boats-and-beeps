package com.williambl.boatsandbeeps

import com.williambl.boatsandbeeps.mixin.BoatEntityAccessor
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class UpgradedBoatEntity(entityType: EntityType<UpgradedBoatEntity>, world: World, val upgrades: Map<Int, Set<BoatUpgrade>> = mapOf(0 to setOf(
    BoatUpgrade(listOf(Vec3d(0.2, 0.0, 0.0), Vec3d(-0.6, 0.0, 0.0)))), 1 to setOf(BoatUpgrade(listOf(Vec3d(0.2, 0.0, 0.0), Vec3d(-0.6, 0.0, 0.0))))
)) : BoatEntity(entityType, world) {

    var parts: Int
        get() = dataTracker.get(partsData)
        set(value) = dataTracker.set(partsData, value)

    init {
        dataTracker.startTracking(partsData, 1)
    }

    override fun canAddPassenger(passenger: Entity): Boolean {
        return passengerList.size < getSeats().size
    }

    override fun updatePassengerPosition(passenger: Entity) {
        if (hasPassenger(passenger)) {
            val i = passengerList.indexOf(passenger)
            val offset = getSeats()[i].add(0.0, (if (this.isRemoved) 0.01 else this.mountedHeightOffset) + passenger.heightOffset, 0.0).rotateY(-yaw * 0.0175f - 1.57f)
            passenger.setPosition(pos.add(offset))
            passenger.yaw = passenger.yaw + getYawVelocity()
            passenger.headYaw = passenger.headYaw + getYawVelocity()
            copyEntityData(passenger)
            if (passenger is AnimalEntity) {
                val rotation = if (passenger.getId() % 2 == 0) 90 else 270
                passenger.setBodyYaw(passenger.bodyYaw + rotation.toFloat())
                passenger.setHeadYaw(passenger.getHeadYaw() + rotation.toFloat())
            }
        }
    }

    private fun getSeats(): List<Vec3d> = upgrades.flatMap { it.value.flatMap { u -> u.seats.map { s -> s.add(-it.key*2.0, 0.0, 0.0) } } }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @JvmName("accessorHelper\$yawVelocity")
    fun getYawVelocity(): Float = (this as BoatEntityAccessor).yawVelocity

    companion object {
        val partsData: TrackedData<Int> = DataTracker.registerData(UpgradedBoatEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
    }
}