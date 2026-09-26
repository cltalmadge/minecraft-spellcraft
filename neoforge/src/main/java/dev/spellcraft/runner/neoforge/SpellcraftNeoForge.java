package dev.spellcraft.runner.neoforge;

import zoltan.minecraft.MinecraftMagic;
import net.neoforged.fml.common.Mod;

@Mod(MinecraftMagic.MOD_ID)
public final class SpellcraftNeoForge {
    public SpellcraftNeoForge() {
        MinecraftMagic.initialize("neoforge");
    }
}
