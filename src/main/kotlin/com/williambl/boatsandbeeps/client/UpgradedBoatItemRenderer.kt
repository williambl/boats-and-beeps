package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.boat.UpgradedBoatEntity
import com.williambl.boatsandbeeps.readPartsAndUpgrades
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3f

class UpgradedBoatItemRenderer(val type: BoatEntity.Type): BuiltinItemRendererRegistry.DynamicItemRenderer {
    var upgradedBoatEntity: UpgradedBoatEntity? = null
    override fun render(
        stack: ItemStack,
        mode: ModelTransformation.Mode,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        if (upgradedBoatEntity == null || upgradedBoatEntity?.world != MinecraftClient.getInstance().world) {
            upgradedBoatEntity = MinecraftClient.getInstance().world?.let { UpgradedBoatEntity(it) }
        }
        upgradedBoatEntity?.let { boat ->
            val partsAndUpgrades = readPartsAndUpgrades(stack.getOrCreateSubTag("BoatData"))
            boat.parts = partsAndUpgrades.first
            boat.upgrades = partsAndUpgrades.second
            boat.boatType = type

            when (mode) {
                ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND, ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(1.5, 0.5, 0.5)
                    matrices.translate(0.0, 0.0, (-boat.parts*2.0+2.0))
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(20f))
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 160.0f, 0.0f, matrices, vertexConsumers, light)
                }
                ModelTransformation.Mode.THIRD_PERSON_LEFT_HAND, ModelTransformation.Mode.THIRD_PERSON_RIGHT_HAND -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(1.5, 0.5, 0.5)
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(20f))
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f))
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 0.0f, 0.0f, matrices, vertexConsumers, light)
                }
                ModelTransformation.Mode.GUI, ModelTransformation.Mode.FIXED -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(1.0, 0.5, 0.5)
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(10f))
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 0.0f, 0.0f, matrices, vertexConsumers, light)
                }
                ModelTransformation.Mode.GROUND -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(1.5, 0.5, 0.5)
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 160.0f, 0.0f, matrices, vertexConsumers, light)
                }
                ModelTransformation.Mode.HEAD -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(0.5, 0.5, 0.5)
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 0.0f, 0.0f, matrices, vertexConsumers, light)
                }
                else -> {
                    matrices.scale(0.5f, 0.5f, 0.5f)
                    matrices.translate(1.0, 0.5, 0.5)
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(20f))
                    MinecraftClient.getInstance().entityRenderDispatcher.render(boat, 0.0, 0.0, 0.0, 0.0f, 0.0f, matrices, vertexConsumers, light)
                }
            }


        }
    }
}