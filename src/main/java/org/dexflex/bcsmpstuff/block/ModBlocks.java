package org.dexflex.bcsmpstuff.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.dexflex.bcsmpstuff.BCSMPStuff;

public class ModBlocks {


    public static final Block SKYLIGHT = registerBlock("skylight",
            new SkylightBlock(FabricBlockSettings.of(Material.METAL).strength(4f).requiresTool()), ItemGroup.DECORATIONS);



    private static Block registerBlock(String name, Block block, ItemGroup tab) {
        registerBlockItem(name, block, tab);
        return Registry.register(Registry.BLOCK, new Identifier(BCSMPStuff.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup tab) {
        Item item = Registry.register(Registry.ITEM, new Identifier(BCSMPStuff.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
        return item;
    }

    public static void registerModBlocks() {
        BCSMPStuff.LOGGER.debug("Registering ModBlocks for " + BCSMPStuff.MOD_ID);
    }
}
