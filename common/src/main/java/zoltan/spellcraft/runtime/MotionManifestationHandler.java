package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.Forms;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public final class MotionManifestationHandler implements FormManifestationHandler {
    public FormId form() { return Forms.MOTION; }
    public void manifest(MinecraftSpellContext context, FormApplication application) {
        var target = application.target();
        target.push(application.direction().scale(0.8 * application.intensity()));
        if (target instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(target));
        var position = target.position();
        context.level().sendParticles(ParticleTypes.CLOUD, position.x, position.y + 1, position.z, 8, 0.2, 0.2, 0.2, 0.02);
    }
}
