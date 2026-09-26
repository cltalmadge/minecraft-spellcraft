package zoltan.spellcraft.runner.fabric;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

import zoltan.spellcraft.client.SpellbookEntrySnapshot;
import zoltan.spellcraft.client.SpellbookSnapshot;
import zoltan.spellcraft.client.gui.SpellbookScreen;


public final class SpellcraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.getItemInHand(hand).is(FabricItems.SPELLBOOK)) {
                // Temporary placeholder source. Networking is out of scope, so the
                // read-model snapshot is built here from static placeholder data.
                // Replace this construction site with a network-populated snapshot later.
                SpellbookSnapshot snapshot = placeholderSnapshot();
                Minecraft.getInstance().setScreenAndShow(new SpellbookScreen(snapshot));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    private static SpellbookSnapshot placeholderSnapshot() {
        return new SpellbookSnapshot(
                List.of(
                        new SpellbookEntrySnapshot(
                                "Flare",
                                List.of("Damage Health", "Magnitude: 6", "Delivery: Ray", "Cost: 12")),
                        new SpellbookEntrySnapshot(
                                "Heal Minor Wounds",
                                List.of("Restore Health", "Magnitude: 5", "Delivery: Self", "Cost: 10")),
                        new SpellbookEntrySnapshot(
                                "Fleet Step",
                                List.of(
                                        "Fortify Speed",
                                        "Magnitude: 10",
                                        "Duration: 20s",
                                        "Delivery: Self",
                                        "Cost: 14")))
        );
    }
}
