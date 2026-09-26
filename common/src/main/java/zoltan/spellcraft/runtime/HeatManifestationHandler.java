package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.Forms;
import net.minecraft.core.particles.ParticleTypes;

public final class HeatManifestationHandler implements FormManifestationHandler {
    public FormId form() { return Forms.HEAT; }
    public void manifest(MinecraftSpellContext context, FormApplication application) {
        application.target().igniteForSeconds((float)(4 * application.intensity()));
        var position = application.target().position();
        context.level().sendParticles(ParticleTypes.FLAME, position.x, position.y + 1, position.z, 8, 0.2, 0.3, 0.2, 0.02);
    }
}
