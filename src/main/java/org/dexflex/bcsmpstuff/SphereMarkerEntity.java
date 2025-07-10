package org.dexflex.bcsmpstuff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

public class SphereMarkerEntity extends Entity {
    private double radius, avoidanceRadius, avoidanceStrength;

    public SphereMarkerEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    // Constructor & data getters...

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        tag.putDouble("radius", radius);
        tag.putDouble("avoidanceRadius", avoidanceRadius);
        tag.putDouble("avoidanceStrength", avoidanceStrength);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return null;
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        radius = tag.getDouble("radius");
        avoidanceRadius = tag.getDouble("avoidanceRadius");
        avoidanceStrength = tag.getDouble("avoidanceStrength");
    }
}
