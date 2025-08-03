package org.dexflex.bcsmpstuff.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

public record SphereParticleEffect(Vec3f color, float size, int lifetime) implements ParticleEffect {

    public static final Factory FACTORY = new Factory();


    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPHERE_PARTICLE_TYPE;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(color.getX());
        buf.writeFloat(color.getY());
        buf.writeFloat(color.getZ());
        buf.writeFloat(size);
        buf.writeInt(lifetime);
    }

    @Override
    public String asString() {
        return String.format("%s{color:[%.2f,%.2f,%.2f],size:%.2f,lifetime:%d}",
                Registry.PARTICLE_TYPE.getId(getType()),
                color.getX(), color.getY(), color.getZ(),
                size, lifetime
        );
    }

    private static class Factory implements ParticleEffect.Factory<SphereParticleEffect> {
        @Override
        public SphereParticleEffect read(ParticleType<SphereParticleEffect> type, StringReader reader) {
            try {
                expect(reader, '{');
                expectKey(reader, "color");
                Vec3f color = readVec3f(reader);
                expect(reader, ',');
                expectKey(reader, "size");
                float size = Float.parseFloat(readNextNumber(reader));
                expect(reader, ',');
                expectKey(reader, "lifetime");
                int lifetime = Integer.parseInt(readNextNumber(reader));
                expect(reader, '}');
                return new SphereParticleEffect(color, size, lifetime);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse SphereParticleEffect", e);
            }
        }

        @Override
        public SphereParticleEffect read(ParticleType<SphereParticleEffect> type, PacketByteBuf buf) {
            Vec3f color = new Vec3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
            float size = buf.readFloat();
            int lifetime = buf.readInt();
            return new SphereParticleEffect(color, size, lifetime);
        }

        private static void expect(StringReader reader, char expected) throws CommandSyntaxException {
            reader.skipWhitespace();
            if (!reader.canRead() || reader.read() != expected) {
                throw new CommandSyntaxException(null, () -> "Expected '" + expected + "'");
            }
        }

        private static void expectKey(StringReader reader, String key) throws CommandSyntaxException {
            reader.skipWhitespace();
            for (char c : key.toCharArray()) {
                if (!reader.canRead() || reader.read() != c) {
                    throw new CommandSyntaxException(null, () -> "Expected key '" + key + "'");
                }
            }
            reader.skipWhitespace();
            if (reader.canRead() && reader.peek() == ':') reader.read();
        }

        private static Vec3f readVec3f(StringReader reader) throws CommandSyntaxException {
            expect(reader, '[');
            float x = Float.parseFloat(readNextNumber(reader));
            expect(reader, ',');
            float y = Float.parseFloat(readNextNumber(reader));
            expect(reader, ',');
            float z = Float.parseFloat(readNextNumber(reader));
            expect(reader, ']');
            return new Vec3f(x, y, z);
        }

        private static String readNextNumber(StringReader reader) throws CommandSyntaxException {
            reader.skipWhitespace();
            StringBuilder sb = new StringBuilder();
            while (reader.canRead() && (Character.isDigit(reader.peek()) || reader.peek() == '.' || reader.peek() == '-')) {
                sb.append(reader.read());
            }
            return sb.toString();
        }
    }
}
