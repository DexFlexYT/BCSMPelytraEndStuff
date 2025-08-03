package org.dexflex.bcsmpstuff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.dexflex.bcsmpstuff.particle.SphereParticleEffect;

import java.util.*;

public class SphereRenderer {
    private static final Map<Integer, SphereData> spheres = new HashMap<>();
    private static final Random random = new Random();

    public static void handlePacket(NbtCompound tag) {
        int entityId = tag.getInt("id");
        BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));
        double radius = tag.getDouble("radius");
        int pointCount = tag.getInt("pointCount");
        double turnSpeed = tag.getDouble("turnSpeed");
        double movementSpeed = tag.getDouble("movementSpeed");
        double lower = tag.getDouble("lowerThreshold");
        double upper = tag.getDouble("upperThreshold");
        double avoidR = tag.getDouble("avoidanceRadius");
        double avoidS = tag.getDouble("avoidanceStrength");

        int pointLife = tag.getInt("pointLife");
        int lineLife = tag.getInt("lineLife");

        float pointSize = tag.contains("pointSize") ? tag.getFloat("pointSize") : 0.25f;
        float lineSize = tag.contains("lineSize") ? tag.getFloat("lineSize") : 0.1f;


        int pointColor = tag.contains("pointColor") ? tag.getInt("pointColor") : 0x33DE98;


        int lineColor = tag.contains("lineColor") ? tag.getInt("lineColor") : 0xFF6666;


        float pr = ((pointColor >> 16) & 0xFF) / 255.0f;
        float pg = ((pointColor >> 8) & 0xFF) / 255.0f;
        float pb = (pointColor & 0xFF) / 255.0f;

        float lr = ((lineColor >> 16) & 0xFF) / 255.0f;
        float lg = ((lineColor >> 8) & 0xFF) / 255.0f;
        float lb = (lineColor & 0xFF) / 255.0f;

        SphereData data = spheres.get(entityId);
        if (data == null) {
            data = new SphereData(entityId, pos, radius, pointCount, turnSpeed, movementSpeed,
                    lower, upper, avoidR, avoidS,
                    pointLife, pr, pg, pb, pointSize,
                    lineLife, lr, lg, lb, lineSize);
            spheres.put(entityId, data);
        } else {
            data.updateParams(pos, radius, turnSpeed, movementSpeed,
                    lower, upper, avoidR, avoidS,
                    pointLife, pr, pg, pb, pointSize,
                    lineLife, lr, lg, lb, lineSize);
        }
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) return;

        Iterator<Map.Entry<Integer, SphereData>> iter = spheres.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, SphereData> entry = iter.next();
            int id = entry.getKey();
            SphereData data = entry.getValue();

            if (world.getEntityById(id) == null) {
                iter.remove();
            } else {
                data.updateAndRender(world);
            }
        }
    }

    private static class SphereData {
        int entityId;
        BlockPos center;
        double radius, turnSpeed, movementSpeed;
        double lower, upper, avoidR, avoidS;

        int pointLife;
        float pr, pg, pb;

        int lineLife;
        float pointSize, lineSize;

        float lr, lg, lb;

        List<Vec3d> positions;
        List<Vec3d> directions;

        SphereData(int entityId, BlockPos center, double radius, int count,
                   double turnSpeed, double movementSpeed,
                   double lower, double upper,
                   double avoidR, double avoidS,
                   int pointLife, float pr, float pg, float pb, float pointSize,
                   int lineLife, float lr, float lg, float lb, float lineSize) {
            this.entityId = entityId;
            this.center = center;
            this.radius = radius;
            this.turnSpeed = turnSpeed;
            this.movementSpeed = movementSpeed;
            this.lower = lower;
            this.upper = upper;
            this.avoidR = avoidR;
            this.avoidS = avoidS;

            this.pointSize = pointSize;
            this.lineSize = lineSize;


            this.pointLife = pointLife;
            this.pr = pr; this.pg = pg; this.pb = pb;

            this.lineLife = lineLife;
            this.lr = lr; this.lg = lg; this.lb = lb;

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
                          double avoidR, double avoidS,
                          int pointLife, float pr, float pg, float pb, float pointSize,
                          int lineLife, float lr, float lg, float lb, float lineSize) {
            this.center = center;
            this.radius = radius;
            this.turnSpeed = turnSpeed;
            this.movementSpeed = movementSpeed;
            this.lower = lower;
            this.upper = upper;
            this.avoidR = avoidR;
            this.avoidS = avoidS;

            this.pointSize = pointSize;
            this.lineSize = lineSize;

            this.pointLife = pointLife;
            this.pr = pr; this.pg = pg; this.pb = pb;

            this.lineLife = lineLife;
            this.lr = lr; this.lg = lg; this.lb = lb;
        }

        void updateAndRender(ClientWorld world) {
            Vec3d c = Vec3d.ofCenter(center);

            for (int i = 0; i < positions.size(); i++) {
                Vec3d pos = positions.get(i);
                Vec3d dir = directions.get(i);

                world.addParticle(new SphereParticleEffect(new Vec3f(pr, pg, pb), pointSize, pointLife),
                        pos.x, pos.y, pos.z, 0, 0, 0);

                Vec3d avoid = Vec3d.ZERO;
                for (Vec3d other : positions) {
                    if (other == pos) continue;
                    double d = pos.distanceTo(other);
                    if (d < avoidR) {
                        Vec3d away = pos.subtract(other).normalize();
                        avoid = avoid.add(away.multiply(1 - d / avoidR));
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

        private void drawProjectedLine(ClientWorld world, Vec3d c, Vec3d a, Vec3d b) {
            Vec3d diff = b.subtract(a).multiply(1.0 / 10);
            for (int i = 0; i <= 10; i++) {
                Vec3d p = a.add(diff.multiply(i));
                Vec3d proj = c.add(p.subtract(c).normalize().multiply(c.distanceTo(p)));


                world.addParticle(new SphereParticleEffect(new Vec3f(lr, lg, lb), lineSize, lineLife),
                        proj.x, proj.y, proj.z, 0, 0, 0);
            }
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
