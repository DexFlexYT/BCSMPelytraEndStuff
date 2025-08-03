package org.dexflex.bcsmpstuff.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.dexflex.bcsmpstuff.BCSMPStuff;

public class ModParticles {

    public static final ParticleType<SphereParticleEffect> SPHERE_PARTICLE_TYPE =
            new SphereParticleType();

    public static final DefaultParticleType THORNLASH_LINE_PARTICLE_TYPE =
            FabricParticleTypes.simple();

    public static final DefaultParticleType INKFLOWER_SEEDS =
            FabricParticleTypes.simple();

    public static void registerModParticles() {
        Registry.register(Registry.PARTICLE_TYPE, new Identifier(BCSMPStuff.MOD_ID, "sphere_particle"), SPHERE_PARTICLE_TYPE);
        Registry.register(Registry.PARTICLE_TYPE, new Identifier(BCSMPStuff.MOD_ID, "thornlash_line"), THORNLASH_LINE_PARTICLE_TYPE);
        Registry.register(Registry.PARTICLE_TYPE, new Identifier(BCSMPStuff.MOD_ID, "inkflower_seeds"), INKFLOWER_SEEDS);
    }
}
