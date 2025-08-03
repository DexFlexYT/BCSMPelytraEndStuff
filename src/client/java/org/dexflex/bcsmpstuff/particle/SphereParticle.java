package org.dexflex.bcsmpstuff.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3f;

public class SphereParticle extends SpriteBillboardParticle {
    private final float initialScale;

    protected SphereParticle(ClientWorld world, double x, double y, double z, double r, double g, double b, float size, int lifetime, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.setSprite(spriteProvider);
        this.maxAge = lifetime;
        this.initialScale = size;
        this.scale = size;
        this.red = (float) r;
        this.green = (float) g;
        this.blue = (float) b;

    }

    @Override
    public void tick() {
        super.tick();
        float lifeRatio = (float)(maxAge - age) / maxAge;
        this.scale = initialScale * lifeRatio;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }


    public static class Factory implements ParticleFactory<SphereParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SphereParticleEffect effect, ClientWorld world, double x, double y, double z, double dx, double dy, double dz) {
            Vec3f c = effect.color();
            return new SphereParticle(world, x, y, z, c.getX(), c.getY(), c.getZ(), effect.size(), effect.lifetime(), spriteProvider);

        }
    }
}
