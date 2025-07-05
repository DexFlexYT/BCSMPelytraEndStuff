package org.dexflex.bcsmpstuff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class SphereRenderer {
    private static final Map<UUID, SphereData> spheres = new HashMap<>();
    private static final Random random = new Random();

    public static void handlePacket(PacketByteBuf buf) {
        UUID id = buf.readUuid();
        BlockPos pos = buf.readBlockPos();
        double radius = buf.readDouble();
        int pointCount = buf.readInt();
        double turnSpeed = buf.readDouble();
        double movementSpeed = buf.readDouble();
        double lower = buf.readDouble();
        double upper = buf.readDouble();
        double avoidR = buf.readDouble();
        double avoidS = buf.readDouble();

        spheres.put(id, new SphereData(pos, radius, pointCount, turnSpeed, movementSpeed,
                lower, upper, avoidR, avoidS));
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) return;
        for (Iterator<Map.Entry<UUID, SphereData>> it = spheres.entrySet().iterator(); it.hasNext();) {
            SphereData data = it.next().getValue();
            // Remove if entity no longer exists
            if (world.getEntityById(data.entityId) == null) {
                it.remove();
            } else {
                data.updateAndRender(world);
            }
        }
    }

    private static class SphereData {
        final int entityId;
        final BlockPos center;
        final double radius, turnSpeed, movementSpeed, lower, upper, avoidR, avoidS;
        final List<Vec3d> positions;
        final List<Vec3d> directions;

        SphereData(BlockPos center, double radius, int count,
                   double turnSpeed, double movementSpeed,
                   double lower, double upper,
                   double avoidR, double avoidS) {
            this.entityId = -1; // you may store entity id if needed
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
            // Update points
            for (int i = 0; i < positions.size(); i++) {
                Vec3d pos = positions.get(i);
                Vec3d dir = directions.get(i);
                // avoidance
                Vec3d avoid = Vec3d.ZERO;
                for (Vec3d other : positions) {
                    if (pos == other) continue;
                    double d = pos.distanceTo(other);
                    if (d < avoidR) avoid = avoid.add(pos.subtract(other).normalize().multiply(1 - d/avoidR));
                }
                if (avoid.lengthSquared() > 0) dir = dir.add(avoid.normalize().multiply(avoidS)).normalize();
                // rotate
                dir = rotate(dir, turnSpeed);
                Vec3d next = pos.add(dir.multiply(movementSpeed));
                Vec3d onSphere = next.subtract(c).normalize().multiply(radius).add(c);
                positions.set(i, onSphere);
                directions.set(i, dir);
                world.addParticle(ParticleTypes.GLOW_SQUID_INK, onSphere.x, onSphere.y, onSphere.z, 0,0,0);
            }
            // lines
            for (int i = 0; i < positions.size(); i++) {
                for (int j = i+1; j < positions.size(); j++) {
                    double d = positions.get(i).distanceTo(positions.get(j));
                    if (d >= lower && d <= upper) drawLine(world, c, positions.get(i), positions.get(j));
                }
            }
        }

        void drawLine(ClientWorld world, Vec3d c, Vec3d a, Vec3d b) {
            Vec3d diff = b.subtract(a).multiply(1.0/10);
            for (int i=0; i<=10; i++) {
                Vec3d p = a.add(diff.multiply(i));
                Vec3d offset = randomPerp(diff).multiply(0.3*(random.nextDouble()-0.5));
                Vec3d proj = c.add(p.add(offset).subtract(c).normalize().multiply(radius));
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, proj.x, proj.y, proj.z, 0,0,0);
            }
        }

        static Vec3d randomUnit() {
            double u=random.nextDouble(), v=random.nextDouble();
            double th=2*Math.PI*u, ph=Math.acos(2*v-1);
            return new Vec3d(Math.sin(ph)*Math.cos(th),Math.cos(ph),Math.sin(ph)*Math.sin(th));
        }

        static Vec3d rotate(Vec3d v,double ang) {
            double yaw=(random.nextDouble()*2-1)*ang;
            double c=Math.cos(yaw),s=Math.sin(yaw);
            return new Vec3d(v.x*c-v.z*s,v.y,v.x*s+v.z*c).normalize();
        }

        static Vec3d randomPerp(Vec3d v) {
            Vec3d a=randomUnit();
            Vec3d p=v.crossProduct(a);
            if(p.lengthSquared()<1e-6) p=v.crossProduct(new Vec3d(a.z,a.x,a.y));
            return p.normalize();
        }
    }
}
