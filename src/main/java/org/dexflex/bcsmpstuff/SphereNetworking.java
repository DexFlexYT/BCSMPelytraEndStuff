package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SphereNetworking {
    public static final Identifier SPHERE_UPDATE_PACKET =
            new Identifier(BCSMPStuff.MOD_ID, "sphere_update");
    //private static final Logger LOGGER = LoggerFactory.getLogger("SphereNetworking");

    /**
     * Send sphere settings from server to client (no lineMode).
     */
    public static void sendSphereSettings(ProtectionSphereEntity entity) {
        for (var p : entity.getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity sp) {
                NbtCompound root = new NbtCompound();
                root.putInt("id", entity.getId());
                root.putLong("pos", entity.getBlockPos().asLong());
                root.putDouble("radius", entity.radius);
                root.putInt("pointCount", entity.pointCount);
                root.putDouble("turnSpeed", entity.turnSpeed);
                root.putDouble("movementSpeed", entity.movementSpeed);
                root.putDouble("lowerThreshold", entity.lowerThreshold);
                root.putDouble("upperThreshold", entity.upperThreshold);
                root.putDouble("avoidanceRadius", entity.avoidanceRadius);
                root.putDouble("avoidanceStrength", entity.avoidanceStrength);
                root.putInt("pointColor", entity.pointColor);
                root.putInt("lineColor", entity.lineColor);
                root.putFloat("pointSize", entity.pointSize);
                root.putFloat("lineSize", entity.lineSize);
                root.putInt("pointLife", entity.pointLife);
                root.putInt("lineLife", entity.lineLife);


                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeNbt(root);


                ServerPlayNetworking.send(sp, SPHERE_UPDATE_PACKET, buf);
            }
        }
    }

}
