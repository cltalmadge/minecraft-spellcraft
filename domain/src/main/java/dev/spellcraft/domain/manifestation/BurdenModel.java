package dev.spellcraft.domain.manifestation;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.program.SpellProgram;

/** Provisional semantic axes, deliberately independent of a mana economy. */
public final class BurdenModel {
    public SpellBurden calculate(SpellProgram program) {
        var active = program.structure().loci().stream().filter(l -> !l.forms().terms().isEmpty()).toList();
        // A derived average preserves the provisional balance without erasing locus ownership.
        double intensity = active.stream().mapToDouble(l -> l.forms().terms().stream()
            .mapToDouble(FormParticipation::strength).sum()).sum() / active.size();
        double complexity = program.participatingForms().stream().map(FormParticipation::form).distinct().count() + switch (program.operation()) {
            case CONCENTRATE -> 1; case TRANSFER -> 2; case MEDIATE -> 3; case STABILIZE -> 4;
        };
        if (program.geometry() instanceof Geometry.Radial || program.geometry() instanceof Geometry.Intersection) complexity += 1;
        return new SpellBurden(intensity, complexity, program.geometry() instanceof Geometry.Enclosure ? 4 : 0);
    }
}
