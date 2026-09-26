package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.MagicalOperation;
import dev.spellcraft.domain.program.SpellProgram;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import static zoltan.spellcraft.runtime.ManifestationResult.Status.*;

/** Geometry selects recipients once; Forms independently determine their response. */
public final class MinecraftSpellRuntime {
    private final Map<FormId, FormManifestationHandler> handlers;
    public MinecraftSpellRuntime(List<FormManifestationHandler> handlers) {
        var map = new HashMap<FormId, FormManifestationHandler>();
        for (var handler : handlers) if (map.put(handler.form(), handler) != null) throw new IllegalArgumentException("Duplicate manifestation handler");
        this.handlers = Map.copyOf(map);
    }
    public ManifestationResult execute(SpellProgram program, MinecraftSpellContext context) {
        var caster = context.caster();
        if (!caster.isAlive() || caster.isSpectator() || caster.level() != context.level()) return new ManifestationResult(INVALID_CASTER, 0);
        // Mediation and fixed fields are representable, but need temporal world state before execution.
        if (program.operation() == MagicalOperation.MEDIATE || program.operation() == MagicalOperation.STABILIZE ||
                program.geometry() instanceof Geometry.Enclosure || program.geometry() instanceof Geometry.Intersection)
            return new ManifestationResult(UNSUPPORTED_OPERATION, 0);
        var forms = program.invokedForms();
        if (forms.isEmpty()) return new ManifestationResult(NO_SOURCE_FORM, 0);
        // Preflight every invoked Form before any world mutation. Structural Forms need no handler.
        if (forms.stream().anyMatch(f -> !handlers.containsKey(f.form()))) return new ManifestationResult(UNSUPPORTED_FORM, 0);
        var binding = MinecraftSpellBinding.resolve(program, context);
        var targets = binding.targets();
        int applications = 0;
        for (var target : targets) {
            Vec3 direction = binding.aim();
            if (program.geometry() instanceof Geometry.Radial radial) {
                direction = target.position().subtract(caster.position()).normalize();
                if (!radial.outward()) direction = direction.scale(-1);
            }
            // Concentration retains full strength at each recipient; transfer shares a finite influence.
            double share = program.operation() == MagicalOperation.TRANSFER ? 1.0 / Math.max(1, targets.size()) : 1;
            for (var form : forms) {
                handlers.get(form.form()).manifest(context, new FormApplication(target, direction, form.strength() * share));
                applications++;
            }
        }
        return new ManifestationResult(applications == 0 ? NO_TARGET : APPLIED, applications);
    }
}
