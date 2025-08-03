package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.nbt.NbtCompound;
import org.dexflex.bcsmpstuff.block.ModBlocks;
import org.dexflex.bcsmpstuff.particle.InkDropletParticle;
import org.dexflex.bcsmpstuff.particle.ModParticles;
import org.dexflex.bcsmpstuff.particle.SphereParticle;
import org.dexflex.bcsmpstuff.particle.ThornlashLineParticle;

public class ClientNetworkHandler implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INK_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ECLIPSE_FLOWER, RenderLayer.getCutout());
        ParticleFactoryRegistry.getInstance().register(ModParticles.INKFLOWER_SEEDS, InkDropletParticle.Factory::new);



        ParticleFactoryRegistry.getInstance().register(
                ModParticles.SPHERE_PARTICLE_TYPE,
                spriteProvider -> new SphereParticle.Factory(spriteProvider)
        );
        ParticleFactoryRegistry.getInstance().register(
                ModParticles.THORNLASH_LINE_PARTICLE_TYPE,
                spriteProvider -> new ThornlashLineParticle.Factory(spriteProvider)
        );



        ClientPlayNetworking.registerGlobalReceiver(
                SphereNetworking.SPHERE_UPDATE_PACKET,
                (client, handler, buf, responseSender) -> {
                    NbtCompound nbt = buf.readNbt();
                    if (nbt != null) {
                        client.execute(() -> SphereRenderer.handlePacket(nbt));
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> SphereRenderer.tick(client));

        EntityRendererRegistry.register(
                BCSMPStuff.PROTECTION_SPHERE,
                EmptyEntityRenderer::new
        );
    }
}
