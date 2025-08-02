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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class ThornlashItem extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger("ThornlashItem");
    private static final double MAX_SPEED_BPS = 25.0; // blocks per second threshold
    private static final double BASE_PULL = 1.0; // base pull strength per tick

    public ThornlashItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        //LOGGER.info("Thornlash used by player: {}, lashing state: {}", player.getName().getString(), isLashing(stack));

        if (isLashing(stack)) {
            //LOGGER.info("Clearing lash for player: {}", player.getName().getString());
            clearLash(stack);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
            return TypedActionResult.success(stack, world.isClient());
        }

        HitResult hitResult = raycast(world, player, 64.0);
        if (hitResult != null) handleHit(world, player, stack, hitResult);
        //else LOGGER.info("Raycast returned null (no hit)");

        return TypedActionResult.success(stack, world.isClient());
    }

    private void handleHit(World world, PlayerEntity player, ItemStack stack, HitResult hitResult) {
        //LOGGER.info("Raycast hit type: {}", hitResult.getType());
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            if (target instanceof LivingEntity victim && !world.isClient) {
                victim.damage(DamageSource.player(player), 0.1f);
                setLashing(stack, true);
                stack.getOrCreateNbt().putUuid("ThornlashTarget", target.getUuid());
                //LOGGER.info("Started lashing entity UUID: {} for {}", target.getUuid(), player.getName().getString());
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 2.0f, 0.7f);
            }

        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hitResult;
            BlockPos pos = bhr.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            if (block.getRegistryEntry().isIn(BlockTags.LOGS)) {
                setLashing(stack, true);
                stack.getOrCreateNbt().putLong("ThornlashTargetBlock", pos.asLong());
                //LOGGER.info("Started lashing block at {} for player: {}", pos, player.getName().getString());
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 2.0f, 0.7f);
            }
        }
    }

    public static void tickPulling(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof ThornlashItem)) return;
        if (!isLashing(stack)) return;

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) { clearLash(stack); return; }

        Vec3d playerPos = player.getPos();
        Vec3d targetPos;
        if (nbt.contains("ThornlashTarget")) {
            targetPos = getEntityCenter(player, nbt.getUuid("ThornlashTarget"));
            if (targetPos == null) { clearLash(stack); return; }
        } else if (nbt.contains("ThornlashTargetBlock")) {
            BlockPos bpos = BlockPos.fromLong(nbt.getLong("ThornlashTargetBlock"));
            targetPos = Vec3d.ofCenter(bpos);
        } else return;

        double dist2 = playerPos.squaredDistanceTo(targetPos);
        if (dist2 <= 9) {
            clearLash(stack);
            player.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
            return;
        }

        pullPlayerToward(player, targetPos);
        drawLine((ServerWorld)player.world, player, playerPos, targetPos, stack);
    }

    private static void pullPlayerToward(PlayerEntity player, Vec3d target) {
        Vec3d velocity = player.getVelocity();
        Vec3d direction = target.subtract(player.getPos()).normalize();
        // player speed along pull direction (blocks/tick)
        double vproj = velocity.dotProduct(direction);
        // normalize to blocks/sec
        double maxPerTick = MAX_SPEED_BPS / 20.0;
        double ratio = vproj / maxPerTick;
        // compute strength factor
        double factor = 1.0 - ratio;
        if (factor <= 0) return;
        // optional cap max factor to 2.0 for strong opposite pull
        factor = Math.min(factor, 2.0);

        Vec3d motion = direction.multiply(BASE_PULL * factor);
        player.addVelocity(motion.x, motion.y, motion.z);
        player.velocityModified = true;
    }

    private static Vec3d getEntityCenter(PlayerEntity player, UUID uuid) {
        if (!(player.world instanceof ServerWorld sw)) return null;
        Entity ent = sw.getEntity(uuid);
        if (ent == null) return null;
        return ent.getPos().add(0, ent.getStandingEyeHeight()-0.5, 0);
    }

    private static void drawLine(ServerWorld world, PlayerEntity player, Vec3d from, Vec3d to, ItemStack stack) {
        Vec3d eye = from.add(0, player.getStandingEyeHeight()-0.5, 0);
        Vec3d delta = to.subtract(eye);
        double len = delta.length();
        int steps = Math.min(100, (int)(len / 0.3));
        for (int i = 0; i <= steps; i++) {
            Vec3d pt = eye.add(delta.multiply((double)i / steps));
            world.spawnParticles(
                (DefaultParticleType) BCSMPStuff.THORNLASH_LINE_PARTICLE_TYPE,
                    pt.x, pt.y, pt.z,
                    1, 0, 0, 0, 0
            );
        }
    }


    private static Entity getTargetEntityByUuid(PlayerEntity player, UUID uuid) {
        for (Entity entity : player.getWorld().getEntitiesByClass(Entity.class, player.getBoundingBox().expand(40), e -> e.getUuid().equals(uuid))) {
            return entity;
        }
        return null;
    }

    private static HitResult raycast(World world, PlayerEntity player, double maxDistance) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        // Block raycast
        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));

        // Find entities in a bounding box along the ray
        EntityHitResult entityHit = null;
        double closestDistance = maxDistance;
        for (Entity entity : world.getEntitiesByClass(Entity.class,
                player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0),
                e -> e.isAlive() && e.collides() && !e.equals(player))) {

            // Calculate intercept with entity bounding box
            Box box = entity.getBoundingBox().expand(0.6);
            Optional<Vec3d> intercept = box.raycast(start, end);
            if (intercept.isPresent()) {
                double distance = start.distanceTo(intercept.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    entityHit = new EntityHitResult(entity, intercept.get());
                }
            }
        }

        // Decide which hit is closer
        if (entityHit != null && (blockHit == null || closestDistance < start.distanceTo(blockHit.getPos()))) {
            return entityHit;
        }

        return blockHit;
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
