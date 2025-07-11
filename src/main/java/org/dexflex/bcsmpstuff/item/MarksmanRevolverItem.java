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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.dexflex.bcsmpstuff.ModSounds;

import java.util.Optional;

public class MarksmanRevolverItem extends Item {
    private static final int COOLDOWN_TICKS = 10;
    private static final double MAX_RANGE = 64.0;

    private static final SoundEvent MARKSMAN_SHOOT_SOUND = ModSounds.MARKSMAN_SHOOT;
    private static final SoundEvent COINFLIP_SOUND = ModSounds.COINFLIP;

    public MarksmanRevolverItem(Settings settings) {
        super(settings);
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

        Vec3d hitPos = maxEnd;
        EntityHitResult entityHitResult = null;

        if (!world.isClient) {
            HitResult blockHit = world.raycast(new RaycastContext(
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
                            entityHitResult = new EntityHitResult(itemEntity, point);
                            hitPos = point;
                            break;
                        }
                    }
                }
                if (entityHitResult != null) break;
            }

            if (entityHitResult == null) {
                Box box = new Box(start, rayEnd).expand(1.0);
                double closestDistance = Double.MAX_VALUE;

                for (Entity entity : world.getOtherEntities(user, box)) {
                    if (entity instanceof LivingEntity livingEntity && !livingEntity.isSpectator() && livingEntity.isAlive()) {
                        Box entityBox = entity.getBoundingBox().expand(0.3);
                        Optional<Vec3d> optional = entityBox.raycast(start, rayEnd);

                        if (optional.isPresent()) {
                            double distance = start.distanceTo(optional.get());
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                entityHitResult = new EntityHitResult(entity, optional.get());
                                hitPos = optional.get();
                            }
                        }
                    }
                }
            }

            if (entityHitResult != null) {
                Entity hitEntity = entityHitResult.getEntity();

                if (hitEntity instanceof ItemEntity itemEntity) {
                    ItemStack itemStack = itemEntity.getStack();
                    if (itemStack.getItem() == Items.GOLD_NUGGET) {
                        double searchRadius = 16.0;
                        Box searchBox = itemEntity.getBoundingBox().expand(searchRadius);

                        LivingEntity nearestTarget = null;
                        double nearestDistance = Double.MAX_VALUE;

                        for (Entity e : world.getOtherEntities(user, searchBox)) {
                            if (e instanceof LivingEntity living && !living.isSpectator() && living.isAlive() && !living.equals(user)) {
                                double dist = e.squaredDistanceTo(itemEntity);
                                if (dist < nearestDistance) {
                                    nearestDistance = dist;
                                    nearestTarget = living;
                                }
                            }
                        }

                        if (nearestTarget != null) {

                            nearestTarget.damage(net.minecraft.entity.damage.DamageSource.player(user), 4.0F);

                            world.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                    COINFLIP_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);


                            if (world instanceof ServerWorld serverWorld) {
                                serverWorld.spawnParticles(
                                        ParticleTypes.END_ROD,
                                        itemEntity.getX(), itemEntity.getY() + 0.5, itemEntity.getZ(),
                                        20,
                                        0.3, 0.3, 0.3,
                                        0.1
                                );
                            }

                            itemEntity.remove(Entity.RemovalReason.DISCARDED);


                            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);


                            spawnHitParticles(world, nearestTarget.getPos());

                            return TypedActionResult.consume(stack);
                        }
                    }
                } else if (hitEntity instanceof LivingEntity livingTarget) {

                    livingTarget.damage(net.minecraft.entity.damage.DamageSource.player(user), 4.0F);
                }
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPos = blockHit.getPos();
            }


            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            spawnHitParticles(world, hitPos);
        }

        if (!world.isClient) {
            float playerPitch = 0.9f + world.random.nextFloat() * 0.2f;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    MARKSMAN_SHOOT_SOUND, SoundCategory.PLAYERS, 0.1f, playerPitch);

            float hitPitch = 0.9f + world.random.nextFloat() * 0.2f;
            world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    MARKSMAN_SHOOT_SOUND, SoundCategory.PLAYERS, 0.1f, hitPitch);
        }

        if (world.isClient) {
            int particleCount = 20;
            for (int i = 0; i < particleCount; i++) {
                double progress = i / (double) particleCount;
                Vec3d particlePos = start.lerp(hitPos, progress);
                world.addParticle(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
        }

        return TypedActionResult.consume(stack);
    }

    private void spawnHitParticles(World world, Vec3d pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        DefaultParticleType particle = ParticleTypes.ELECTRIC_SPARK;

        for (int i = 0; i < 5; i++) {
            double offsetX = (world.random.nextDouble() - 0.5);
            double offsetY = (world.random.nextDouble() - 0.5);
            double offsetZ = (world.random.nextDouble() - 0.5);

            serverWorld.spawnParticles(
                    particle,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    1,
                    0, 0, 0, 0
            );
        }
    }
}
