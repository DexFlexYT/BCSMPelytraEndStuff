// SphereMarkerEntity.java
package org.dexflex.bcsmpstuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

public class SphereMarkerEntity extends Entity {
    public double radius = 32;
    public int pointCount = 32;
    public double turnSpeed = 0.1;
    public double movementSpeed = 0.2;
    public double lowerThreshold = 18;
    public double upperThreshold = 26;
    public double avoidanceRadius = 16;
    public double avoidanceStrength = 0.1;

    public SphereMarkerEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        radius = nbt.getDouble("Radius");
        pointCount = nbt.getInt("PointCount");
        turnSpeed = nbt.getDouble("TurnSpeed");
        movementSpeed = nbt.getDouble("MovementSpeed");
        lowerThreshold = nbt.getDouble("LowerThreshold");
        upperThreshold = nbt.getDouble("UpperThreshold");
        avoidanceRadius = nbt.getDouble("AvoidanceRadius");
        avoidanceStrength = nbt.getDouble("AvoidanceStrength");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putDouble("Radius", radius);
        nbt.putInt("PointCount", pointCount);
        nbt.putDouble("TurnSpeed", turnSpeed);
        nbt.putDouble("MovementSpeed", movementSpeed);
        nbt.putDouble("LowerThreshold", lowerThreshold);
        nbt.putDouble("UpperThreshold", upperThreshold);
        nbt.putDouble("AvoidanceRadius", avoidanceRadius);
        nbt.putDouble("AvoidanceStrength", avoidanceStrength);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return null;
    }

    @Override
    public boolean shouldSave() {
        return true;
    }
}
