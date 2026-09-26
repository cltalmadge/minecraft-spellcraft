package dev.spellcraft.domain.form;

import java.util.Objects;

public record FormId(String value) {
    public FormId {
        Objects.requireNonNull(value);
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid namespaced id: " + value);
    }
}
