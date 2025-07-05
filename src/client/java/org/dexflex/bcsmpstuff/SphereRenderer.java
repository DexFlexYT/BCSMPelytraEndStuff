package org.dexflex.bcsmpstuff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class SphereRenderer {
    // Map of entity ID -> sphere data
    private static final Map<Integer, SphereData> spheres = new HashMap<>();
    private static final Random random = new Random();

    /**
     * Called when receiving the S2C packet.
     */
    public static void handlePacket(PacketByteBuf buf) {
        int entityId = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        double radius = buf.readDouble();
        int pointCount = buf.readInt();
        double turnSpeed = buf.readDouble();
        double movementSpeed = buf.readDouble();
        double lowerThreshold = buf.readDouble();
        double upperThreshold = buf.readDouble();
        double avoidR = buf.readDouble();
        double avoidS = buf.readDouble();

        spheres.put(entityId, new SphereData(entityId, pos, radius, pointCount,
                turnSpeed, movementSpeed, lowerThreshold, upperThreshold,
                avoidR, avoidS));
    }

    /**
     * Called each client tick to update and render spheres.
     */
    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) return;

        Iterator<Map.Entry<Integer, SphereData>> iter = spheres.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, SphereData> entry = iter.next();
            int entityId = entry.getKey();
            SphereData data = entry.getValue();

            // Remove if the entity no longer exists
            if (world.getEntityById(entityId) == null) {
                iter.remove();
            } else {
                data.updateAndRender(world);
            }
        }
    }

    private static class SphereData {
        final int entityId;
        final BlockPos center;
        final double radius, turnSpeed, movementSpeed;
        final double lower, upper, avoidR, avoidS;
        final List<Vec3d> positions;
        final List<Vec3d> directions;

        SphereData(int entityId, BlockPos center, double radius, int count,
                   double turnSpeed, double movementSpeed,
                   double lower, double upper,
                   double avoidR, double avoidS) {
            this.entityId = entityId;
            this.center = center;
            this.radius = radius;
            this.turnSpeed = turnSpeed;
            this.movementSpeed = movementSpeed;
            this.lower = lower;
            this.upper = upper;
            this.avoidR = avoidR;
            this.avoidS = avoidS;

            this.positions = new ArrayList<>(count);
            this.directions = new ArrayList<>(count);
            Vec3d c = Vec3d.ofCenter(center);
            for (int i = 0; i < count; i++) {
                Vec3d dir = randomUnit();
                positions.add(c.add(dir.multiply(radius)));
                directions.add(dir);
            }
        }

        void updateAndRender(ClientWorld world) {
            Vec3d c = Vec3d.ofCenter(center);

            // Move points on sphere
            for (int i = 0; i < positions.size(); i++) {
                Vec3d pos = positions.get(i);
                Vec3d dir = directions.get(i);

                // Avoidance
                Vec3d avoid = Vec3d.ZERO;
                for (Vec3d other : positions) {
                    if (pos == other) continue;
                    double d = pos.distanceTo(other);
                    if (d < avoidR) {
                        Vec3d away = pos.subtract(other).normalize();
                        avoid = avoid.add(away.multiply(1.0 - d / avoidR));
                    }
                }
                if (avoid.lengthSquared() > 0) {
                    dir = dir.add(avoid.normalize().multiply(avoidS)).normalize();
                }

                // Twist direction
                dir = rotate(dir, turnSpeed);
                Vec3d next = pos.add(dir.multiply(movementSpeed));
                Vec3d onSphere = next.subtract(c).normalize().multiply(radius).add(c);
                positions.set(i, onSphere);
                directions.set(i, dir);

                // Spawn surface particle
                world.addParticle(ParticleTypes.GLOW_SQUID_INK,
                        onSphere.x, onSphere.y, onSphere.z,
                        0, 0, 0);
            }

            // Draw connecting lines
            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    double d = positions.get(i).distanceTo(positions.get(j));
                    if (d >= lower && d <= upper) {
                        drawLine(world, c, positions.get(i), positions.get(j));
                    }
                }
            }
        }

        private void drawLine(ClientWorld world, Vec3d c,
                              Vec3d a, Vec3d b) {
            Vec3d diff = b.subtract(a).multiply(1.0 / 10);
            for (int i = 0; i <= 10; i++) {
                Vec3d p = a.add(diff.multiply(i));
                Vec3d offset = randomPerp(diff)
                        .multiply(0.3 * (random.nextDouble() - 0.5));
                Vec3d proj = c.add(p.add(offset)
                        .subtract(c)
                        .normalize().multiply(radius));
                world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        proj.x, proj.y, proj.z,
                        0, 0, 0);
            }
        }
    }

    // Utility methods
    static Vec3d randomUnit() {
        double u = random.nextDouble(), v = random.nextDouble();
        double th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1);
        return new Vec3d(
                Math.sin(ph) * Math.cos(th),
                Math.cos(ph),
                Math.sin(ph) * Math.sin(th)
        );
    }
    static Vec3d rotate(Vec3d v, double ang) {
        double yaw = (random.nextDouble() * 2 - 1) * ang;
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }
    static Vec3d randomPerp(Vec3d v) {
        Vec3d a = randomUnit();
        Vec3d p = v.crossProduct(a);
        if (p.lengthSquared() < 1e-6) p = v.crossProduct(
                new Vec3d(a.z, a.x, a.y)
        );
        return p.normalize();
    }
}
