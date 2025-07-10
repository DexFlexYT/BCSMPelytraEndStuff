package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.network.PacketByteBuf;

import static org.dexflex.bcsmpstuff.SphereNetworking.SPHERE_UPDATE_PACKET;

public class ClientNetworkHandler implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SPHERE_UPDATE_PACKET,
                (client, handler, buf, responseSender) -> {
                    // Make a safe copy of the data to read later:
                    var copy = buf.copy();
                    client.execute(() -> SphereRenderer.handlePacket((PacketByteBuf) copy));
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> SphereRenderer.tick(client));

        EntityRendererRegistry.register(
                BCSMPStuff.PROTECTION_SPHERE,
                (EntityRendererFactory.Context ctx) -> new EmptyEntityRenderer<>(ctx)
        );
    }
}
