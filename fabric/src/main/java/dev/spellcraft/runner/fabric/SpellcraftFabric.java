package dev.spellcraft.prototype.fabric;

import dev.spellcraft.prototype.minecraft.MinecraftMagic;
import net.fabricmc.api.ModInitializer;

public final class SpellcraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MinecraftMagic.initialize("fabric");
    }
}
