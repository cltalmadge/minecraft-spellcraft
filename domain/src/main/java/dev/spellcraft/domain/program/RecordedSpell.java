package dev.spellcraft.domain.program;

import java.util.Objects;

public record RecordedSpell(int schemaVersion, String name, SpellProgram program) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public RecordedSpell {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported recording schema");
        if (Objects.requireNonNull(name).isBlank() || name.length() > 128) throw new IllegalArgumentException("Invalid recording name");
        Objects.requireNonNull(program);
    }
}
