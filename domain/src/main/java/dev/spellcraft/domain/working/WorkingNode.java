package dev.spellcraft.domain.working;

import java.util.Objects;

import dev.spellcraft.domain.material.MaterialProfile;

public record WorkingNode(NodeId id, GridPoint position, MaterialProfile material) {
    public WorkingNode { Objects.requireNonNull(id); Objects.requireNonNull(position); Objects.requireNonNull(material); }
}
