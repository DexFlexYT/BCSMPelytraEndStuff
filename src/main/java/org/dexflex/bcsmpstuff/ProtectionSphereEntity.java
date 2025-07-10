package org.dexflex.bcsmpstuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger("ProtectionSphereEntity");

    public ProtectionSphereEntity(EntityType<? extends ProtectionSphereEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        LOGGER.info("ProtectionSphereEntity constructed: id={}, world={}", this.getId(), world.isClient ? "client" : "server");
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

