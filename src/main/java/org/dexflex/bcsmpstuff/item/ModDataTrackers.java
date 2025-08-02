package org.dexflex.bcsmpstuff.item;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;

public class ModDataTrackers {
    public static final TrackedData<Boolean> IS_LASHING = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final TrackedData<Integer> LASH_TARGET_ID = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // Store Vec3d via ItemStack NBT since Fabric 1.19 has no Vec3d data tracker
    public static void setBlockLashPos(ItemStack stack, Vec3d pos) {
        NbtCompound tag = stack.getOrCreateNbt();
        tag.putDouble("LashX", pos.x);
        tag.putDouble("LashY", pos.y);
        tag.putDouble("LashZ", pos.z);
    }

    public static Vec3d getBlockLashPos(ItemStack stack) {
        NbtCompound tag = stack.getOrCreateNbt();
        return new Vec3d(tag.getDouble("LashX"), tag.getDouble("LashY"), tag.getDouble("LashZ"));
    }

    public static void register(PlayerEntity player) {
        player.getDataTracker().startTracking(IS_LASHING, false);
        player.getDataTracker().startTracking(LASH_TARGET_ID, -1);
    }
}
