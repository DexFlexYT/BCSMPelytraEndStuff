package org.dexflex.bcsmpstuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;

public class ProtectionSphereEntity extends Entity {
    public double radius = 32;
    public int pointCount = 32;
    public double turnSpeed = 0.1;
    public double movementSpeed = 0.2;
    public double lowerThreshold = 18;
    public double upperThreshold = 26;
    public double avoidanceRadius = 16;
    public double avoidanceStrength = 0.1;

    public ProtectionSphereEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    public Packet<?> createSpawnPacket() {
        // This tells Fabric/Minecraft how to tell clients “hey, a new entity spawned here”
        return new EntitySpawnS2CPacket(this);
    }
    @Override
    protected void initDataTracker() {}

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
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        radius = tag.getDouble("radius");
        pointCount = tag.getInt("pointCount");
        turnSpeed = tag.getDouble("turnSpeed");
        movementSpeed = tag.getDouble("movementSpeed");
        lowerThreshold = tag.getDouble("lowerThreshold");
        upperThreshold = tag.getDouble("upperThreshold");
        avoidanceRadius = tag.getDouble("avoidanceRadius");
        avoidanceStrength = tag.getDouble("avoidanceStrength");
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            // Send S2C packet to nearby players
            SphereNetworking.sendSphereSettings(this);
        }
    }

    @Override
    public boolean shouldSave() {
        return true;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }


    @Override
    public boolean isSpectator() {
        return true;
    }
}
