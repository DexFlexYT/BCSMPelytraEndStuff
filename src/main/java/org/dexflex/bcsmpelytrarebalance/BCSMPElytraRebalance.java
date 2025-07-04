package org.dexflex.bcsmpelytrarebalance;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class BCSMPElytraRebalance implements ModInitializer {

	public static final String MOD_ID = "bcsmpelytrarebalance";

	public static final Item SKYGLEAM = Registry.register(
			Registries.ITEM,
			new Identifier(MOD_ID, "skygleam"),
			new SkygleamItem(new Item.Settings())
	);

	public static final Block SKYLIGHT_BLOCK = Registry.register(
			Registries.BLOCK,
			new Identifier(MOD_ID, "skylight"),
			new SkylightBlock(FabricBlockSettings.create().strength(0.3f).luminance(15).nonOpaque())
	);


	public static final Item SKYLIGHT_ITEM = Registry.register(
			Registries.ITEM,
			new Identifier(MOD_ID, "skylight"),
			new BlockItem(SKYLIGHT_BLOCK, new Item.Settings())
	);

	@Override
	public void onInitialize() {
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

			if (random.nextInt(1250) != 0) return;

			List<ServerPlayerEntity> players = serverWorld.getPlayers(player -> true);
			if (players.isEmpty()) return;

			ServerPlayerEntity player = players.get(random.nextInt(players.size()));

			double x = player.getX() + (random.nextDouble() * 20.0) - 10.0;
			double z = player.getZ() + (random.nextDouble() * 20.0) - 10.0;
			double y = 256.0;  // Fixed Y level

			ItemStack stack = new ItemStack(SKYGLEAM);
			ItemEntity drop = new ItemEntity(serverWorld, x, y, z, stack);
			drop.setVelocity(0, -5.0, 0);
			serverWorld.spawnEntity(drop);
		});
	}
}
