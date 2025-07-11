package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dexflex.bcsmpstuff.item.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoinThrowHandler {

    //private static final Logger LOGGER = LogManager.getLogger("CoinThrowDebug");

    private static final SoundEvent MARKSMAN_COINFLIP_SOUND = ModSounds.COINFLIP;

    private static final Map<UUID, Vec3d> previousPositions = new HashMap<>();

    private static final Map<UUID, Vec3d> playerVelocities = new HashMap<>();

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (stack.getItem() == Items.GOLD_NUGGET) {
                boolean hasRevolver = player.getInventory().contains(new ItemStack(ModItems.MARKSMAN_REVOLVER));
                if (hasRevolver) {
                    if (!world.isClient) {

                        float playerPitch = 0.9f + world.random.nextFloat() * 0.2f;
                        world.playSound(null, player.getX(), player.getY() + player.getStandingEyeHeight(), player.getZ(),
                                MARKSMAN_COINFLIP_SOUND, SoundCategory.PLAYERS, 1.1f, playerPitch);
                        stack.decrement(1);

                        ItemStack thrownStack = new ItemStack(Items.GOLD_NUGGET);
                        ItemEntity thrownItem = new ItemEntity(world, player.getX(), player.getY() + player.getStandingEyeHeight(), player.getZ(), thrownStack);

                        Vec3d playerVelocity = playerVelocities.getOrDefault(player.getUuid(), Vec3d.ZERO);
                        Vec3d lookVec = player.getRotationVec(1.0F).normalize();

                        double forwardSpeed = 0.3;


                        Vec3d forwardVelocity = lookVec.multiply(forwardSpeed);


                        double momentumFactor = 1.2;
                        Vec3d momentumVelocity = playerVelocity.multiply(momentumFactor);


                        Vec3d throwVelocity = forwardVelocity.add(momentumVelocity).add(0, 0.15, 0);


                        thrownItem.setVelocity(throwVelocity);
                        thrownItem.setPickupDelay(12000);
                        world.spawnEntity(thrownItem);
                    }
                    return TypedActionResult.success(stack, world.isClient);
                }
            }
            return TypedActionResult.pass(stack);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerId = player.getUuid();
                Vec3d currentPos = new Vec3d(player.getX(), player.getY(), player.getZ());

                Vec3d prevPos = previousPositions.get(playerId);
                if (prevPos != null) {
                    Vec3d velocity = currentPos.subtract(prevPos);
                    playerVelocities.put(playerId, velocity);
                }

                previousPositions.put(playerId, currentPos);
            }
        });
    }
}
