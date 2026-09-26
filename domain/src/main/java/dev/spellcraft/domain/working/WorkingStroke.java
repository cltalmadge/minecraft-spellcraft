package dev.spellcraft.domain.working;

import java.util.Objects;

/** An intentional directed relationship. Inactive nodes may serve as geometric anchors. */
public record WorkingStroke(NodeId from, NodeId to) {
    public WorkingStroke { Objects.requireNonNull(from); Objects.requireNonNull(to); }
}
