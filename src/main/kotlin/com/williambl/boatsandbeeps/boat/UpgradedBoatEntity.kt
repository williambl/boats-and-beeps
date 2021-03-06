package com.williambl.boatsandbeeps.boat

import com.williambl.boatsandbeeps.*
import com.williambl.boatsandbeeps.mixin.BoatEntityAccessor
import com.williambl.boatsandbeeps.upgrade.BoatUpgrade
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeSlot
import com.williambl.boatsandbeeps.upgrade.BoatUpgradeType
import com.williambl.multipartentities.MultipartEntity
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.crash.CrashException
import net.minecraft.util.crash.CrashReport
import net.minecraft.util.crash.CrashReportSection
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import kotlin.math.max
import kotlin.properties.Delegates

class UpgradedBoatEntity(world: World, position: Vec3d = Vec3d.ZERO, initialParts: Int = 1, var upgrades: List<Map<BoatUpgradeSlot, BoatUpgrade>> = listOf())
    : BoatEntity(upgradedBoatEntityType, world), MultipartEntity {

    init {
        ignoreCameraFrustum = true //todo fix the big BB
        setPosition(position)
        dataTracker.startTracking(isLitKey, false)
    }

    var parts: Int by Delegates.observable(initialParts) { _, _, new ->
        partEntities = Array(max(new, 1)) { BoatPartEntity(this, this.width, this.height) }
    }

    var partEntities = Array(max(parts, 1)) { BoatPartEntity(this, this.width, this.height) }

    var fuel: Int = 0

    var velocityDecayModifier: Float = 1.0f

    var isLit
        get() = dataTracker.get(isLitKey)
        set(value) = dataTracker.set(isLitKey, value)

    var inventories: List<Map<BoatUpgradeSlot, SimpleInventory?>> = upgrades.map { it.map { upgrade -> (upgrade.key) to (if (upgrade.value.type == BoatUpgradeType.CHEST) SimpleInventory(27) else null) }.toMap() }

    override fun canAddPassenger(passenger: Entity): Boolean {
        return passengerList.size < getSeats().size
    }

    override fun updatePassengerPosition(passenger: Entity) {
        if (hasPassenger(passenger)) {
            val i = passengerList.indexOf(passenger)
            val seat = getSeats().getOrNull(i)
            if (seat == null) {
                passenger.stopRiding()
                return
            }
            val offset = seat.add(0.0, (if (this.isRemoved) 0.01 else this.mountedHeightOffset) + passenger.heightOffset, 0.0).rotateY(-yaw * 0.0175f - 1.57f)
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

    private fun getSeats(): List<Vec3d> = upgrades.flatMapIndexed { index, map ->
        map.map { (slot, upgrade) ->
            if (upgrade.type == BoatUpgradeType.SEAT) {
                slot.position.add(-index * 2.0, 0.0, 0.0)
            } else {
                null
            }
        }.filterNotNull()
    }

    override fun tick() {
        velocityDecayModifier = 1.0f
        partEntities.forEachIndexed { i, part ->
            part.partTick(i)
        }
        upgrades.forEachIndexed { idx, p -> p.forEach {
            it.value.tick(this, pos.add(it.key.position.add(-2.0*idx, 0.0, 0.0).rotateY(-yaw * 0.0175f - 1.57f)))
        } }
        super.tick()
        val list = partEntities.flatMap { world.getOtherEntities(
            this,
            it.boundingBox.expand(0.5, -0.01, 0.5), // brrrr
            EntityPredicates.canBePushedBy(it)
        ) }
        if (list.isNotEmpty()) {
            val bl = !world.isClient && this.primaryPassenger !is PlayerEntity
            for (entity in list) {
                if (!entity.hasPassenger(this)) {
                    if (bl && canAddPassenger(entity) && !entity.hasVehicle() && entity.width < this.width && entity is LivingEntity && entity !is WaterCreatureEntity && entity !is PlayerEntity) {
                        entity.startRiding(this)
                    } else {
                        pushAwayFrom(entity)
                    }
                }
            }
        }
        if (!world.isClient()) {
            if (fuel > 0 && !isLit) {
                isLit = true
            } else if (fuel <= 0 && isLit) {
                isLit = false
            }
        }
    }

    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return this.type.dimensions.scaled(parts*2f, 1f)
    }

    override fun isCollidable(): Boolean {
        return false
    }

    override fun collides(): Boolean {
        return false
    }

    override fun collidesWith(other: Entity): Boolean {
        return false
    }

    override fun isPushable(): Boolean {
        return false
    }

    override fun collidesWithStateAtPos(pos: BlockPos, state: BlockState): Boolean {
        return VoxelShapes.matchesAnywhere(
            state.getCollisionShape(world, pos, ShapeContext.of(this)),
            partEntities.map { VoxelShapes.cuboid(it.boundingBox) }.reduce { acc, voxelShape -> VoxelShapes.combine(acc, voxelShape, BooleanBiFunction.AND) },
            BooleanBiFunction.AND
        )
    }

    override fun checkBlockCollision() {
        partEntities.map { it.boundingBox }.forEach { box ->
            val blockPos = BlockPos(box.minX + 0.001, box.minY + 0.001, box.minZ + 0.001)
            val blockPos2 = BlockPos(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001)
            if (world.isRegionLoaded(blockPos, blockPos2)) {
                val mutable = BlockPos.Mutable()
                for (i in blockPos.x..blockPos2.x) {
                    for (j in blockPos.y..blockPos2.y) {
                        for (k in blockPos.z..blockPos2.z) {
                            mutable[i, j] = k
                            val blockState = world.getBlockState(mutable)
                            try {
                                blockState.onEntityCollision(world, mutable, this)
                                onBlockCollision(blockState)
                            } catch (var12: Throwable) {
                                val crashReport = CrashReport.create(var12, "Colliding entity with block")
                                val crashReportSection = crashReport.addElement("Block being collided with")
                                CrashReportSection.addBlockInfo(crashReportSection, world, mutable, blockState)
                                throw CrashException(crashReport)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun pushAwayFrom(entity: Entity?) {}

    override fun getParts(): Array<BoatPartEntity> {
        return partEntities
    }

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS
        }
        for (upgrade in upgrades) {
            for (entry in upgrade) {
                val result = entry.value.interact(this, player, hand)
                if (result != ActionResult.PASS) {
                    return result
                }
            }
        }
        return super.interact(player, hand)
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        return if (isInvulnerableTo(source)) {
            false
        } else if (!world.isClient && !this.isRemoved) {
            this.damageWobbleSide = -this.damageWobbleSide
            this.damageWobbleTicks = 10
            this.damageWobbleStrength = this.damageWobbleStrength + amount * 10.0f
            scheduleVelocityUpdate()
            this.emitGameEvent(GameEvent.ENTITY_DAMAGED, source.attacker)
            val bl = source.attacker is PlayerEntity && (source.attacker as PlayerEntity?)!!.abilities.creativeMode
            if (bl || this.damageWobbleStrength > 40.0f) {
                if (!bl && world.gameRules.getBoolean(GameRules.DO_ENTITY_DROPS)) {
                    dropStack(getItemStack())
                }
                discard()
            }
            true
        } else {
            true
        }
    }

    private fun getItemStack(): ItemStack {
        return upgradedBoatItems[boatType]?.defaultStack?.also { stack ->
            stack.setSubNbt("BoatData", writePartsAndUpgrades(parts, upgrades))
        } ?: ItemStack.EMPTY
    }

    override fun getPickBlockStack(): ItemStack = getItemStack()

    override fun createSpawnPacket(): Packet<*> {
        return ServerPlayNetworking.createS2CPacket(
            Identifier("boats-and-beeps:spawn"),
            PacketByteBufs.create()
                .also {
                    EntitySpawnS2CPacket(this).write(it)
                }
                .writeNbt(this.getAsNbt())
        )
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.put("BoatData", getAsNbt())
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        val extraData = readPartsAndUpgrades(nbt.getCompound("BoatData"))
        parts = extraData.first
        upgrades = extraData.second
        inventories = upgrades.map { it.map { upgrade -> (upgrade.key) to (if (upgrade.value.type == BoatUpgradeType.CHEST) SimpleInventory(27) else null) }.toMap() }
    }

    fun interactAtPart(player: PlayerEntity, hand: Hand, hitPos: Vec3d, partNumber: Int): ActionResult {
        val slot = if (hitPos.rotateY(-yaw).z < 0) BoatUpgradeSlot.FRONT else BoatUpgradeSlot.BACK
        val result = upgrades[partNumber][slot]?.interactSpecifically(this, player, hand, partNumber, slot) ?: ActionResult.PASS
        if (result != ActionResult.PASS) {
            return result
        }
        return ActionResult.PASS
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    @JvmName("accessorHelper\$yawVelocity")
    fun getYawVelocity(): Float = (this as BoatEntityAccessor).yawVelocity


    @JvmName("accessorHelper\$getLocation")
    fun getLocation(): Location? {
        @Suppress("CAST_NEVER_SUCCEEDS")
        return ((this as BoatEntityAccessor).location)
    }

    fun getAsNbt(): NbtCompound {
        return writePartsAndUpgrades(parts, upgrades)
    }

    companion object {
        val isLitKey = DataTracker.registerData(UpgradedBoatEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
    }
}