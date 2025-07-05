package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SphereNetworking {
    public static final Identifier SPHERE_UPDATE_PACKET =
            new Identifier(BCSMPStuff.MOD_ID, "sphere_update");

    /** Call this on the server whenever you spawn or update your ProtectionSphereEntity. */
    public static void sendSphereSettings(ProtectionSphereEntity entity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(entity.getUuid());
        buf.writeBlockPos(entity.getBlockPos());
        buf.writeDouble(entity.radius);
        buf.writeInt(entity.pointCount);
        buf.writeDouble(entity.turnSpeed);
        buf.writeDouble(entity.movementSpeed);
        buf.writeDouble(entity.lowerThreshold);
        buf.writeDouble(entity.upperThreshold);
        buf.writeDouble(entity.avoidanceRadius);
        buf.writeDouble(entity.avoidanceStrength);

        for (var p : entity.getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, SPHERE_UPDATE_PACKET, buf);
            }
        }
    }
}
