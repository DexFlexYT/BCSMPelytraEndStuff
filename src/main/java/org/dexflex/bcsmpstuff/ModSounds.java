package org.dexflex.bcsmpstuff;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModSounds {
    public static final SoundEvent MARKSMAN_SHOOT = registerSound("marksman.shoot");
    public static final SoundEvent COINFLIP = registerSound("marksman.coinflip");

    private static SoundEvent registerSound(String name) {
        Identifier id = new Identifier("bcsmp-stuff", name);
        SoundEvent soundEvent = new SoundEvent(id);
        return Registry.register(Registry.SOUND_EVENT, id, soundEvent);
    }

    public static void registerModSounds() {
    }
}
