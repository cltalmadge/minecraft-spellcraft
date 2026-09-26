package dev.spellcraft.domain.form;

import java.util.Comparator;
import java.util.List;

/** Forms contributed by one working node or semantic locus. Empty means no contribution. */
public record FormExpression(List<FormParticipation> terms) {
    public FormExpression {
        terms = terms.stream()
            .sorted(Comparator.comparing(term -> term.form().value()))
            .toList();
        if (terms.stream().anyMatch(t -> t.strength() == 0)) throw new IllegalArgumentException("Inactive form term");
        if (terms.stream().map(FormParticipation::form).distinct().count() != terms.size()) throw new IllegalArgumentException("Duplicate form");
    }
}
