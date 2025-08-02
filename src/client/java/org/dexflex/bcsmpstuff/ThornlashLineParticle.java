package org.dexflex.bcsmpstuff;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;


public class ThornlashLineParticle extends SpriteBillboardParticle {
    protected ThornlashLineParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(spriteProvider);
        this.maxAge = (this.random.nextInt(2)-1)+this.random.nextInt(1); // lasts only 1 tick
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.scale = this.random.nextFloat()*.25f+.1f;
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
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double dx, double dy, double dz) {
            return new ThornlashLineParticle(world, x, y, z, dx, dy, dz, spriteProvider);
        }
    }
}
