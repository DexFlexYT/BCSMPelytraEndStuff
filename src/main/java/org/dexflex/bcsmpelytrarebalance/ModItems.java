package org.dexflex.bcsmpelytrarebalance;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final Item SKYGLEAM = registerItem("skygleam",
            new Item(new FabricItemSettings()));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(BCSMPElytraRebalance.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BCSMPElytraRebalance.LOGGER.debug("Registering Mod Items for " + BCSMPElytraRebalance.MOD_ID);
    }
}
