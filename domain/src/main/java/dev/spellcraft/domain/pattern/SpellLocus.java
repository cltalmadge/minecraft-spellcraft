package dev.spellcraft.domain.pattern;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.material.MaterialId;
import java.util.Objects;

/** Material nature belongs to a participant; its role determines how it contributes. */
public record SpellLocus(LocusId id, LocusRole role, MaterialId material, FormExpression forms) {
    public SpellLocus {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
        Objects.requireNonNull(material);
        Objects.requireNonNull(forms);
        if (role == LocusRole.ANCHOR && !forms.terms().isEmpty())
            throw new IllegalArgumentException("A geometric anchor cannot contribute Forms");
    }
}
