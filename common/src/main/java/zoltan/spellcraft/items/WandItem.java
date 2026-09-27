package zoltan.spellcraft.items;

import dev.spellcraft.domain.manifestation.ManifestationSource;
import dev.spellcraft.domain.manifestation.SpellBurden;
import dev.spellcraft.domain.vessel.VesselModel;
import dev.spellcraft.domain.vessel.VesselProfile;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import zoltan.spellcraft.runtime.*;

public final class WandItem extends Item {
    private final CastService casts = new CastService();
    private final VesselModel vessel = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of());
    public WandItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            if (player.isShiftKeyDown() && SpellPageBinding.bind(player.getItemInHand(hand),
                player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND))) {
                player.sendOverlayMessage(Component.translatable("workbench.spellcraft.bound"));
                return InteractionResult.SUCCESS;
            }
            // The wand holds and directs; the caster supplies capacity and incurs provisional fatigue.
            ManifestationSource spirit = burden -> player.getFoodData().getFoodLevel() > 0 && burden.intensity() <= 3 && burden.persistence() == 0;
            var result = casts.cast(new MinecraftSpellContext(server, player), player.getItemInHand(hand), vessel, spirit);
            player.sendOverlayMessage(Component.translatable("cast.spellcraft." + result.status().name().toLowerCase(java.util.Locale.ROOT)));
        }
        return InteractionResult.SUCCESS;
    }
}
