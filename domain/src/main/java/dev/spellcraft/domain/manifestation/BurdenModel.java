package dev.spellcraft.domain.manifestation;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.program.SpellProgram;

/** Provisional semantic axes, deliberately independent of a mana economy. */
public final class BurdenModel {
    public SpellBurden calculate(SpellProgram program) {
        double intensity = program.forms().stream().mapToDouble(FormParticipation::strength).sum();
        double complexity = program.forms().size() + switch (program.operation()) {
            case CONCENTRATE -> 1; case TRANSFER -> 2; case MEDIATE -> 3; case STABILIZE -> 4;
        };
        if (program.geometry() instanceof Geometry.Radial || program.geometry() instanceof Geometry.Intersection) complexity += 1;
        return new SpellBurden(intensity, complexity, program.geometry() instanceof Geometry.Enclosure ? 4 : 0);
    }
}
