package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public class ClientNetworkHandler implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1) Safe copy before scheduling!
        ClientPlayNetworking.registerGlobalReceiver(
                SphereNetworking.SPHERE_UPDATE_PACKET, // "bcsmp-stuff:sphere_update"
                (client, handler, buf, responseSender) -> {
                    PacketByteBuf copy = PacketByteBufs.copy(buf);
                    client.execute(() -> SphereRenderer.handlePacket(copy));
                }
        );

        // 2) Tick render
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                SphereRenderer.tick(client)
        );

        // 3) Dummy renderer registration (as before)
        EntityRendererRegistry.register(
                BCSMPStuff.PROTECTION_SPHERE,
                (ctx) -> new EmptyEntityRenderer<>(ctx)
        );
    }
}
