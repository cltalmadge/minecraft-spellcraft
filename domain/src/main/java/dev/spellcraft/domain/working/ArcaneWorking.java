package dev.spellcraft.domain.working;

import java.util.Comparator;
import java.util.List;

public record ArcaneWorking(List<WorkingNode> nodes, List<WorkingStroke> strokes, List<WorkingBoundary> boundaries) {
    public ArcaneWorking {
        nodes = nodes.stream().sorted(Comparator.comparing(WorkingNode::id)).toList();
        strokes = strokes.stream().sorted(Comparator.comparing(WorkingStroke::from).thenComparing(WorkingStroke::to)).toList();
        boundaries = List.copyOf(boundaries);
    }
}
