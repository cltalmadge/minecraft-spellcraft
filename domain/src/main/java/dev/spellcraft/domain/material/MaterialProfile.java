package dev.spellcraft.domain.material;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;

public record MaterialProfile(MaterialId material, List<FormParticipation> forms) {
    public MaterialProfile {
        Objects.requireNonNull(material);
        forms = forms.stream().sorted(Comparator.comparing(f -> f.form().value())).toList();
        if (forms.stream().map(FormParticipation::form).distinct().count() != forms.size()) throw new IllegalArgumentException("Duplicate form");
    }
    public double participation(FormId form) {
        Objects.requireNonNull(form);
        return forms.stream().filter(f -> f.form().equals(form)).mapToDouble(FormParticipation::strength).findFirst().orElse(0);
    }
    public boolean active() { return forms.stream().anyMatch(f -> f.strength() > 0); }
}
