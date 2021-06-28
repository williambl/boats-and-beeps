package com.williambl.boatsandbeeps.client

import com.williambl.boatsandbeeps.UpgradedBoatEntity
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.model.BoatEntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3f

class UpgradedBoatRenderer(context: EntityRendererFactory.Context) : EntityRenderer<UpgradedBoatEntity>(context) {
    private val texturesAndModels: Map<BoatEntity.Type, Pair<Identifier, BoatEntityModel>> = BoatEntity.Type.values().associate {
        it to Pair(
            Identifier("textures/entity/boat/" + it.getName() + ".png"),
            BoatEntityModel(context.getPart(EntityModelLayers.createBoat(it)))
        )
    }

    init {
        shadowRadius = 0.8f
    }

    override fun render(
        boatEntity: UpgradedBoatEntity,
        f: Float,
        g: Float,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int
    ) {
        matrixStack.push()
        matrixStack.translate(0.0, 0.375, 0.0)
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0f - f))
        val h = boatEntity.damageWobbleTicks.toFloat() - g
        var j = boatEntity.damageWobbleStrength - g
        if (j < 0.0f) {
            j = 0.0f
        }
        if (h > 0.0f) {
            matrixStack.multiply(
                Vec3f.POSITIVE_X.getDegreesQuaternion(
                    MathHelper.sin(h) * h * j / 10.0f * boatEntity.damageWobbleSide
                        .toFloat()
                )
            )
        }
        val k = boatEntity.interpolateBubbleWobble(g)
        if (!MathHelper.approximatelyEquals(k, 0.0f)) {
            matrixStack.multiply(Quaternion(Vec3f(1.0f, 0.0f, 1.0f), boatEntity.interpolateBubbleWobble(g), true))
        }
        val pair: Pair<Identifier, BoatEntityModel> = texturesAndModels[boatEntity.boatType]!!
        val identifier = pair.first
        val boatEntityModel = pair.second
        matrixStack.scale(-1.0f, -1.0f, 1.0f)
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0f))
        boatEntityModel.setAngles(boatEntity, g, 0.0f, -0.1f, 0.0f, 0.0f)
        val vertexConsumer = vertexConsumerProvider.getBuffer(boatEntityModel.getLayer(identifier))
        matrixStack.push()
        for (l in 0 until boatEntity.parts) {
            boatEntityModel.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f)
            matrixStack.translate(-2.0, 0.0, 0.0)
        }
        matrixStack.pop()
        for ((pos, blockstate) in boatEntity.upgrades.flatMapIndexed { idx, entry -> entry.map { Pair(it.key.position.add(-2.6666667*idx, 0.0, 0.0), it.value.blockstate) } }) {
            if (blockstate.renderType != BlockRenderType.INVISIBLE) {
                matrixStack.push()
                matrixStack.scale(-0.75f, -0.75f, 0.75f)
                matrixStack.translate(-pos.x, pos.y, pos.z)
                matrixStack.translate(-0.45, -0.25, 0.5)
                matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0f))
                renderBlock(boatEntity, g, blockstate, matrixStack, vertexConsumerProvider, i)
                matrixStack.pop()
            }
        }
        matrixStack.push()
        val vertexConsumer2 = vertexConsumerProvider.getBuffer(RenderLayer.getWaterMask())
        for (l in 0 until boatEntity.parts) {
            boatEntityModel.waterPatch.render(matrixStack, vertexConsumer2, i, OverlayTexture.DEFAULT_UV)
            matrixStack.translate(-2.0, 0.0, 0.0)
        }
        matrixStack.pop()
        matrixStack.pop()
        super.render(boatEntity, f, g, matrixStack, vertexConsumerProvider, i)
    }

    override fun getTexture(boatEntity: UpgradedBoatEntity): Identifier? {
        return texturesAndModels[boatEntity.boatType]?.first
    }

    private fun renderBlock(
        entity: UpgradedBoatEntity,
        delta: Float,
        state: BlockState,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        MinecraftClient.getInstance().blockRenderManager.renderBlockAsEntity(
            state,
            matrices,
            vertexConsumers,
            light,
            OverlayTexture.DEFAULT_UV
        )
    }
}