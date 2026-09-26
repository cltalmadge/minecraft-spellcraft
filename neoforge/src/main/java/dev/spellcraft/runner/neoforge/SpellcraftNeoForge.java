package dev.spellcraft.runner.neoforge;

import zoltan.minecraft.MinecraftMagic;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import zoltan.spellcraft.persistence.SpellComponents;

@Mod(MinecraftMagic.MOD_ID)
public final class SpellcraftNeoForge {
    public SpellcraftNeoForge(IEventBus modBus) {
        var components = DeferredRegister.create(
                Registries.DATA_COMPONENT_TYPE, MinecraftMagic.MOD_ID);
        components.register("recorded_spell", () -> SpellComponents.RECORDED_SPELL);
        components.register(modBus);
        NeoForgeItems.register(modBus);
        MinecraftMagic.initialize("neoforge");
    }
}
