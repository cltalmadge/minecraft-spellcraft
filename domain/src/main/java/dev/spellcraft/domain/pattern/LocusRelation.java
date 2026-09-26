package dev.spellcraft.domain.pattern;

import java.util.Objects;

/** Direction of the pattern's operation between two declared participants. */
public record LocusRelation(LocusId from, LocusId to) {
    public LocusRelation {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) throw new IllegalArgumentException("Self relation");
    }
}
