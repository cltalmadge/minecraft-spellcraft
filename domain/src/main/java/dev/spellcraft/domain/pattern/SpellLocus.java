package dev.spellcraft.domain.pattern;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.material.MaterialId;
import java.util.Objects;

/** Discovery-time contributions, independent of later material correspondence changes.
 * Material identity records experimental provenance, not a runtime target restriction. */
public record SpellLocus(LocusId id, LocusRole role, MaterialId material, FormExpression expressedForms) {
    public SpellLocus {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
        Objects.requireNonNull(material);
        Objects.requireNonNull(expressedForms);
        if (role == LocusRole.ANCHOR && !expressedForms.terms().isEmpty())
            throw new IllegalArgumentException("A geometric anchor cannot contribute Forms");
    }
}
