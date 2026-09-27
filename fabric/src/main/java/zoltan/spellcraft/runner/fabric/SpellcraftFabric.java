package zoltan.spellcraft.runner.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import zoltan.spellcraft.persistence.SpellComponents;
import net.minecraft.core.registries.BuiltInRegistries;

import zoltan.minecraft.MinecraftMagic;

public final class SpellcraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MinecraftMagic.initialize("fabric");

        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(MinecraftMagic.MOD_ID, "recorded_spell"),
                SpellComponents.RECORDED_SPELL);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(MinecraftMagic.MOD_ID, "divider_selection"),
                zoltan.spellcraft.workbench.DividerSelection.TYPE);
        FabricItems.initialize();
        FabricWorkbench.initialize();

        MinecraftMagic.LOGGER.info(
                "Spellbook registry key = {}",
                BuiltInRegistries.ITEM.getKey(FabricItems.SPELLBOOK)
        );
    }
}