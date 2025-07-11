package org.dexflex.bcsmpstuff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SphereRenderer {
    private static final Map<Integer, SphereData> spheres = new HashMap<>();
    private static final Random random = new Random();
    //private static final Logger LOGGER = LoggerFactory.getLogger("SphereRenderer");

    public static void handlePacket(PacketByteBuf buf) {
        //LOGGER.info("Received sphere update packet");
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

        SphereData data = spheres.get(entityId);
        if (data == null) {
            spheres.put(entityId, new SphereData(entityId, pos, radius, pointCount,
                    turnSpeed, movementSpeed, lowerThreshold, upperThreshold,
                    avoidR, avoidS));
            //LOGGER.info("Created new sphere for entity {}", entityId);
        } else {
            data.updateParams(pos, radius, turnSpeed, movementSpeed,
                    lowerThreshold, upperThreshold, avoidR, avoidS);
            //LOGGER.info("Updated sphere for entity {}", entityId);
        }
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) return;
        //LOGGER.info("SphereRenderer.tick called, spheres={}", spheres.size());

        Iterator<Map.Entry<Integer, SphereData>> iter = spheres.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, SphereData> entry = iter.next();
            int id = entry.getKey();
            SphereData data = entry.getValue();

            if (world.getEntityById(id) == null) {
                iter.remove();
                //LOGGER.info("Removed sphere for entity {} (entity missing)", id);
            } else {
                data.updateAndRender(world);
                // Only log the first sphere per tick to avoid spam
                if (id == spheres.keySet().iterator().next()) {
                    //LOGGER.info("Rendering sphere for entity {}", id);
                }
            }
        }
    }

    private static class SphereData {
        int entityId;
        BlockPos center;
        double radius, turnSpeed, movementSpeed;
        double lower, upper, avoidR, avoidS;
        List<Vec3d> positions;
        List<Vec3d> directions;

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

        void updateParams(BlockPos center, double radius,
                          double turnSpeed, double movementSpeed,
                          double lower, double upper,
                          double avoidR, double avoidS) {
            this.center = center;
            this.radius = radius;
            this.turnSpeed = turnSpeed;
            this.movementSpeed = movementSpeed;
            this.lower = lower;
            this.upper = upper;
            this.avoidR = avoidR;
            this.avoidS = avoidS;
        }

        void updateAndRender(ClientWorld world) {
            Vec3d c = Vec3d.ofCenter(center);

            for (int i = 0; i < positions.size(); i++) {
                Vec3d pos = positions.get(i);
                Vec3d dir = directions.get(i);

                world.addParticle(ParticleTypes.GLOW_SQUID_INK,
                        pos.x, pos.y, pos.z, 0, 0, 0);

                Vec3d avoid = Vec3d.ZERO;
                for (Vec3d other : positions) {
                    if (other == pos) continue;
                    double d = pos.distanceTo(other);
                    if (d < avoidR) {
                        Vec3d away = pos.subtract(other).normalize();
                        avoid = avoid.add(away.multiply(1 - d/avoidR));
                    }
                }
                if (avoid.lengthSquared() > 0) {
                    dir = dir.add(avoid.normalize().multiply(avoidS)).normalize();
                }

                dir = rotateAroundRandomAxis(dir, turnSpeed).normalize();
                Vec3d next = pos.add(dir.multiply(movementSpeed));
                Vec3d onSphere = next.subtract(c).normalize().multiply(radius).add(c);
                positions.set(i, onSphere);
                directions.set(i, dir);
            }

            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    double d = positions.get(i).distanceTo(positions.get(j));
                    if (d >= lower && d <= upper) {
                        drawProjectedLine(world, c, positions.get(i), positions.get(j));
                    }
                }
            }
        }
    }

    private static void drawProjectedLine(ClientWorld world, Vec3d c, Vec3d a, Vec3d b) {
        Vec3d diff = b.subtract(a).multiply(1.0 / 10);
        for (int i = 0; i <= 10; i++) {
            Vec3d p = a.add(diff.multiply(i));
            Vec3d offset = randomPerp(diff).multiply(0.3 * (random.nextDouble() - 0.5));
            Vec3d proj = c.add(p.add(offset).subtract(c).normalize().multiply(c.distanceTo(p)));
            world.addParticle(ParticleTypes.ELECTRIC_SPARK, proj.x, proj.y, proj.z, 0, 0, 0);
        }
    }

    private static Vec3d randomUnit() {
        double u = random.nextDouble(), v = random.nextDouble();
        double th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1);
        return new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
    }

    private static Vec3d rotateAroundRandomAxis(Vec3d v, double maxAngle) {
        Vec3d axis = randomUnit();
        double angle = (random.nextDouble() * 2 - 1) * maxAngle;
        return rotate(v, axis, angle).normalize();
    }

    private static Vec3d rotate(Vec3d v, Vec3d axis, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle), dot = v.dotProduct(axis);
        Vec3d cross = new Vec3d(axis.y * v.z - axis.z * v.y,
                axis.z * v.x - axis.x * v.z,
                axis.x * v.y - axis.y * v.x);
        return v.multiply(cos).add(cross.multiply(sin)).add(axis.multiply(dot * (1 - cos)));
    }

    private static Vec3d randomPerp(Vec3d v) {
        Vec3d a = randomUnit();
        Vec3d p = v.crossProduct(a);
        if (p.lengthSquared() < 1e-6) p = v.crossProduct(new Vec3d(a.z, a.x, a.y));
        return p.normalize();
    }
}
