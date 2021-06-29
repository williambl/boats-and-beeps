package com.williambl.boatsandbeeps

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.util.Identifier

object DistHelper {
    fun sendSyncPartClientToServer(syncId: Int, currentPart: Int) {
        ClientPlayNetworking.send(Identifier("boats-and-beeps:sync_part"), PacketByteBufs.create().writeVarInt(syncId).writeVarInt(currentPart))
    }
}