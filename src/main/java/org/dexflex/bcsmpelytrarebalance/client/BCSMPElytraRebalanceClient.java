package org.dexflex.bcsmpelytrarebalance.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import org.dexflex.bcsmpelytrarebalance.BCSMPElytraRebalance;

public class BCSMPElytraRebalanceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Render skylight with translucency so transparent PNG regions show through
        BlockRenderLayerMap.INSTANCE.putBlock(
                BCSMPElytraRebalance.SKYLIGHT_BLOCK,
                RenderLayer.getCutoutMipped()
        );
    }
}
