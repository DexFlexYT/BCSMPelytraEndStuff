package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BCSMPStuff implements ModInitializer {

	public static final String MOD_ID = "bcsmp-stuff";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClient && player.isFallFlying() && player.getStackInHand(hand).getItem() == Items.FIREWORK_ROCKET) {
				player.sendMessage(Text.literal("You can't use rockets while flying!"), true);
				return TypedActionResult.fail(player.getStackInHand(hand));
			}
			return TypedActionResult.pass(player.getStackInHand(hand));
		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!(world instanceof ServerWorld)) return;
			ServerWorld serverWorld = (ServerWorld) world;
			if (!serverWorld.isRaining() && !serverWorld.isThundering()) return;

			Random random = serverWorld.getRandom();

			if (random.nextInt(12) != 0) return;

			List<ServerPlayerEntity> players = serverWorld.getPlayers(player -> true);
			if (players.isEmpty()) return;

			ServerPlayerEntity player = players.get(random.nextInt(players.size()));

			double x = player.getX() + (random.nextDouble() * 20.0) - 10.0;
			double z = player.getZ() + (random.nextDouble() * 20.0) - 10.0;
			double y = 321.0;  // Fixed Y level

			ItemStack stack = new ItemStack(ModItems.SKYGLEAM);
			ItemEntity drop = new ItemEntity(serverWorld, x, y, z, stack);
			drop.setVelocity(0, -5.0, 0);
			serverWorld.spawnEntity(drop);
		});
	}
}