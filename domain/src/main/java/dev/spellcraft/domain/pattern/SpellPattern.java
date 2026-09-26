package dev.spellcraft.domain.pattern;

import java.util.Objects;

import dev.spellcraft.domain.form.FormExpression;

public record SpellPattern(FormExpression forms, NumericalPrinciple principle, Geometry geometry) {
    public SpellPattern { Objects.requireNonNull(forms); Objects.requireNonNull(principle); Objects.requireNonNull(geometry); }
    public MagicalOperation operation() { return principle.operation(); }
}
