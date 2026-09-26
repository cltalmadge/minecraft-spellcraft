package dev.spellcraft.domain.working;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.NumericalPrinciple;
import dev.spellcraft.domain.pattern.SpellPattern;

import static dev.spellcraft.domain.working.WorkingDiagnostic.Kind.*;

/** A deliberately small grammar, not number recognition by node count. */
public final class WorkingAnalyzer {
    public AnalysisResult analyze(ArcaneWorking working) {
        Objects.requireNonNull(working);
        var nodes = working.nodes();
        if (nodes.isEmpty()) return failure(EMPTY_WORKING);
        var byId = new HashMap<NodeId, WorkingNode>();
        var positions = new HashSet<GridPoint>();
        for (var node : nodes) {
            if (byId.put(node.id(), node) != null || !positions.add(node.position())) return failure(AMBIGUOUS_STRUCTURE, node.id());
        }
        var edges = working.strokes();
        if (new HashSet<>(edges).size() != edges.size()) return failure(AMBIGUOUS_STRUCTURE);
        for (var edge : edges) {
            if (!byId.containsKey(edge.from())) return failure(INCOMPLETE_RELATION, edge.from());
            if (!byId.containsKey(edge.to())) return failure(INCOMPLETE_RELATION, edge.to());
            if (edge.from().equals(edge.to())) return failure(INCOMPLETE_RELATION, edge.from());
        }
        if (working.boundaries().size() > 1) return failure(AMBIGUOUS_STRUCTURE);
        for (var boundary : working.boundaries()) {
            if (!byId.keySet().containsAll(boundary.enclosed())) return failure(INCOMPLETE_RELATION);
            if (!new HashSet<>(boundary.enclosed()).equals(byId.keySet())) return failure(AMBIGUOUS_STRUCTURE);
        }
        var active = nodes.stream().filter(n -> n.material().active()).toList();
        if (active.isEmpty()) return failure(NO_ACTIVE_FORM);
        var reached = new HashSet<NodeId>();
        reached.add(nodes.getFirst().id());
        boolean changed;
        do {
            changed = false;
            for (var e : edges) if (reached.contains(e.from()) || reached.contains(e.to())) {
                changed |= reached.add(e.from()); changed |= reached.add(e.to());
            }
        } while (changed);
        if (reached.size() != nodes.size()) return failure(INCOMPLETE_RELATION);

        NumericalPrinciple number;
        Geometry geometry;
        if (nodes.size() == 1) {
            number = NumericalPrinciple.MONAD; geometry = new Geometry.Point();
        } else if (nodes.size() == 2 && edges.size() == 1 && active.size() == 2) {
            number = NumericalPrinciple.DYAD; geometry = new Geometry.Line();
        } else if (nodes.size() == 3 && edges.size() == 2 && active.size() == 3 && directedChain(edges)) {
            var startEdge = edges.stream().filter(e -> edges.stream().noneMatch(other -> other.to().equals(e.from()))).findFirst().orElseThrow();
            var endEdge = edges.stream().filter(e -> e.from().equals(startEdge.to())).findFirst().orElseThrow();
            var a = byId.get(startEdge.from()).position();
            var b = byId.get(startEdge.to()).position();
            var c = byId.get(endEdge.to()).position();
            // Limit to a straight, forward mediated relationship in this grammar.
            var abx = java.math.BigInteger.valueOf((long)b.x() - a.x());
            var aby = java.math.BigInteger.valueOf((long)b.y() - a.y());
            var bcx = java.math.BigInteger.valueOf((long)c.x() - b.x());
            var bcy = java.math.BigInteger.valueOf((long)c.y() - b.y());
            if (!abx.multiply(bcy).equals(aby.multiply(bcx)) || abx.multiply(bcx).add(aby.multiply(bcy)).signum() <= 0)
                return failure(AMBIGUOUS_STRUCTURE);
            number = NumericalPrinciple.TRIAD; geometry = new Geometry.Line();
        } else {
            var center = nodes.stream().filter(n -> edges.size() == nodes.size() - 1 &&
                edges.stream().allMatch(e -> e.from().equals(n.id()))).findFirst();
            boolean outward = true;
            if (center.isEmpty()) {
                center = nodes.stream().filter(n -> edges.size() == nodes.size() - 1 &&
                    edges.stream().allMatch(e -> e.to().equals(n.id()))).findFirst();
                outward = false;
            }
            if (center.isEmpty() || nodes.size() != 5 || !symmetricArms(nodes, center.get())) return failure(AMBIGUOUS_STRUCTURE);
            if (active.size() == 1 && active.getFirst().equals(center.get())) {
                number = NumericalPrinciple.MONAD; geometry = new Geometry.Radial(outward);
            } else if (active.size() == 4 && !center.get().material().active() &&
                    active.stream().allMatch(n -> n.material().forms().equals(active.getFirst().material().forms()))) {
                number = NumericalPrinciple.TETRAD; geometry = new Geometry.Intersection();
            } else return failure(UNSTABLE_STRUCTURE);
        }
        boolean enclosed = !working.boundaries().isEmpty();
        if (enclosed && number != NumericalPrinciple.TETRAD) return failure(UNSTABLE_STRUCTURE);
        if (enclosed) geometry = new Geometry.Enclosure(geometry);
        var strengths = new TreeMap<String, Double>();
        for (var node : active) for (var form : node.material().forms()) {
            if (form.strength() > 0) strengths.merge(form.form().value(), form.strength() / active.size(), Double::sum);
        }
        var forms = new FormExpression(strengths.entrySet().stream()
            .map(e -> new FormParticipation(new FormId(e.getKey()), Math.min(1, e.getValue()))).toList());
        var diagnostics = new ArrayList<WorkingDiagnostic>();
        if (!enclosed && number != NumericalPrinciple.MONAD) diagnostics.add(new WorkingDiagnostic(UNCONTAINED_INFLUENCE));
        diagnostics.add(new WorkingDiagnostic(COHERENT));
        return new AnalysisResult(Optional.of(new SpellPattern(forms, number, geometry)), diagnostics);
    }
    private boolean directedChain(List<WorkingStroke> edges) {
        return edges.get(0).to().equals(edges.get(1).from()) || edges.get(1).to().equals(edges.get(0).from());
    }
    private boolean symmetricArms(List<WorkingNode> nodes, WorkingNode center) {
        long cx = center.position().x(), cy = center.position().y();
        var arms = nodes.stream().filter(n -> !n.equals(center)).toList();
        long distance = Math.abs((long) arms.getFirst().position().x() - cx) + Math.abs((long) arms.getFirst().position().y() - cy);
        return arms.stream().allMatch(n -> {
            long dx = (long)n.position().x() - cx, dy = (long)n.position().y() - cy;
            return (dx == 0 || dy == 0) && Math.abs(dx) + Math.abs(dy) == distance;
        });
    }
    private AnalysisResult failure(WorkingDiagnostic.Kind kind, NodeId node) {
        return new AnalysisResult(Optional.empty(), List.of(new WorkingDiagnostic(kind, Optional.of(node), Optional.empty())));
    }
    private AnalysisResult failure(WorkingDiagnostic.Kind kind) {
        return new AnalysisResult(Optional.empty(), List.of(new WorkingDiagnostic(kind)));
    }
}
