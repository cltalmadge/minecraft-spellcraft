package dev.spellcraft.prototype.domain;

import java.util.Objects;

public record SpellEffectSpec(
    EffectId effect,
    Delivery delivery,
    int magnitude,
    int durationTicks,
    double radius
) {
    public SpellEffectSpec {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(delivery, "delivery");
    }
}
