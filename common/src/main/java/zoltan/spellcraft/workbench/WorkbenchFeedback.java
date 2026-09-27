package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.working.WorkingDiagnostic.Kind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;

/** Event-based sensory vocabulary, separate from the domain diagnostics. */
public enum WorkbenchFeedback {
    COHERENT, UNCONTAINED, UNSTABLE, AMBIGUOUS, INERT, INCOMPLETE, UNRESOLVED;
    public static WorkbenchFeedback classify(WorkbenchWorkingAdapter.Snapshot snapshot) {
        if (snapshot.unresolved()) return UNRESOLVED;
        var kinds = snapshot.analysis().diagnostics().stream().map(d -> d.kind()).toList();
        if (kinds.contains(Kind.NO_ACTIVE_FORM) || kinds.contains(Kind.EMPTY_WORKING)) return INERT;
        if (kinds.contains(Kind.INCOMPLETE_RELATION)) return INCOMPLETE;
        if (kinds.contains(Kind.AMBIGUOUS_STRUCTURE)) return AMBIGUOUS;
        if (kinds.contains(Kind.UNSTABLE_STRUCTURE)) return UNSTABLE;
        if (kinds.contains(Kind.UNCONTAINED_INFLUENCE)) return UNCONTAINED;
        return COHERENT;
    }
    public void emit(ServerLevel level, BlockPos pos, ArcaneWorkbenchState state) {
        float pitch = switch (this) { case COHERENT -> 1.4f; case UNCONTAINED -> 1.0f; case UNSTABLE -> .5f; case AMBIGUOUS -> .7f; case INCOMPLETE -> .85f; default -> .6f; };
        level.playSound(null, pos, this == INERT ? SoundEvents.STONE_BUTTON_CLICK_OFF : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, .65f, pitch);
        if (this == INERT) return;
        SimpleParticleType particle = switch (this) {
            case COHERENT -> ParticleTypes.END_ROD;
            case UNCONTAINED -> ParticleTypes.SMOKE;
            case UNSTABLE -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.ASH;
        };
        var facing = level.getBlockState(pos).getValue(ArcaneWorkbenchBlock.FACING);
        for (var n : state.loci()) {
            var world = ArcaneWorkbenchLayout.world(pos, facing, WorkbenchCoordinates.x(n.point()), 1.08, WorkbenchCoordinates.z(n.point()));
            level.sendParticles(particle, world.x, world.y, world.z, this == UNSTABLE ? 7 : 3, .025, .06, .025, .01);
        }
        if (this == COHERENT || this == UNCONTAINED) for (var r : state.relations()) {
            var a = state.locus(r.from()).orElseThrow().point(); var b = state.locus(r.to()).orElseThrow().point();
            for (int i = 0; i <= 8; i++) {
                double t = i / 8.0;
                var world = ArcaneWorkbenchLayout.world(pos, facing, WorkbenchCoordinates.x(a)*(1-t)+WorkbenchCoordinates.x(b)*t,
                    1.04, WorkbenchCoordinates.z(a)*(1-t)+WorkbenchCoordinates.z(b)*t);
                level.sendParticles(particle, world.x, world.y, world.z, 1, 0, .01, 0, .005);
            }
        }
    }
}
