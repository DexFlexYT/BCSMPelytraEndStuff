package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;

public class BCSMPStuffClient implements ClientModInitializer {
    public static final Identifier SPHERE_CONFIG_CHANNEL =
            new Identifier("bcsmp-stuff", "sphere_config");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SPHERE_CONFIG_CHANNEL,
                (client, handler, buf, responseSender) -> {
                    var pos = buf.readBlockPos();
                    double radius = buf.readDouble();
                    double avoidR = buf.readDouble();
                    double avoidS = buf.readDouble();
                    client.execute(() ->
                            SphereClientRenderer.createOrUpdate(pos, radius, avoidR, avoidS)
                    );
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                SphereClientRenderer.tickClient(client);
            }
        });
    }
}
