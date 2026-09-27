package zoltan.spellcraft.runner.fabric;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import zoltan.minecraft.MinecraftMagic;
import zoltan.spellcraft.items.SpellbookItem;
import zoltan.spellcraft.items.WandItem;

public final class FabricItems {
    public static final ResourceKey<Item> SPELLBOOK_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(MinecraftMagic.MOD_ID, "spellbook")
            );

    public static final SpellbookItem SPELLBOOK =
            register(
                    SPELLBOOK_KEY,
                    SpellbookItem::new,
                    new Item.Properties().stacksTo(1)
            );

    public static final Item WAND = register(key("wand"), WandItem::new,
            new Item.Properties().stacksTo(1));
    public static final Item SPELL_PAGE = register(key("spell_page"), Item::new,
            new Item.Properties().stacksTo(1));

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MinecraftMagic.MOD_ID, name));
    }
    private FabricItems() {}

    private static <T extends Item> T register(
            ResourceKey<Item> key,
            Function<Item.Properties, T> factory,
            Item.Properties properties
    ) {
        T item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void initialize() {}
}