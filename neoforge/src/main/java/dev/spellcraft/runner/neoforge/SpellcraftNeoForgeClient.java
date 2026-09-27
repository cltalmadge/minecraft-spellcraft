package dev.spellcraft.runner.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import zoltan.spellcraft.client.workbench.ArcaneWorkbenchRenderer;

@EventBusSubscriber(modid = "spellcraft", value = Dist.CLIENT)
public final class SpellcraftNeoForgeClient {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NeoForgeWorkbench.TYPE.get(), ArcaneWorkbenchRenderer::new);
    }
}
