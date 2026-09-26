package dev.spellcraft.domain.working;

import java.util.Objects;

public record NodeId(String value) implements Comparable<NodeId> {
    public NodeId { if (Objects.requireNonNull(value).isBlank()) throw new IllegalArgumentException("Empty node id"); }
    @Override public int compareTo(NodeId other) { return value.compareTo(other.value); }
}
