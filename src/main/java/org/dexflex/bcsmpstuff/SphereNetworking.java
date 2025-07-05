// SphereNetworking.java
package org.dexflex.bcsmpstuff;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class SphereNetworking {
    public static final Identifier SPHERE_UPDATE_PACKET = new Identifier(BCSMPStuff.MOD_ID, "sphere_update");

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

    public static class SphereRenderer {
        private static final Map<UUID, Sphere> spheres = new HashMap<>();
        private static final Random random = new Random();

        public static void handlePacket(PacketByteBuf buf) {
            UUID id = buf.readUuid();
            BlockPos pos = buf.readBlockPos();
            double radius = buf.readDouble();
            int pointCount = buf.readInt();
            double turn = buf.readDouble();
            double move = buf.readDouble();
            double lower = buf.readDouble();
            double upper = buf.readDouble();
            double avoidR = buf.readDouble();
            double avoidS = buf.readDouble();

            spheres.put(id, new Sphere(pos, radius, pointCount, turn, move, lower, upper, avoidR, avoidS));
        }

        public static void tick(MinecraftClient client) {
            if (client.world == null) return;
            spheres.values().forEach(s -> s.update(client));
        }

        private static class Sphere {
            final BlockPos center;
            final double radius, turnSpeed, movementSpeed, lower, upper, avoidRadius, avoidStrength;
            final List<Vec3d> positions = new ArrayList<>();
            final List<Vec3d> directions = new ArrayList<>();

            Sphere(BlockPos center, double r, int count, double turn, double move, double l, double u, double ar, double as) {
                this.center = center;
                this.radius = r;
                this.turnSpeed = turn;
                this.movementSpeed = move;
                this.lower = l;
                this.upper = u;
                this.avoidRadius = ar;
                this.avoidStrength = as;
                for (int i = 0; i < count; i++) {
                    Vec3d dir = randomUnit();
                    positions.add(Vec3d.ofCenter(center).add(dir.multiply(r)));
                    directions.add(dir);
                }
            }

            void update(MinecraftClient client) {
                Vec3d c = Vec3d.ofCenter(center);

                for (int i = 0; i < positions.size(); i++) {
                    Vec3d pos = positions.get(i);
                    Vec3d dir = directions.get(i);

                    Vec3d avoidance = Vec3d.ZERO;
                    for (int j = 0; j < positions.size(); j++) {
                        if (i == j) continue;
                        Vec3d other = positions.get(j);
                        double d = pos.distanceTo(other);
                        if (d < avoidRadius) {
                            Vec3d away = pos.subtract(other).normalize();
                            avoidance = avoidance.add(away.multiply(1.0 - d / avoidRadius));
                        }
                    }
                    if (avoidance.lengthSquared() > 0)
                        dir = dir.add(avoidance.normalize().multiply(avoidStrength)).normalize();

                    dir = rotate(dir, turnSpeed);
                    Vec3d next = pos.add(dir.multiply(movementSpeed));
                    Vec3d fromC = next.subtract(c).normalize().multiply(radius);
                    positions.set(i, c.add(fromC));
                    directions.set(i, dir);
                    client.world.addParticle(ParticleTypes.GLOW_SQUID_INK, fromC.x + c.x, fromC.y + c.y, fromC.z + c.z, 0, 0, 0);
                }

                for (int i = 0; i < positions.size(); i++) {
                    for (int j = i + 1; j < positions.size(); j++) {
                        double d = positions.get(i).distanceTo(positions.get(j));
                        if (d >= lower && d <= upper)
                            drawLine(c, positions.get(i), positions.get(j), client);
                    }
                }
            }

            void drawLine(Vec3d center, Vec3d a, Vec3d b, MinecraftClient client) {
                Vec3d d = b.subtract(a);
                for (int i = 0; i <= 10; i++) {
                    Vec3d p = a.add(d.multiply(i / 10.0));
                    Vec3d offset = randomPerp(d).multiply(0.3 * (random.nextDouble() - 0.5));
                    Vec3d projected = center.add(p.add(offset).subtract(center).normalize().multiply(radius));
                    client.world.addParticle(ParticleTypes.ELECTRIC_SPARK, projected.x, projected.y, projected.z, 0, 0, 0);
                }
            }
        }

        static Vec3d randomUnit() {
            double u = random.nextDouble(), v = random.nextDouble();
            double th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1);
            return new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
        }

        static Vec3d rotate(Vec3d v, double ang) {
            double yaw = (random.nextDouble() * 2 - 1) * ang;
            double c = Math.cos(yaw), s = Math.sin(yaw);
            return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
        }

        static Vec3d randomPerp(Vec3d v) {
            Vec3d a = randomUnit();
            Vec3d p = v.crossProduct(a);
            if (p.lengthSquared() < 1e-6) p = v.crossProduct(new Vec3d(a.z, a.x, a.y));
            return p.normalize();
        }
    }
}
