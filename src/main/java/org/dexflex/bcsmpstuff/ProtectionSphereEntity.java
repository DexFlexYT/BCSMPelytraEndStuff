package org.dexflex.bcsmpstuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class ProtectionSphereEntity extends Entity {
    // Default values
    public double radius = 16.0;
    public int pointCount = 16;
    public double turnSpeed = 0.1;
    public double movementSpeed = 0.2;
    public double lowerThreshold = 8.0;
    public double upperThreshold = 12.0;
    public double avoidanceRadius = 4.0;
    public double avoidanceStrength = 0.2;

    public int pointColor = 0x33DE98;
    public int lineColor = 0xFFFFFF;
    public float pointSize = 1.0f;
    public float lineSize = 0.1f;
    public int pointLife = 15;
    public int lineLife = 1;

    public ProtectionSphereEntity(EntityType<? extends ProtectionSphereEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker() {}

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        tag.putDouble("radius", radius);
        tag.putInt("pointCount", pointCount);
        tag.putDouble("turnSpeed", turnSpeed);
        tag.putDouble("movementSpeed", movementSpeed);
        tag.putDouble("lowerThreshold", lowerThreshold);
        tag.putDouble("upperThreshold", upperThreshold);
        tag.putDouble("avoidanceRadius", avoidanceRadius);
        tag.putDouble("avoidanceStrength", avoidanceStrength);

        tag.putInt("pointColor", pointColor);
        tag.putInt("lineColor", lineColor);
        tag.putFloat("pointSize", pointSize);
        tag.putFloat("lineSize", lineSize);
        tag.putInt("pointLife", pointLife);
        tag.putInt("lineLife", lineLife);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        if (tag.contains("radius")) radius = tag.getDouble("radius");
        if (tag.contains("pointCount")) pointCount = tag.getInt("pointCount");
        if (tag.contains("turnSpeed")) turnSpeed = tag.getDouble("turnSpeed");
        if (tag.contains("movementSpeed")) movementSpeed = tag.getDouble("movementSpeed");
        if (tag.contains("lowerThreshold")) lowerThreshold = tag.getDouble("lowerThreshold");
        if (tag.contains("upperThreshold")) upperThreshold = tag.getDouble("upperThreshold");
        if (tag.contains("avoidanceRadius")) avoidanceRadius = tag.getDouble("avoidanceRadius");
        if (tag.contains("avoidanceStrength")) avoidanceStrength = tag.getDouble("avoidanceStrength");

        if (tag.contains("pointColor")) pointColor = tag.getInt("pointColor");
        if (tag.contains("lineColor")) lineColor = tag.getInt("lineColor");
        if (tag.contains("pointSize")) pointSize = tag.getFloat("pointSize");
        if (tag.contains("lineSize")) lineSize = tag.getFloat("lineSize");
        if (tag.contains("pointLife")) pointLife = tag.getInt("pointLife");
        if (tag.contains("lineLife")) lineLife = tag.getInt("lineLife");
    }

    @Override
    public boolean shouldSave() { return true; }

    @Override
    public boolean isInvisible() { return true; }

    @Override
    public boolean isSpectator() { return true; }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient && this.world instanceof ServerWorld) {
            SphereNetworking.sendSphereSettings(this);
        }
    }
}
