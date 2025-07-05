package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProtectedSphere {
    private static final Vec3d CENTER = new Vec3d(-40, 112, 0);
    private static final double RADIUS = 32;

    private static final int POINT_COUNT = 32;
    private static final List<SpherePoint> surfacePoints = new ArrayList<>(POINT_COUNT);
    private static final double TURN_SPEED = 0.1;
    private static final double MOVEMENT_SPEED = 0.4;
    private static final double LOWER_THRESHOLD = 17.0;
    private static final double UPPER_THRESHOLD = 20.0;
    private static final double AVOIDANCE_RADIUS = 13.0;
    private static final double AVOIDANCE_STRENGTH = 0.1;

    private static final Random rand = new Random();

    public static void register() {
        for (int i = 0; i < POINT_COUNT; i++) {
            surfacePoints.add(SpherePoint.random());
        }
        ServerTickEvents.END_SERVER_TICK.register(ProtectedSphere::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            updateSurfacePoints();
            spawnParticles(world);
        }
    }

    private static void updateSurfacePoints() {
        for (SpherePoint sp : surfacePoints) {
            Vec3d avoidance = Vec3d.ZERO;
            for (SpherePoint other : surfacePoints) {
                if (sp == other) continue;
                double dist = sp.pos.distanceTo(other.pos);
                if (dist < AVOIDANCE_RADIUS) {
                    Vec3d away = sp.pos.subtract(other.pos).normalize();
                    avoidance = avoidance.add(away.multiply(1.0 - dist / AVOIDANCE_RADIUS));
                }
            }

            if (avoidance.lengthSquared() > 0) {
                avoidance = avoidance.normalize().multiply(AVOIDANCE_STRENGTH);
                sp.dir = sp.dir.add(avoidance).normalize();
            }

            sp.dir = rotateAroundRandomAxis(sp.dir, TURN_SPEED).normalize();
            Vec3d targetPos = sp.pos.add(sp.dir.multiply(MOVEMENT_SPEED));
            Vec3d fromCenter = targetPos.subtract(CENTER).normalize().multiply(RADIUS);
            sp.pos = CENTER.add(fromCenter);
        }
    }

    private static void spawnParticles(ServerWorld world) {
        for (SpherePoint sp : surfacePoints) {
            BlockPos bp = new BlockPos(sp.pos.x, sp.pos.y, sp.pos.z);
            if (!world.getBlockState(bp).isAir()) continue;
            for (ServerPlayerEntity pl : world.getPlayers()) {
                world.spawnParticles(pl, ParticleTypes.GLOW_SQUID_INK, true,
                        sp.pos.x, sp.pos.y, sp.pos.z, 1, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < surfacePoints.size(); i++) {
            Vec3d p1 = surfacePoints.get(i).pos;
            for (int j = i + 1; j < surfacePoints.size(); j++) {
                Vec3d p2 = surfacePoints.get(j).pos;
                double dist = p1.distanceTo(p2);
                if (dist >= LOWER_THRESHOLD && dist <= UPPER_THRESHOLD) {
                    spawnJaggedLine(world, p1, p2);
                }
            }
        }
    }

    private static void spawnJaggedLine(ServerWorld world, Vec3d start, Vec3d end) {
        int segments = 20;
        Vec3d diff = end.subtract(start);
        Vec3d step = diff.multiply(1.0 / segments);

        for (int i = 0; i <= segments; i++) {
            Vec3d point = start.add(step.multiply(i));
            Vec3d offset = randomPerpendicularVector(diff).multiply(0.3 * (rand.nextDouble() - 0.5));
            Vec3d pos = point.add(offset);

            // Project to sphere surface
            pos = CENTER.add(pos.subtract(CENTER).normalize().multiply(RADIUS));

            BlockPos bp = new BlockPos(pos.x, pos.y, pos.z);
            if (!world.getBlockState(bp).isAir()) continue;

            for (ServerPlayerEntity pl : world.getPlayers()) {
                world.spawnParticles(pl, ParticleTypes.ELECTRIC_SPARK, true,
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static Vec3d randomPerpendicularVector(Vec3d v) {
        Vec3d axis = randomUnitVector();
        Vec3d perp = v.crossProduct(axis);
        if (perp.lengthSquared() < 1e-6) {
            axis = new Vec3d(axis.z, axis.x, axis.y);
            perp = v.crossProduct(axis);
        }
        return perp.normalize();
    }

    private static Vec3d rotateAroundRandomAxis(Vec3d vec, double maxAngle) {
        Vec3d axis = randomUnitVector();
        double angle = (rand.nextDouble() * 2 - 1) * maxAngle;
        return rotateAroundAxis(vec, axis, angle);
    }

    private static Vec3d rotateAroundAxis(Vec3d v, Vec3d axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dotProduct(axis);
        Vec3d cross = new Vec3d(
                axis.y * v.z - axis.z * v.y,
                axis.z * v.x - axis.x * v.z,
                axis.x * v.y - axis.y * v.x
        );
        return v.multiply(cos)
                .add(cross.multiply(sin))
                .add(axis.multiply(dot * (1 - cos)));
    }

    private static class SpherePoint {
        Vec3d pos;
        Vec3d dir;

        SpherePoint(Vec3d pos, Vec3d dir) {
            this.pos = pos;
            this.dir = dir;
        }

        static SpherePoint random() {
            Vec3d d = randomUnitVector();
            Vec3d p = CENTER.add(d.multiply(RADIUS));
            return new SpherePoint(p, d);
        }
    }

    private static Vec3d randomUnitVector() {
        double u = rand.nextDouble();
        double v = rand.nextDouble();
        double theta = 2 * Math.PI * u;
        double phi = Math.acos(2 * v - 1);
        return new Vec3d(
                Math.sin(phi) * Math.cos(theta),
                Math.cos(phi),
                Math.sin(phi) * Math.sin(theta)
        ).normalize();
    }
}
