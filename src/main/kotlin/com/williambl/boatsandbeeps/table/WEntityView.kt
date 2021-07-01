package com.williambl.boatsandbeeps.table

import com.mojang.blaze3d.systems.RenderSystem
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3f

class WEntityView(var entity: Entity? = null): WWidget() {

    @Environment(EnvType.CLIENT)
    private var backgroundPainter: BackgroundPainter? = background

    /**
     * Sets the [BackgroundPainter] of this panel.
     *
     * @param painter the new painter
     * @return this panel
     */
    @Environment(EnvType.CLIENT)
    fun setBackgroundPainter(painter: BackgroundPainter?): WEntityView {
        backgroundPainter = painter
        return this
    }

    /**
     * Gets the current [BackgroundPainter] of this panel.
     *
     * @return the painter
     */
    @Environment(EnvType.CLIENT)
    fun getBackgroundPainter(): BackgroundPainter? {
        return backgroundPainter
    }

    @Environment(EnvType.CLIENT)
    override fun paint(matrices: MatrixStack?, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter?.paintBackground(matrices, x, y, this)

        entity?.let {
            drawEntity(x+width/2, (y+height/1.27).toInt(), 25, 120.0f, 10.0f, it)
        }
    }

    override fun canResize(): Boolean {
        return true
    }

    @Environment(EnvType.CLIENT)
    fun drawEntity(x: Int, y: Int, size: Int, f: Float, g: Float, entityToDraw: Entity) {
        val matrixStack = RenderSystem.getModelViewStack()
        matrixStack.push()
        matrixStack.translate(x.toDouble(), y.toDouble(), 1050.0)
        matrixStack.scale(1.0f, 1.0f, -1.0f)
        RenderSystem.applyModelViewMatrix()
        val matrixStack2 = MatrixStack()
        matrixStack2.translate(0.0, 0.0, 1000.0)
        matrixStack2.scale(size.toFloat(), -size.toFloat(), size.toFloat())
        matrixStack2.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-g))
        DiffuseLighting.method_34742()
        val entityRenderDispatcher = MinecraftClient.getInstance().entityRenderDispatcher
        entityRenderDispatcher.setRenderShadows(false)
        val immediate = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers
        RenderSystem.runAsFancy {
            entityRenderDispatcher.render(
                entityToDraw,
                0.0,
                0.0,
                0.0,
                f,
                1.0f,
                matrixStack2,
                immediate,
                15728880
            )
        }
        immediate.draw()
        entityRenderDispatcher.setRenderShadows(true)
        matrixStack.pop()
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.enableGuiDepthLighting()
    }

    companion object {
        @JvmField
        @Environment(EnvType.CLIENT)
        val background = BackgroundPainter.createNinePatch(Identifier("boats-and-beeps:textures/gui/entity_view_background.png"))
    }
}