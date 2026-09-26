package zoltan.spellcraft.runner.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;

import zoltan.minecraft.MinecraftMagic;

public final class SpellcraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MinecraftMagic.initialize("fabric");

        FabricItems.initialize();

        MinecraftMagic.LOGGER.info(
                "Spellbook registry key = {}",
                BuiltInRegistries.ITEM.getKey(FabricItems.SPELLBOOK)
        );
    }
}