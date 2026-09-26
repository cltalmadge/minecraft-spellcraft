package dev.spellcraft.runner.neoforge;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import zoltan.spellcraft.runtime.ManifestationGameTests;

@EventBusSubscriber(modid = "spellcraft")
public final class SpellcraftGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath("spellcraft", "laboratory"));
        var data = new TestData<>(environment, Identifier.fromNamespaceAndPath("spellcraft", "laboratory"), 40, 0, true);
        for (var entry : ManifestationGameTests.tests(NeoForgeItems.WAND.get(), NeoForgeItems.SPELL_PAGE.get()).entrySet()) {
            event.registerTest(Identifier.fromNamespaceAndPath("spellcraft", entry.getKey()),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override public void run(GameTestHelper helper) { entry.getValue().accept(helper); }
                });
        }
    }
}
