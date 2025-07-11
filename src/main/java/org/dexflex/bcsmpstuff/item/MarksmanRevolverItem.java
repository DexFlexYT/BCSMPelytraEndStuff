package org.dexflex.bcsmpstuff.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.dexflex.bcsmpstuff.ModSounds;

import java.util.HashSet;
import java.util.Set;

public class MarksmanRevolverItem extends Item {
    private static final int COOLDOWN_TICKS = 10;
    private static final double MAX_RANGE = 64.0;
    private static final int MAX_RICOCHETS = 5;
    private static final float BASE_DAMAGE = 4.0f;

    private static final SoundEvent MARKSMAN_SHOOT_SOUND = ModSounds.MARKSMAN_SHOOT;
    private static final SoundEvent COINFLIP_SOUND = ModSounds.COINFLIP;

    public MarksmanRevolverItem(Settings settings) {
        super(settings);
    }

    private void removeGroundedCoins(World world) {
        // Search for all gold nugget item entities that are on the ground and remove them
        for (Entity entity : world.getEntitiesByClass(ItemEntity.class, new Box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), e -> {
            ItemEntity item = (ItemEntity) e;
            return item.getStack().getItem() == Items.GOLD_NUGGET && item.isOnGround();
        })) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        Vec3d start = user.getCameraPosVec(1.0F);
        Vec3d direction = user.getRotationVec(1.0F).normalize();
        Vec3d maxEnd = start.add(direction.multiply(MAX_RANGE));

        EntityHitResult coinHitResult = null;
        Vec3d coinHitPos = null;
        ItemEntity coinHitEntity = null;

        if (!world.isClient) {
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    start, maxEnd,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    user
            ));

            Vec3d rayEnd = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getPos();

            double stepSize = 0.5;
            int steps = (int) (start.distanceTo(rayEnd) / stepSize);

            for (int i = 0; i <= steps; i++) {
                double progress = i / (double) steps;
                Vec3d point = start.lerp(rayEnd, progress);

                Box box = new Box(point.x - 0.5, point.y - 0.5, point.z - 0.5,
                        point.x + 0.5, point.y + 0.5, point.z + 0.5);

                for (Entity entity : world.getOtherEntities(user, box)) {
                    if (entity instanceof ItemEntity itemEntity) {
                        ItemStack itemStack = itemEntity.getStack();
                        if (itemStack.getItem() == Items.GOLD_NUGGET) {
                            coinHitResult = new EntityHitResult(itemEntity, point);
                            coinHitPos = point;
                            coinHitEntity = itemEntity;
                            break;
                        }
                    }
                }
                if (coinHitResult != null) break;
            }

            if (coinHitEntity != null) {
                // Remove all coins on the ground first
                removeGroundedCoins(world);

                // Start ricochet chain with base damage
                ricochetChain(world, user, coinHitEntity.getPos(), direction, new HashSet<>(), 0, BASE_DAMAGE);

                coinHitEntity.remove(Entity.RemovalReason.DISCARDED);
                user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            }
            else {
                EntityHitResult entityHitResult = null;
                double closestDistance = Double.MAX_VALUE;

                Box searchBox = new Box(start, rayEnd).expand(1.0);

                for (Entity entity : world.getOtherEntities(user, searchBox)) {
                    if (entity instanceof LivingEntity livingEntity && !livingEntity.isSpectator() && livingEntity.isAlive()) {
                        Box entityBox = entity.getBoundingBox().expand(0.3);
                        if (entityBox.raycast(start, rayEnd).isPresent()) {
                            double dist = start.distanceTo(entity.getPos());
                            if (dist < closestDistance) {
                                closestDistance = dist;
                                entityHitResult = new EntityHitResult(entity, entity.getPos());
                            }
                        }
                    }
                }

                Vec3d hitPos = rayEnd;

                if (entityHitResult != null) {
                    LivingEntity target = (LivingEntity) entityHitResult.getEntity();
                    target.damage(net.minecraft.entity.damage.DamageSource.player(user), BASE_DAMAGE);
                    hitPos = entityHitResult.getPos();
                }

                user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                spawnHitParticles(world, hitPos);
                spawnParticleLine(world, start, hitPos);
            }
        }

        if (!world.isClient) {
            float playerPitch = 0.9f + world.random.nextFloat() * 0.2f;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    MARKSMAN_SHOOT_SOUND, SoundCategory.PLAYERS, 0.1f, playerPitch);
        }

        return TypedActionResult.consume(stack);
    }

    private void ricochetChain(World world, PlayerEntity shooter, Vec3d fromPos, Vec3d direction,
                               Set<Entity> hitCoins, int ricochetCount, float currentDamage) {
        if (ricochetCount >= MAX_RICOCHETS) return;

        ItemEntity nearestCoin = findNearestCoin(world, fromPos, hitCoins, MAX_RANGE);
        LivingEntity nearestLiving = findNearestLiving(world, fromPos, shooter, MAX_RANGE);

        Entity target = nearestCoin != null ? nearestCoin : nearestLiving;

        if (target == null) {
            spawnParticleLine(world, fromPos, fromPos.add(direction.multiply(MAX_RANGE)));
            return;
        }

        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

        BlockHitResult blockHit = world.raycast(new RaycastContext(
                fromPos, targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                shooter
        ));

        if (blockHit.getType() != HitResult.Type.MISS && blockHit.getPos().distanceTo(fromPos) < fromPos.distanceTo(targetPos)) {
            spawnParticleLine(world, fromPos, blockHit.getPos());
            return;
        }

        spawnParticleLine(world, fromPos, targetPos);

        if (target instanceof ItemEntity coin) {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.END_ROD,
                        coin.getX(), coin.getY() + 0.5, coin.getZ(),
                        5, 0, 0, 0, 0.2
                );
            }
            world.playSound(null, coin.getX(), coin.getY(), coin.getZ(),
                    COINFLIP_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);

            coin.remove(Entity.RemovalReason.DISCARDED);
            hitCoins.add(coin);

            // Increase damage by 1 for each coin hit
            ricochetChain(world, shooter, targetPos, direction, hitCoins, ricochetCount + 1, currentDamage + 1.0f);

        } else if (target instanceof LivingEntity living) {
            living.damage(net.minecraft.entity.damage.DamageSource.player(shooter), currentDamage);
            spawnHitParticles(world, targetPos);
        }
    }

    private ItemEntity findNearestCoin(World world, Vec3d pos, Set<Entity> excludeCoins, double range) {
        Box searchBox = new Box(pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range);

        ItemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getOtherEntities(null, searchBox)) {
            if (entity instanceof ItemEntity itemEntity && !excludeCoins.contains(entity)) {
                if (itemEntity.getStack().getItem() == Items.GOLD_NUGGET) {
                    double dist = entity.squaredDistanceTo(pos);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = itemEntity;
                    }
                }
            }
        }
        return nearest;
    }

    private LivingEntity findNearestLiving(World world, Vec3d pos, PlayerEntity shooter, double range) {
        Box searchBox = new Box(pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range);

        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getOtherEntities(shooter, searchBox)) {
            if (entity instanceof LivingEntity living && !living.isSpectator() && living.isAlive()) {
                double dist = entity.squaredDistanceTo(pos);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = living;
                }
            }
        }
        return nearest;
    }

    private void spawnParticleLine(World world, Vec3d start, Vec3d end) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        int particles = 20;
        for (int i = 0; i <= particles; i++) {
            double progress = i / (double) particles;
            Vec3d pos = start.lerp(end, progress);
            serverWorld.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    private void spawnHitParticles(World world, Vec3d pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        DefaultParticleType particle = ParticleTypes.ELECTRIC_SPARK;

        for (int i = 0; i < 5; i++) {
            serverWorld.spawnParticles(
                    particle,
                    pos.x, pos.y, pos.z,
                    1,
                    0, 0, 0, 0
            );
        }
    }
}
