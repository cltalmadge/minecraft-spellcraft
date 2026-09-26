package dev.spellcraft.prototype.neoforge;

import dev.spellcraft.prototype.minecraft.MinecraftMagic;
import net.neoforged.fml.common.Mod;

@Mod(MinecraftMagic.MOD_ID)
public final class SpellcraftNeoForge {
    public SpellcraftNeoForge() {
        MinecraftMagic.initialize("neoforge");
    }
}
