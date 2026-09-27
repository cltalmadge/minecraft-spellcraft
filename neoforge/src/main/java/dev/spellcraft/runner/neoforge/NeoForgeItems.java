package dev.spellcraft.runner.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import zoltan.minecraft.MinecraftMagic;
import zoltan.spellcraft.items.SpellbookItem;
import zoltan.spellcraft.items.WandItem;
import net.minecraft.world.item.Item;

public final class NeoForgeItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MinecraftMagic.MOD_ID);

    public static final DeferredItem<SpellbookItem> SPELLBOOK =
            ITEMS.registerItem(
                    "spellbook",
                    SpellbookItem::new,
                    properties -> properties.stacksTo(1)
            );

    public static final DeferredItem<WandItem> WAND = ITEMS.registerItem(
            "wand", WandItem::new,
            properties -> properties.stacksTo(1));
    public static final DeferredItem<Item> SPELL_PAGE = ITEMS.registerItem(
            "spell_page", Item::new,
            properties -> properties.stacksTo(1));

    private NeoForgeItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
