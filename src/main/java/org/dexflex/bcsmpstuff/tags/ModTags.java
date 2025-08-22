package org.dexflex.bcsmpstuff.tags;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.block.Block;
import org.dexflex.bcsmpstuff.BCSMPStuff;

public class ModTags {
    public static final TagKey<Block> THORNLASHABLE = TagKey.of(Registry.BLOCK_KEY, new Identifier(BCSMPStuff.MOD_ID, "thornlashable"));
}
