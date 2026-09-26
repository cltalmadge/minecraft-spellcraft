package dev.spellcraft.domain.pattern;

import java.util.Objects;

/** Logical identity preserved from a working; independent of tabletop coordinates. */
public record LocusId(String value) implements Comparable<LocusId> {
    public LocusId {
        if (Objects.requireNonNull(value).isBlank()) throw new IllegalArgumentException("Empty locus id");
    }

    @Override public int compareTo(LocusId other) { return value.compareTo(other.value); }
}
