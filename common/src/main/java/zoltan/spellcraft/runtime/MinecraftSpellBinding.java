package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.LocusRole;
import dev.spellcraft.domain.pattern.MagicalOperation;
import dev.spellcraft.domain.pattern.SpellLocus;
import dev.spellcraft.domain.program.SpellProgram;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** SOURCE/FOCUS bind to the caster; RECIPIENT binds to the ray target. Anchors remain geometric. */
public record MinecraftSpellBinding(SpellLocus originLocus, Optional<SpellLocus> recipientLocus,
                                    Vec3 origin, Vec3 aim, List<LivingEntity> targets) {
    public MinecraftSpellBinding { targets = List.copyOf(targets); }

    public static MinecraftSpellBinding resolve(SpellProgram program, MinecraftSpellContext context) {
        var caster = context.caster();
        Vec3 origin = caster.getEyePosition(), aim = caster.getLookAngle();
        if (program.operation() == MagicalOperation.TRANSFER && program.geometry() instanceof Geometry.Line) {
            var source = program.structure().single(LocusRole.SOURCE);
            var recipient = program.structure().single(LocusRole.RECIPIENT);
            Vec3 end = context.level().clip(new ClipContext(origin, origin.add(aim.scale(16)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getLocation();
            var targets = context.level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(1),
                    e -> e != caster && e.isAlive() && !e.isSpectator())
                .stream().filter(e -> e.getBoundingBox().inflate(0.15).clip(origin, end).isPresent())
                .sorted(Comparator.comparingDouble((LivingEntity e) -> e.getBoundingBox().inflate(0.15).clip(origin, end).orElseThrow().distanceToSqr(origin))
                    .thenComparingInt(LivingEntity::getId)).limit(1).toList();
            return new MinecraftSpellBinding(source, Optional.of(recipient), origin, aim, targets);
        }
        if (program.operation() == MagicalOperation.CONCENTRATE) {
            var focus = program.structure().single(LocusRole.FOCUS);
            if (program.geometry() instanceof Geometry.Point)
                return new MinecraftSpellBinding(focus, Optional.of(focus), origin, aim, List.of(caster));
            if (program.geometry() instanceof Geometry.Radial) {
                var targets = context.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(4),
                    e -> e != caster && e.isAlive() && !e.isSpectator() && e.distanceToSqr(caster) <= 16 && caster.hasLineOfSight(e));
                return new MinecraftSpellBinding(focus, Optional.empty(), origin, aim, targets);
            }
        }
        throw new IllegalArgumentException("No world binding for this operation and geometry");
    }
}
