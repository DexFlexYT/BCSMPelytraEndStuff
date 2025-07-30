package org.dexflex.bcsmpstuff;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.registry.Registry;


public record SphereParticleEffect(double r, double g, double b, float size, int lifetime) implements ParticleEffect {
    public static final ParticleEffect.Factory<SphereParticleEffect> FACTORY = new Factory();


    @Override
    public ParticleType<?> getType() {
        return BCSMPStuff.SPHERE_PARTICLE_TYPE;
    }


    @Override
    public void write(PacketByteBuf buf) {

        buf.writeDouble(r);
        buf.writeDouble(g);
        buf.writeDouble(b);
        buf.writeDouble(size);
        buf.writeInt(lifetime);
    }

    @Override
    public String asString() {
        return String.format("%s %.2f %.2f %.2f %d", Registry.PARTICLE_TYPE.getId(getType()), r, g, b, size, lifetime);
    }

    private static class Factory implements ParticleEffect.Factory<SphereParticleEffect> {
        @Override
        public SphereParticleEffect read(ParticleType<SphereParticleEffect> type, StringReader reader) {
            try {
                float r = Float.parseFloat(readNextNumber(reader));
                float g = Float.parseFloat(readNextNumber(reader));
                float b = Float.parseFloat(readNextNumber(reader));
                float size = Float.parseFloat(readNextNumber(reader));
                int lifetime = Integer.parseInt(readNextNumber(reader));
                return new SphereParticleEffect(r, g, b, size, lifetime);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse SphereParticleEffect", e);
            }
        }


        private String readNextNumber(StringReader reader) throws CommandSyntaxException {
            reader.skipWhitespace();
            StringBuilder sb = new StringBuilder();
            while (reader.canRead() && (Character.isDigit(reader.peek()) || reader.peek() == '.' || reader.peek() == '-')) {
                sb.append(reader.read());
            }
            return sb.toString();
        }

        @Override
        public SphereParticleEffect read(ParticleType<SphereParticleEffect> type, PacketByteBuf buf) {
            return new SphereParticleEffect(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt());
        }
    }
}
