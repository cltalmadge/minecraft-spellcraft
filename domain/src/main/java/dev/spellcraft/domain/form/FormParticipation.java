package dev.spellcraft.domain.form;

import java.util.Objects;

/** Strength is normalized material participation, from absence (0) to full affinity (1). */
public record FormParticipation(FormId form, double strength) {
    public FormParticipation {
        Objects.requireNonNull(form);
        if (!Double.isFinite(strength) || strength < 0 || strength > 1) throw new IllegalArgumentException("Invalid participation");
    }
}
