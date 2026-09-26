package dev.spellcraft.domain.working;

import dev.spellcraft.domain.pattern.*;
import java.math.BigInteger;
import java.util.*;

import static dev.spellcraft.domain.working.WorkingDiagnostic.Kind.*;

/** Recognizes a small physical grammar, then preserves its participants as semantic data. */
public final class WorkingAnalyzer {
    private record RecognizedStructure(NumericalPrinciple principle, Geometry geometry, Map<NodeId, LocusRole> roles) {}

    public AnalysisResult analyze(ArcaneWorking working) {
        Objects.requireNonNull(working);
        var invalid = validate(working);
        if (invalid.isPresent()) return new AnalysisResult(Optional.empty(), List.of(invalid.get()));
        var recognized = recognize(working);
        if (recognized.isEmpty()) return new AnalysisResult(Optional.empty(), List.of(new WorkingDiagnostic(AMBIGUOUS_STRUCTURE)));
        var motif = recognized.get();
        var loci = working.nodes().stream().map(n -> new SpellLocus(new LocusId(n.id().value()), motif.roles().get(n.id()),
            n.material().material(), n.expressedForms())).toList();
        var relations = working.strokes().stream()
            .map(e -> new LocusRelation(new LocusId(e.from().value()), new LocusId(e.to().value()))).toList();
        boolean enclosed = !working.boundaries().isEmpty();
        Geometry geometry = enclosed ? new Geometry.Enclosure(motif.geometry()) : motif.geometry();
        var pattern = new SpellPattern(new SpellStructure(loci, relations), motif.principle(), geometry);
        var diagnostics = new ArrayList<WorkingDiagnostic>();
        if (!enclosed && pattern.principle() != NumericalPrinciple.MONAD) diagnostics.add(new WorkingDiagnostic(UNCONTAINED_INFLUENCE));
        if ((enclosed || pattern.principle() == NumericalPrinciple.TETRAD) && !pattern.stabilized()) {
            diagnostics.add(new WorkingDiagnostic(UNSTABLE_STRUCTURE));
        } else {
            diagnostics.add(new WorkingDiagnostic(COHERENT));
        }
        return new AnalysisResult(Optional.of(pattern), diagnostics);
    }

    private Optional<WorkingDiagnostic> validate(ArcaneWorking working) {
        if (working.nodes().isEmpty()) return issue(EMPTY_WORKING);
        var ids = new HashSet<NodeId>();
        var positions = new HashSet<GridPoint>();
        for (var node : working.nodes()) {
            if (!ids.add(node.id()) || !positions.add(node.position())) return issue(AMBIGUOUS_STRUCTURE, node.id());
        }
        if (new HashSet<>(working.strokes()).size() != working.strokes().size()) return issue(AMBIGUOUS_STRUCTURE);
        for (var edge : working.strokes()) {
            if (!ids.contains(edge.from())) return issue(INCOMPLETE_RELATION, edge.from());
            if (!ids.contains(edge.to())) return issue(INCOMPLETE_RELATION, edge.to());
            if (edge.from().equals(edge.to())) return issue(INCOMPLETE_RELATION, edge.from());
        }
        if (working.boundaries().size() > 1) return issue(AMBIGUOUS_STRUCTURE);
        for (var boundary : working.boundaries()) {
            if (!ids.containsAll(boundary.enclosed())) return issue(INCOMPLETE_RELATION);
            if (!new HashSet<>(boundary.enclosed()).equals(ids)) return issue(AMBIGUOUS_STRUCTURE);
        }
        if (working.nodes().stream().noneMatch(n -> !n.expressedForms().terms().isEmpty())) return issue(NO_ACTIVE_FORM);
        var reached = new HashSet<NodeId>();
        reached.add(working.nodes().getFirst().id());
        boolean changed;
        do {
            changed = false;
            for (var edge : working.strokes()) if (reached.contains(edge.from()) || reached.contains(edge.to())) {
                changed |= reached.add(edge.from());
                changed |= reached.add(edge.to());
            }
        } while (changed);
        return reached.equals(ids) ? Optional.empty() : issue(INCOMPLETE_RELATION);
    }

    private Optional<RecognizedStructure> recognize(ArcaneWorking working) {
        var nodes = working.nodes();
        var edges = working.strokes();
        if (nodes.size() == 1) {
            return Optional.of(new RecognizedStructure(NumericalPrinciple.MONAD, new Geometry.Point(),
                Map.of(nodes.getFirst().id(), LocusRole.FOCUS)));
        }
        if (nodes.size() == 2 && edges.size() == 1) {
            var edge = edges.getFirst();
            return Optional.of(new RecognizedStructure(NumericalPrinciple.DYAD, new Geometry.Line(),
                Map.of(edge.from(), LocusRole.SOURCE, edge.to(), LocusRole.RECIPIENT)));
        }
        if (nodes.size() == 3 && edges.size() == 2) return recognizeChain(working);
        if (nodes.size() == 5 && edges.size() == 4) return recognizeSpokes(working);
        return Optional.empty();
    }

    private Optional<RecognizedStructure> recognizeChain(ArcaneWorking working) {
        var edges = working.strokes();
        var start = edges.stream().filter(e -> edges.stream().noneMatch(other -> other.to().equals(e.from()))).findFirst();
        if (start.isEmpty()) return Optional.empty();
        var first = start.get();
        var end = edges.stream().filter(e -> e.from().equals(first.to())).findFirst();
        if (end.isEmpty()) return Optional.empty();
        var byId = new HashMap<NodeId, GridPoint>();
        working.nodes().forEach(n -> byId.put(n.id(), n.position()));
        if (!straightForward(byId.get(first.from()), byId.get(first.to()), byId.get(end.get().to()))) return Optional.empty();
        return Optional.of(new RecognizedStructure(NumericalPrinciple.TRIAD, new Geometry.Line(),
            Map.of(first.from(), LocusRole.SOURCE, first.to(), LocusRole.MEDIATOR, end.get().to(), LocusRole.RECIPIENT)));
    }

    private Optional<RecognizedStructure> recognizeSpokes(ArcaneWorking working) {
        var nodes = working.nodes();
        var edges = working.strokes();
        var center = nodes.stream().filter(n -> edges.stream().allMatch(e -> e.from().equals(n.id()))).findFirst();
        boolean outward = center.isPresent();
        if (center.isEmpty()) center = nodes.stream().filter(n -> edges.stream().allMatch(e -> e.to().equals(n.id()))).findFirst();
        if (center.isEmpty() || !symmetricArms(nodes, center.get())) return Optional.empty();
        var middle = center.get();
        var arms = nodes.stream().filter(n -> !n.equals(middle)).toList();
        var roles = new HashMap<NodeId, LocusRole>();
        if (!middle.expressedForms().terms().isEmpty() && arms.stream().noneMatch(n -> !n.expressedForms().terms().isEmpty())) {
            roles.put(middle.id(), LocusRole.FOCUS);
            arms.forEach(n -> roles.put(n.id(), LocusRole.ANCHOR));
            return Optional.of(new RecognizedStructure(NumericalPrinciple.MONAD, new Geometry.Radial(outward), Map.copyOf(roles)));
        }
        if (middle.expressedForms().terms().isEmpty()) {
            roles.put(middle.id(), LocusRole.ANCHOR);
            arms.forEach(n -> roles.put(n.id(), LocusRole.STABILIZER));
            return Optional.of(new RecognizedStructure(NumericalPrinciple.TETRAD, new Geometry.Intersection(), Map.copyOf(roles)));
        }
        return Optional.empty();
    }

    private boolean straightForward(GridPoint a, GridPoint b, GridPoint c) {
        var abx = BigInteger.valueOf((long)b.x() - a.x());
        var aby = BigInteger.valueOf((long)b.y() - a.y());
        var bcx = BigInteger.valueOf((long)c.x() - b.x());
        var bcy = BigInteger.valueOf((long)c.y() - b.y());
        return abx.multiply(bcy).equals(aby.multiply(bcx)) && abx.multiply(bcx).add(aby.multiply(bcy)).signum() > 0;
    }

    private boolean symmetricArms(List<WorkingNode> nodes, WorkingNode center) {
        long cx = center.position().x(), cy = center.position().y();
        var arms = nodes.stream().filter(n -> !n.equals(center)).toList();
        long distance = Math.abs((long)arms.getFirst().position().x() - cx) + Math.abs((long)arms.getFirst().position().y() - cy);
        return arms.stream().allMatch(n -> {
            long dx = (long)n.position().x() - cx, dy = (long)n.position().y() - cy;
            return (dx == 0 || dy == 0) && Math.abs(dx) + Math.abs(dy) == distance;
        });
    }

    private Optional<WorkingDiagnostic> issue(WorkingDiagnostic.Kind kind) {
        return Optional.of(new WorkingDiagnostic(kind));
    }

    private Optional<WorkingDiagnostic> issue(WorkingDiagnostic.Kind kind, NodeId node) {
        return Optional.of(new WorkingDiagnostic(kind, Optional.of(node), Optional.empty()));
    }
}
