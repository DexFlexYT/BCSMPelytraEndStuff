package org.dexflex.bcsmpstuff.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class InkDropletParticle extends SpriteBillboardParticle {
    protected InkDropletParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.setSprite(spriteProvider);
        this.scale = 0.175f;
        this.maxAge = 40 + world.random.nextInt(10);
        this.gravityStrength = 0.5f;
        this.collidesWithWorld = true;
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        super.tick();

        // Fade out after 20 ticks
        if (this.age > 20) {
            float fadeProgress = (float)(this.age - 20) / (this.maxAge - 20);
            this.alpha = 1.0f - fadeProgress;
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new InkDropletParticle(world, x, y, z, spriteProvider);
        }
    }
}
