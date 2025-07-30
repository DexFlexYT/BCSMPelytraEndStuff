package org.dexflex.bcsmpstuff;

import com.mojang.serialization.Codec;
import net.minecraft.particle.ParticleType;

public class SphereParticleType extends ParticleType<SphereParticleEffect> {
    public SphereParticleType() {
        super(true, SphereParticleEffect.FACTORY);
    }
    @Override
    public Codec<SphereParticleEffect> getCodec() {
        return Codec.unit(new SphereParticleEffect(1.0, 1.0, 1.0, 1.0f, 20));
    }
}