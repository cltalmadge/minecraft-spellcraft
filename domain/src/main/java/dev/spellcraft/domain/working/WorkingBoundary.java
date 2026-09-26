package dev.spellcraft.domain.working;

import java.util.HashSet;
import java.util.List;

/** Explicit membership avoids implying a physical polygon precision the prototype does not have. */
public record WorkingBoundary(List<NodeId> enclosed) {
    public WorkingBoundary {
        enclosed = enclosed.stream().sorted().toList();
        if (enclosed.isEmpty() || new HashSet<>(enclosed).size() != enclosed.size()) throw new IllegalArgumentException("Invalid boundary membership");
    }
}
