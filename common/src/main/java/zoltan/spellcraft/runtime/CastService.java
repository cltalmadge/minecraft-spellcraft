package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.manifestation.BurdenModel;
import dev.spellcraft.domain.manifestation.ManifestationSource;
import dev.spellcraft.domain.vessel.VesselModel;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import zoltan.spellcraft.persistence.SpellComponents;
import static zoltan.spellcraft.runtime.ManifestationResult.Status.*;

public final class CastService {
    private final MinecraftSpellRuntime runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler(), new MotionManifestationHandler()));
    private final BurdenModel burdenModel = new BurdenModel();
    public ManifestationResult cast(MinecraftSpellContext context, ItemStack held, VesselModel vessel, ManifestationSource source) {
        if ((held != context.caster().getMainHandItem() && held != context.caster().getOffhandItem()) ||
                !context.caster().isAlive() || context.caster().isSpectator()) return new ManifestationResult(INVALID_CASTER, 0);
        if (context.caster().getCooldowns().isOnCooldown(held)) return new ManifestationResult(COOLDOWN, 0);
        var recording = held.get(SpellComponents.RECORDED_SPELL);
        if (recording == null) return new ManifestationResult(UNSUPPORTED_OPERATION, 0);
        var burden = burdenModel.calculate(recording.program());
        if (!vessel.evaluate(recording.program(), burden).viable()) return new ManifestationResult(INCOMPATIBLE_VESSEL, 0);
        if (!source.supports(burden)) return new ManifestationResult(INSUFFICIENT_SOURCE, 0);
        var result = runtime.execute(recording.program(), context);
        if (result.status() == APPLIED) {
            context.caster().getCooldowns().addCooldown(held, 20);
            context.caster().causeFoodExhaustion((float)(burden.intensity() * 0.2 + burden.complexity() * 0.05));
        }
        return result;
    }
}
