package dev.spellcraft.domain.vessel;

import java.util.List;

public record ContainmentReport(List<Issue> issues) {
    public enum Issue { INTENSITY, COMPLEXITY, PERSISTENCE, GEOMETRY, FORM }
    public ContainmentReport { issues = List.copyOf(issues); }
    public boolean viable() { return issues.isEmpty(); }
}
