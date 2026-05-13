package com.chinaex123.create_small_generator.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class CSGItemTags {

//    TagKey<Item> WOOLS = neoforgeTag("wools");

    static TagKey<Item> neoforgeTag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", name));
    }
}
