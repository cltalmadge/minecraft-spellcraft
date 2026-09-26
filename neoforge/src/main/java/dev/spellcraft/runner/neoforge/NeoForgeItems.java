package dev.spellcraft.runner.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import zoltan.minecraft.MinecraftMagic;
import zoltan.spellcraft.items.SpellbookItem;

public final class NeoForgeItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MinecraftMagic.MOD_ID);

    public static final DeferredItem<SpellbookItem> SPELLBOOK =
            ITEMS.registerItem(
                    "spellbook",
                    SpellbookItem::new,
                    properties -> properties.stacksTo(1)
            );

    private NeoForgeItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}