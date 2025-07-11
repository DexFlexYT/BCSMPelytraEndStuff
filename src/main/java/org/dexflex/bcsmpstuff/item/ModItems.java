package org.dexflex.bcsmpstuff.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.dexflex.bcsmpstuff.BCSMPStuff;

public class ModItems {
    public static final Item SKYGLEAM = registerItem("skygleam",
            new Item(new FabricItemSettings()));

    public static final Item MARKSMAN_REVOLVER = registerItem("marksman_revolver",
            new MarksmanRevolverItem(new FabricItemSettings().maxCount(1)));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(BCSMPStuff.MOD_ID, name), item);
    }


    public static void registerModItems() {
        BCSMPStuff.LOGGER.debug("Registering Mod Items for " + BCSMPStuff.MOD_ID);
    }
}
