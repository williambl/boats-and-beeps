package com.williambl.boatsandbeeps

import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d

object DistHelper {
    fun sendSyncPartClientToServer(syncId: Int, currentPart: Int) {
        ClientPlayNetworking.send(Identifier("boats-and-beeps:sync_part"), PacketByteBufs.create().writeVarInt(syncId).writeVarInt(currentPart))
    }

    fun sendInteractAtPArtClientToServer(boat: UpgradedBoatEntity, hand: Hand, hitPos: Vec3d, part: Int) {
        ClientPlayNetworking.send(Identifier("boats-and-beeps:interact"),
            PacketByteBufs.create().writeVarInt(boat.id).writeEnumConstant(hand).also { it.writeDouble(hitPos.x).writeDouble(hitPos.y).writeDouble(hitPos.z) }.writeVarInt(part)
        )
    }
}