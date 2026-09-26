package dev.spellcraft.domain.pattern;

import java.util.Objects;

public record SpellPattern(SpellStructure structure, NumericalPrinciple principle, Geometry geometry) {
    public SpellPattern {
        Objects.requireNonNull(structure);
        Objects.requireNonNull(principle);
        Objects.requireNonNull(geometry);
        structure.validate(principle.operation(), geometry);
    }
    public MagicalOperation operation() { return principle.operation(); }

    public boolean stabilized() {
        return principle == NumericalPrinciple.TETRAD && structure.hasEquivalentStabilizers();
    }
}
