package dev.spellcraft.domain.form;

import java.util.Comparator;
import java.util.List;

public record FormExpression(List<FormParticipation> terms) {
    public FormExpression {
        terms = terms.stream()
            .sorted(Comparator.comparing(term -> term.form().value()))
            .toList();
        if (terms.isEmpty() || terms.stream().anyMatch(t -> t.strength() == 0)) throw new IllegalArgumentException("Empty form expression");
        if (terms.stream().map(FormParticipation::form).distinct().count() != terms.size()) throw new IllegalArgumentException("Duplicate form");
    }
}
