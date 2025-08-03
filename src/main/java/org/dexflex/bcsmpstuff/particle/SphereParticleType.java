package org.dexflex.bcsmpstuff.particle;

import com.mojang.serialization.Codec;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.Vec3f;

public class SphereParticleType extends ParticleType<SphereParticleEffect> {
    public SphereParticleType() {
        super(true, SphereParticleEffect.FACTORY);
    }

    @Override
    public Codec<SphereParticleEffect> getCodec() {
        return Codec.unit(new SphereParticleEffect(new Vec3f(1.0f, 1.0f, 1.0f), 1.0f, 20));
    }
}
