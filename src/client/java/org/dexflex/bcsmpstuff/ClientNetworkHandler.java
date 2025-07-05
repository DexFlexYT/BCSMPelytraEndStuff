package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ClientNetworkHandler implements ClientModInitializer {
    public static final Identifier SPHERE_UPDATE_PACKET = SphereNetworking.SPHERE_UPDATE_PACKET;

    @Override
    public void onInitializeClient() {
        // Register packet receiver
        ClientPlayNetworking.registerGlobalReceiver(SPHERE_UPDATE_PACKET, (client, handler, buf, responseSender) -> {
            // Handle on main thread
            client.execute(() -> ClientNetworkHandler.handlePacket(buf));
        });

        // Register tick event to render spheres
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SphereRenderer.tick(client);
        });
    }

    private static void handlePacket(PacketByteBuf buf) {
        SphereRenderer.handlePacket(buf);
    }
}