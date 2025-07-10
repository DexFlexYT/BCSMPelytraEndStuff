package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SphereNetworking {
    public static final Identifier SPHERE_UPDATE_PACKET =
            new Identifier(BCSMPStuff.MOD_ID, "sphere_update");

    /**
     * Send sphere settings from server to client (no lineMode).
     */
    public static void sendSphereSettings(ProtectionSphereEntity entity) {
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
        // Removed lineMode boolean

        for (var p : entity.getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity sp) {
                ServerPlayNetworking.send(sp, SPHERE_UPDATE_PACKET, buf);
                BCSMPStuff.LOGGER.info("[SphereNetworking] Sent sphere update packet to player " + sp.getName().getString());
            }
        }
    }
}
