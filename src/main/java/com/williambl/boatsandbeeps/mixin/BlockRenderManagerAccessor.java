package com.williambl.boatsandbeeps.mixin;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderManager.class)
public interface BlockRenderManagerAccessor {
    @Accessor("blockColors")
    BlockColors getBlockColors();

    @Accessor("blockModelRenderer")
    BlockModelRenderer getBlockModelRenderer();

    @Accessor("builtinModelItemRenderer")
    BuiltinModelItemRenderer getBuiltinModelItemRenderer();
}
