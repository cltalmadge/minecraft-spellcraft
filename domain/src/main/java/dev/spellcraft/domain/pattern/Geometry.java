package dev.spellcraft.domain.pattern;

import java.util.Objects;

public sealed interface Geometry {
    record Point() implements Geometry {}
    record Line() implements Geometry {}
    record Enclosure(Geometry interior) implements Geometry {
        public Enclosure { Objects.requireNonNull(interior); if (interior instanceof Enclosure) throw new IllegalArgumentException("Nested enclosure unsupported"); }
    }
    record Radial(boolean outward) implements Geometry {}
    record Intersection() implements Geometry {}
}
