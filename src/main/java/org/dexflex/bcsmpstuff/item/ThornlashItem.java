package org.dexflex.bcsmpstuff.item;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.dexflex.bcsmpstuff.BCSMPStuff;
import org.dexflex.bcsmpstuff.particle.ModParticles;

import java.util.Optional;
import java.util.UUID;

public class ThornlashItem extends Item {
    private static final double MAX_SPEED_BPS = 25.0;
    private static final double BASE_PULL = 1.0;

    public ThornlashItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (isLashing(stack)) {
            clearLash(stack);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
            return TypedActionResult.success(stack, world.isClient());
        }

        HitResult hitResult = raycast(world, player, 64.0);
        if (hitResult != null) handleHit(world, player, stack, hitResult);

        return TypedActionResult.success(stack, world.isClient());
    }


    private void handleHit(World world, PlayerEntity player, ItemStack stack, HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            if (target instanceof LivingEntity victim && !world.isClient) {
                victim.damage(DamageSource.player(player), 0.1f);
                setLashing(stack, true);
                stack.getOrCreateNbt().putUuid("ThornlashTarget", target.getUuid());
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 2.0f, 0.7f);
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hitResult;
            BlockPos pos = bhr.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            if (block.getRegistryEntry().isIn(BlockTags.LOGS)) {
                setLashing(stack, true);
                stack.getOrCreateNbt().putLong("ThornlashTargetBlock", pos.asLong());
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 2.0f, 0.7f);
            }
        }
    }

    public static void tickPulling(PlayerEntity player) {
        ItemStack activeStack = null;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof ThornlashItem && isLashing(stack)) {
                activeStack = stack;
                break;
            }
        }
        if (activeStack == null) return;
        NbtCompound nbt = activeStack.getNbt();
        if (nbt == null) { clearLash(activeStack); return; }

        Vec3d playerPos = player.getPos();
        Vec3d targetPos;
        UUID targetUUID = null;
        Entity entityTarget = null;

        if (nbt.contains("ThornlashTarget")) {
            targetUUID = nbt.getUuid("ThornlashTarget");
            targetPos = getEntityCenter(player, targetUUID);
            if (targetPos == null) { clearLash(activeStack); return; }

            if (player.world instanceof ServerWorld sw) {
                entityTarget = sw.getEntity(targetUUID);
                if (entityTarget == null) { clearLash(activeStack); return; }
            }
        } else if (nbt.contains("ThornlashTargetBlock")) {
            BlockPos bpos = BlockPos.fromLong(nbt.getLong("ThornlashTargetBlock"));
            targetPos = Vec3d.ofCenter(bpos);
        } else return;

        double dist2 = playerPos.squaredDistanceTo(targetPos);
        if (dist2 <= 6) {
            clearLash(activeStack);
            player.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
            return;
        }

        if (entityTarget != null && player.isSneaking()) {
            // Pull entity to player
            pullEntityToward(entityTarget, player.getPos());
        } else {
            // Pull player to target
            pullPlayerToward(player, targetPos);
        }

        drawLine((ServerWorld)player.world, player, playerPos, targetPos, activeStack);

        if (entityTarget instanceof ServerPlayerEntity lashedPlayer) {
            BlockPos pos = lashedPlayer.getBlockPos();
            if (lashedPlayer.world.getBlockState(pos).getBlock().getDefaultState().getFluidState().isIn(net.minecraft.tag.FluidTags.LAVA)) {
                clearLash(activeStack);
                lashedPlayer.world.playSound(null, lashedPlayer.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
                return;
            }
        }

        if (entityTarget != null && player != null) {
            double maxDistance = 64.0;
            double distance = entityTarget.getPos().distanceTo(player.getPos());

            if (distance > maxDistance) {
                cancelPull(entityTarget);
                cancelPull(player);
                player.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
                return;
            }
        }



    }

    private static void pullEntityToward(Entity entity, Vec3d target) {
        Vec3d direction = target.subtract(entity.getPos()).normalize();
        Vec3d velocity = entity.getVelocity();
        double vproj = velocity.dotProduct(direction);
        double maxPerTick = MAX_SPEED_BPS / 20.0;
        double ratio = vproj / maxPerTick;
        double factor = 1.0 - ratio;
        if (factor <= 0) return;
        factor = Math.min(factor, 2.0);

        Vec3d motion = direction.multiply(BASE_PULL * factor);
        entity.addVelocity(motion.x, motion.y, motion.z);
        entity.velocityModified = true;
    }

    private static void pullPlayerToward(PlayerEntity player, Vec3d target) {
        Vec3d velocity = player.getVelocity();
        Vec3d direction = target.subtract(player.getPos()).normalize();
        double vproj = velocity.dotProduct(direction);
        double maxPerTick = MAX_SPEED_BPS / 20.0;
        double ratio = vproj / maxPerTick;
        double factor = 1.0 - ratio;
        if (factor <= 0) return;
        factor = Math.min(factor, 2.0);

        Vec3d motion = direction.multiply(BASE_PULL * factor);
        player.addVelocity(motion.x, motion.y, motion.z);
        player.velocityModified = true;
    }

    public static boolean isBeingPulledBy(ServerPlayerEntity player, PlayerEntity byPlayer) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof ThornlashItem && isLashing(stack)) {
                UUID targetUUID = stack.getOrCreateNbt().contains("ThornlashTarget") ? stack.getOrCreateNbt().getUuid("ThornlashTarget") : null;
                if (targetUUID != null && targetUUID.equals(byPlayer.getUuid())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void unlashPlayer(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof ThornlashItem && isLashing(stack)) {
                clearLash(stack);
                break;
            }
        }
    }

    public static void cancelPull(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof ThornlashItem && isLashing(stack)) {
                clearLash(stack);
            }
        }
    }


    private static Vec3d getEntityCenter(PlayerEntity player, UUID uuid) {
        if (!(player.world instanceof ServerWorld sw)) return null;
        Entity ent = sw.getEntity(uuid);
        if (ent == null) return null;
        return ent.getPos().add(0, ent.getStandingEyeHeight() - 0.5, 0);
    }

    private static void drawLine(ServerWorld world, PlayerEntity player, Vec3d from, Vec3d to, ItemStack stack) {
        Vec3d eye = from.add(0, player.getStandingEyeHeight() - 0.5, 0);
        Vec3d delta = to.subtract(eye);
        double len = delta.length();
        int steps = Math.min(100, (int) (len / 0.3));
        for (int i = 0; i <= steps; i++) {
            Vec3d pt = eye.add(delta.multiply((double) i / steps));
            world.spawnParticles((DefaultParticleType) ModParticles.THORNLASH_LINE_PARTICLE_TYPE, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    private static HitResult raycast(World world, PlayerEntity player, double maxDistance) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        HitResult blockHit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player));

        double blockHitDistance = blockHit.getType() == HitResult.Type.BLOCK
                ? start.squaredDistanceTo(blockHit.getPos())
                : Double.MAX_VALUE;

        EntityHitResult closestEntityHit = null;
        double closestEntityDistSq = Double.MAX_VALUE;
        Double speed = BCSMPStuff.playerSpeeds.get(player);
        double velocityMag = speed != null ? speed : 0.0;
        for (Entity entity : world.getEntitiesByClass(Entity.class,
                player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0),
                e -> e.isAlive() && e.collides() && !e.equals(player))) {

            double distance = start.distanceTo(entity.getPos());

            double distFactor = (distance / maxDistance) * 1.7;
            double speedFactor = Math.min(velocityMag / MAX_SPEED_BPS, 1.0) * 1.2;
            double expansion = 0.3 + distFactor + speedFactor;

            Box expandedBox = entity.getBoundingBox().expand(expansion);

            Optional<Vec3d> intercept = expandedBox.raycast(start, end);
            if (intercept.isPresent()) {
                double distSq = start.squaredDistanceTo(intercept.get());
                if (distSq < closestEntityDistSq) {
                    closestEntityDistSq = distSq;
                    closestEntityHit = new EntityHitResult(entity, intercept.get());
                }
            }
        }

        if (closestEntityHit != null && closestEntityDistSq < blockHitDistance) {
            return closestEntityHit;
        } else {
            return blockHit;
        }
    }

    private static boolean isLashing(ItemStack stack) {
        return stack.getOrCreateNbt().getBoolean("ThornlashLashing");
    }

    private static void setLashing(ItemStack stack, boolean lashing) {
        stack.getOrCreateNbt().putBoolean("ThornlashLashing", lashing);
    }

    private static void clearLash(ItemStack stack) {
        stack.removeSubNbt("ThornlashLashing");
        stack.removeSubNbt("ThornlashTarget");
        stack.removeSubNbt("ThornlashTargetBlock");
    }
}
