package org.dexflex.bcsmpstuff.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.dexflex.bcsmpstuff.BCSMPStuff;

public class ModItemGroup {
    public static final ItemGroup BCSMP_GROUP = FabricItemGroupBuilder.build(new Identifier(BCSMPStuff.MOD_ID, "bcsmp-stuff"), () -> new ItemStack(ModItems.SKYGLEAM));

    public static void registerItemGroups() {
        BCSMPStuff.LOGGER.info("Item groups loaded");
    }
}
