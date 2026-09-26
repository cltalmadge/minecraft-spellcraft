package dev.spellcraft.domain.material;

import java.util.Objects;

public record MaterialId(String value) {
    public MaterialId {
        Objects.requireNonNull(value);
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid namespaced id: " + value);
    }
}
