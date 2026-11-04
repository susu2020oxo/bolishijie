package cn.susu2020oxo.bolishijie.client;

import cn.susu2020oxo.bolishijie.block.GlassTerrain;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;

public final class GlassWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GlassTerrain.blocks().forEach(block -> BlockRenderLayerMap.putBlock(block, BlockRenderLayer.TRANSLUCENT));
    }
}
