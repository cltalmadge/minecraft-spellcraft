package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.MagicalOperation;
import dev.spellcraft.domain.program.SpellProgram;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
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
        if (program.forms().stream().anyMatch(f -> !handlers.containsKey(f.form()))) return new ManifestationResult(UNSUPPORTED_FORM, 0);
        // Mediation and fixed fields are representable, but need temporal world state before execution.
        if (program.operation() == MagicalOperation.MEDIATE || program.operation() == MagicalOperation.STABILIZE ||
                program.geometry() instanceof Geometry.Enclosure || program.geometry() instanceof Geometry.Intersection)
            return new ManifestationResult(UNSUPPORTED_OPERATION, 0);
        Vec3 origin = caster.getEyePosition(), aim = caster.getLookAngle();
        List<LivingEntity> targets;
        if (program.geometry() instanceof Geometry.Point) {
            targets = List.of(caster);
        } else if (program.geometry() instanceof Geometry.Line) {
            Vec3 end = context.level().clip(new ClipContext(origin, origin.add(aim.scale(16)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getLocation();
            targets = context.level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(1),
                    e -> e != caster && e.isAlive() && !e.isSpectator())
                .stream().filter(e -> e.getBoundingBox().inflate(0.15).clip(origin, end).isPresent())
                .sorted(Comparator.comparingDouble((LivingEntity e) -> e.getBoundingBox().inflate(0.15).clip(origin, end).orElseThrow().distanceToSqr(origin))
                    .thenComparingInt(LivingEntity::getId)).limit(1).toList();
        } else {
            targets = context.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(4),
                e -> e != caster && e.isAlive() && !e.isSpectator() && e.distanceToSqr(caster) <= 16 && caster.hasLineOfSight(e));
        }
        int applications = 0;
        for (var target : targets) {
            Vec3 direction = aim;
            if (program.geometry() instanceof Geometry.Radial radial) {
                direction = target.position().subtract(caster.position()).normalize();
                if (!radial.outward()) direction = direction.scale(-1);
            }
            // Concentration retains full strength at each recipient; transfer shares a finite influence.
            double share = program.operation() == MagicalOperation.TRANSFER ? 1.0 / Math.max(1, targets.size()) : 1;
            for (var form : program.forms()) {
                handlers.get(form.form()).manifest(context, new FormApplication(target, direction, form.strength() * share));
                applications++;
            }
        }
        return new ManifestationResult(applications == 0 ? NO_TARGET : APPLIED, applications);
    }
}
