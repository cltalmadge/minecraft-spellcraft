package dev.spellcraft.domain.manifestation;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.program.SpellProgram;

/** Provisional semantic axes, deliberately independent of a mana economy. */
public final class BurdenModel {
    public SpellBurden calculate(SpellProgram program) {
        var active = program.structure().loci().stream().filter(l -> !l.expressedForms().terms().isEmpty()).toList();
        // Intensity averages all contributing loci, including recipient/mediator/stabilizer contributions.
        // This provisional structural burden is not a measure of manifested force alone.
        double intensity = active.stream().mapToDouble(l -> l.expressedForms().terms().stream()
            .mapToDouble(FormParticipation::strength).sum()).average().orElse(0);
        // Complexity counts distinct expressed Forms across all roles plus operation/topology.
        double complexity = program.participatingForms().stream().map(FormParticipation::form).distinct().count() + switch (program.operation()) {
            case CONCENTRATE -> 1; case TRANSFER -> 2; case MEDIATE -> 3; case STABILIZE -> 4;
        };
        if (program.geometry() instanceof Geometry.Radial || program.geometry() instanceof Geometry.Intersection) complexity += 1;
        // Persistence reflects enclosure, even for unstable or currently unexecutable patterns.
        return new SpellBurden(intensity, complexity, program.geometry() instanceof Geometry.Enclosure ? 4 : 0);
    }
}
