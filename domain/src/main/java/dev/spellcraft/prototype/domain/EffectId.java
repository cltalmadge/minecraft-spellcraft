package dev.spellcraft.prototype.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record EffectId(String value) {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public EffectId {
        Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid effect id: " + value);
        }
    }

    public static EffectId of(String value) {
        return new EffectId(value);
    }
}
