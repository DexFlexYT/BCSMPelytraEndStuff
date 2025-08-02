package org.dexflex.bcsmpstuff;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import org.dexflex.bcsmpstuff.block.ModBlocks;
import org.dexflex.bcsmpstuff.item.ModItems;
import org.dexflex.bcsmpstuff.item.ThornlashItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

public class BCSMPStuff implements ModInitializer {

	public static final String MOD_ID = "bcsmp-stuff";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityType<ProtectionSphereEntity> PROTECTION_SPHERE =
			Registry.register(Registry.ENTITY_TYPE, new Identifier(MOD_ID, "protection_sphere"),
					FabricEntityTypeBuilder.<ProtectionSphereEntity>create(SpawnGroup.MISC, ProtectionSphereEntity::new)
							.dimensions(EntityDimensions.fixed(0.1f, 0.1f))
							.trackRangeChunks(10)
							.trackedUpdateRate(20)
							.build());

	public static final ParticleType<SphereParticleEffect> SPHERE_PARTICLE_TYPE =
			new SphereParticleType();
	public static final ParticleType<DefaultParticleType> THORNLASH_LINE_PARTICLE_TYPE =
			FabricParticleTypes.simple();


	@Override
	public void onInitialize() {
		Registry.register(Registry.PARTICLE_TYPE, new Identifier(MOD_ID, "sphere_particle"), SPHERE_PARTICLE_TYPE);
		Registry.register(Registry.PARTICLE_TYPE, new Identifier(MOD_ID, "thornlash_line"), THORNLASH_LINE_PARTICLE_TYPE);

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModSounds.registerModSounds();

		// Existing rain item drop code omitted for brevity, keep as is
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!(world instanceof ServerWorld)) return;
			ServerWorld serverWorld = (ServerWorld) world;
			if (!serverWorld.isRaining() && !serverWorld.isThundering()) return;

			Random random = serverWorld.getRandom();
			if (random.nextInt(5000) != 0) return;

			List<ServerPlayerEntity> players = serverWorld.getPlayers(player -> true);
			if (players.isEmpty()) return;

			ServerPlayerEntity player = players.get(random.nextInt(players.size()));

			double x = player.getX() + (random.nextDouble() * 60.0) - 30.0;
			double z = player.getZ() + (random.nextDouble() * 60.0) - 30.0;
			double y = 321.0;

			ItemStack stack = new ItemStack(ModItems.SKYGLEAM);
			ItemEntity drop = new ItemEntity(serverWorld, x, y, z, stack);
			//drop.setVelocity(0, -5.0, 0);
			serverWorld.spawnEntity(drop);
		});

		// Register Thornlash pulling logic per player every server tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				ThornlashItem.tickPulling(player);
			}
		});

		LOGGER.info("BCSMPStuff initialized");
	}
}
