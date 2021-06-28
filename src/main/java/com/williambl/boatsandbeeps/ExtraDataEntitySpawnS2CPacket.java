package com.williambl.boatsandbeeps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class ExtraDataEntitySpawnS2CPacket extends EntitySpawnS2CPacket {
    NbtCompound extraData;
    public ExtraDataEntitySpawnS2CPacket(int id, UUID uuid, double x, double y, double z, float pitch, float yaw, EntityType<?> entityTypeId, int entityData, Vec3d velocity) {
        super(id, uuid, x, y, z, pitch, yaw, entityTypeId, entityData, velocity);
    }

    public ExtraDataEntitySpawnS2CPacket(Entity entity) {
        super(entity);
    }

    public ExtraDataEntitySpawnS2CPacket(Entity entity, NbtCompound entityData) {
        super(entity);
        extraData = entityData;
    }

    public ExtraDataEntitySpawnS2CPacket(Entity entity, EntityType<?> entityType, int data, BlockPos pos) {
        super(entity, entityType, data, pos);
    }

    public ExtraDataEntitySpawnS2CPacket(PacketByteBuf buf) {
        super(buf);
        extraData = buf.readNbt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        super.write(buf);
        buf.writeNbt(extraData);
    }
}
