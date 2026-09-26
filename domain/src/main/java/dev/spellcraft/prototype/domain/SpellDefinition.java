package dev.spellcraft.prototype.domain;

import java.util.List;
import java.util.Objects;

public record SpellDefinition(String name, List<SpellEffectSpec> effects) {
    public SpellDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(effects, "effects");
        effects = List.copyOf(effects);
    }
}
