package zoltan.spellcraft.runner.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import zoltan.spellcraft.client.gui.SpellbookScreen;


public final class SpellcraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player.getItemInHand(hand).is(FabricItems.SPELLBOOK)) {
                Minecraft.getInstance().setScreenAndShow(new SpellbookScreen());
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }
}