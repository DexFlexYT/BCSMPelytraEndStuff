package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SphereNetworking {
    public static final Identifier SPHERE_UPDATE_PACKET =
            new Identifier(BCSMPStuff.MOD_ID, "sphere_update");
    private static final Logger LOGGER = LoggerFactory.getLogger("SphereNetworking");

    /**
     * Send sphere settings from server to client (no lineMode).
     */
    public static void sendSphereSettings(ProtectionSphereEntity entity) {
        for (var p : entity.getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity sp) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(entity.getId());
                buf.writeBlockPos(entity.getBlockPos());
                buf.writeDouble(entity.radius);
                buf.writeInt(entity.pointCount);
                buf.writeDouble(entity.turnSpeed);
                buf.writeDouble(entity.movementSpeed);
                buf.writeDouble(entity.lowerThreshold);
                buf.writeDouble(entity.upperThreshold);
                buf.writeDouble(entity.avoidanceRadius);
                buf.writeDouble(entity.avoidanceStrength);
                ServerPlayNetworking.send(sp, SPHERE_UPDATE_PACKET, buf);
                LOGGER.info("Sent sphere update packet for entity {} to player {}", entity.getId(), sp.getEntityName());
            }
        }
    }
}

