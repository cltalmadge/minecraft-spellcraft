package dev.spellcraft.domain.integration;

import org.junit.jupiter.api.Test;
import java.util.*;

import dev.spellcraft.domain.form.*;
import dev.spellcraft.domain.manifestation.*;
import dev.spellcraft.domain.material.*;
import dev.spellcraft.domain.pattern.*;
import dev.spellcraft.domain.program.*;
import dev.spellcraft.domain.vessel.*;
import dev.spellcraft.domain.working.*;

import static org.junit.jupiter.api.Assertions.*;
import static dev.spellcraft.domain.working.WorkingDiagnostic.Kind.*;

class SpellcraftPipelineTest {
    private final WorkingAnalyzer analyzer = new WorkingAnalyzer();
    private final SpellCompiler compiler = new SpellCompiler();
    private MaterialProfile material(FormId form) {
        return new MaterialProfile(new MaterialId("test:material"), form == null ? List.of() : List.of(new FormParticipation(form, 1)));
    }
    private WorkingNode node(String id, int x, int y, FormId form) { return new WorkingNode(new NodeId(id), new GridPoint(x, y), material(form)); }
    private WorkingStroke edge(String a, String b) { return new WorkingStroke(new NodeId(a), new NodeId(b)); }
    private ArcaneWorking line(FormId form) {
        return new ArcaneWorking(List.of(node("a", 0, 0, form), node("b", 2, 0, form)), List.of(edge("a", "b")), List.of());
    }
    private ArcaneWorking star(FormId form, boolean tetrad, boolean enclosed, boolean outward) {
        var nodes = List.of(node("x", 0, 0, tetrad ? null : form), node("a", 0, 2, tetrad ? form : null),
            node("b", 2, 0, tetrad ? form : null), node("c", 0, -2, tetrad ? form : null), node("d", -2, 0, tetrad ? form : null));
        var edges = List.of("a", "b", "c", "d").stream().map(id -> outward ? edge("x", id) : edge(id, "x")).toList();
        return new ArcaneWorking(nodes, edges, enclosed ? List.of(new WorkingBoundary(nodes.stream().map(WorkingNode::id).toList())) : List.of());
    }
    private SpellPattern pattern(ArcaneWorking working) { return analyzer.analyze(working).pattern().orElseThrow(); }
    private SpellProgram program(ArcaneWorking working) { return compiler.compile(pattern(working)); }
    private void fails(ArcaneWorking working, WorkingDiagnostic.Kind kind) {
        var result = analyzer.analyze(working);
        assertTrue(result.pattern().isEmpty()); assertEquals(List.of(kind), result.diagnostics().stream().map(WorkingDiagnostic::kind).toList());
    }
    @Test void identifiersAndParticipationValidate() {
        assertEquals("spellcraft:heat", Forms.HEAT.value());
        for (String id : List.of("Heat", "spellcraft:", "x:UPPER", "x:a b", ":a")) assertThrows(IllegalArgumentException.class, () -> new FormId(id));
        assertThrows(NullPointerException.class, () -> new FormId(null));
        for (double value : new double[]{-1, 1.01, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> new FormParticipation(Forms.HEAT, value));
        assertThrows(NullPointerException.class, () -> new FormParticipation(null, 1));
    }
    @Test void profilesAreImmutableSortedAndRejectDuplicates() {
        var input = new ArrayList<>(List.of(new FormParticipation(Forms.MOTION, 0.3), new FormParticipation(Forms.HEAT, 1)));
        var profile = new MaterialProfile(new MaterialId("test:blaze"), input); input.clear();
        assertEquals(1, profile.participation(Forms.HEAT)); assertEquals(0.3, profile.participation(Forms.MOTION));
        assertEquals(0, profile.participation(new FormId("test:unknown")));
        assertEquals(Forms.HEAT, profile.forms().getFirst().form());
        assertThrows(UnsupportedOperationException.class, () -> profile.forms().clear());
        assertThrows(IllegalArgumentException.class, () -> new MaterialProfile(profile.material(), List.of(profile.forms().getFirst(), profile.forms().getFirst())));
    }
    @Test void singleLocusIsMonadPoint() {
        var p = pattern(new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT)), List.of(), List.of()));
        assertEquals(NumericalPrinciple.MONAD, p.principle()); assertEquals(MagicalOperation.CONCENTRATE, p.operation());
        assertInstanceOf(Geometry.Point.class, p.geometry());
    }
    @Test void relatedPairIsDyadLine() {
        var p = pattern(line(Forms.HEAT)); assertEquals(NumericalPrinciple.DYAD, p.principle());
        assertEquals(MagicalOperation.TRANSFER, p.operation()); assertInstanceOf(Geometry.Line.class, p.geometry());
    }
    @Test void directedSequenceIsMediatedTriad() {
        var p = pattern(new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT), node("b", 1, 0, Forms.HEAT), node("c", 2, 0, Forms.HEAT)), List.of(edge("a", "b"), edge("b", "c")), List.of()));
        assertEquals(NumericalPrinciple.TRIAD, p.principle()); assertEquals(MagicalOperation.MEDIATE, p.operation());
        assertEquals(MagicalOperation.MEDIATE, compiler.compile(p).operation());
    }
    @Test void bentSequenceIsNotAssumedLinear() {
        fails(new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT), node("b", 1, 0, Forms.HEAT), node("c", 1, 1, Forms.HEAT)), List.of(edge("a", "b"), edge("b", "c")), List.of()), AMBIGUOUS_STRUCTURE);
    }
    @Test void symmetryAndEquivalentArmsEstablishTetrad() {
        var p = pattern(star(Forms.HEAT, true, true, false));
        assertEquals(NumericalPrinciple.TETRAD, p.principle()); assertEquals(MagicalOperation.STABILIZE, p.operation());
        assertInstanceOf(Geometry.Enclosure.class, p.geometry());
        assertInstanceOf(Geometry.Intersection.class, ((Geometry.Enclosure)p.geometry()).interior());
        assertInstanceOf(Geometry.Intersection.class, pattern(star(Forms.HEAT, true, false, true)).geometry());
    }
    @Test void unrelatedFourAreNotTetrad() {
        fails(new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT), node("b", 1, 2, Forms.HEAT), node("c", 3, 4, Forms.HEAT), node("d", 7, 2, Forms.HEAT)), List.of(), List.of()), INCOMPLETE_RELATION);
    }
    @Test void asymmetricArmsAreNotStable() {
        var w = star(Forms.HEAT, true, true, true); var nodes = new ArrayList<>(w.nodes());
        nodes.set(0, node("a", 0, 3, Forms.HEAT));
        fails(new ArcaneWorking(nodes, w.strokes(), w.boundaries()), AMBIGUOUS_STRUCTURE);
    }
    @Test void differingArmsAreNotEquivalent() {
        var w = star(Forms.HEAT, true, true, true); var nodes = new ArrayList<>(w.nodes()); nodes.set(0, node("a", 0, 2, Forms.MOTION));
        fails(new ArcaneWorking(nodes, w.strokes(), w.boundaries()), UNSTABLE_STRUCTURE);
    }
    @Test void radialDirectionComesFromStrokes() {
        assertEquals(new Geometry.Radial(true), pattern(star(Forms.HEAT, false, false, true)).geometry());
        assertEquals(new Geometry.Radial(false), pattern(star(Forms.HEAT, false, false, false)).geometry());
    }
    @Test void emptyAndInactiveHaveSpecificDiagnostics() {
        fails(new ArcaneWorking(List.of(), List.of(), List.of()), EMPTY_WORKING);
        fails(new ArcaneWorking(List.of(node("a", 0, 0, null)), List.of(), List.of()), NO_ACTIVE_FORM);
    }
    @Test void danglingSelfDuplicateAndCoincidentRelationsFail() {
        var w = line(Forms.HEAT);
        fails(new ArcaneWorking(w.nodes(), List.of(edge("a", "missing")), List.of()), INCOMPLETE_RELATION);
        fails(new ArcaneWorking(w.nodes(), List.of(edge("a", "a")), List.of()), INCOMPLETE_RELATION);
        fails(new ArcaneWorking(w.nodes(), List.of(edge("a", "b"), edge("a", "b")), List.of()), AMBIGUOUS_STRUCTURE);
        fails(new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT), node("b", 0, 0, Forms.HEAT)), w.strokes(), List.of()), AMBIGUOUS_STRUCTURE);
        fails(new ArcaneWorking(List.of(w.nodes().getFirst(), w.nodes().getFirst()), List.of(), List.of()), AMBIGUOUS_STRUCTURE);
    }
    @Test void diagnosticIdentifiesMissingLocus() {
        var w = line(Forms.HEAT);
        var result = analyzer.analyze(new ArcaneWorking(w.nodes(), List.of(edge("a", "missing")), List.of()));
        assertEquals(Optional.of(new NodeId("missing")), result.diagnostics().getFirst().node());
    }
    @Test void boundaryMustBeCompleteAndStabilized() {
        var w = line(Forms.HEAT);
        fails(new ArcaneWorking(w.nodes(), w.strokes(), List.of(new WorkingBoundary(List.of(new NodeId("a"))))), AMBIGUOUS_STRUCTURE);
        fails(new ArcaneWorking(w.nodes(), w.strokes(), List.of(new WorkingBoundary(List.of(new NodeId("missing"))))), INCOMPLETE_RELATION);
        fails(new ArcaneWorking(w.nodes(), w.strokes(), List.of(new WorkingBoundary(List.of(new NodeId("a"), new NodeId("b"))))), UNSTABLE_STRUCTURE);
        assertTrue(analyzer.analyze(w).diagnostics().contains(new WorkingDiagnostic(UNCONTAINED_INFLUENCE)));
    }
    @Test void fourProofCasesDifferThroughSharedSemantics() {
        var heat = program(line(Forms.HEAT)); var motion = program(line(Forms.MOTION));
        var radial = program(star(Forms.HEAT, false, false, true)); var contained = program(star(Forms.HEAT, true, true, false));
        assertEquals(heat.instructions().subList(1, 4), motion.instructions().subList(1, 4));
        assertNotEquals(heat.forms(), motion.forms()); assertEquals(heat.forms(), radial.forms());
        assertEquals(MagicalOperation.CONCENTRATE, radial.operation()); assertInstanceOf(Geometry.Radial.class, radial.geometry());
        assertEquals(MagicalOperation.STABILIZE, contained.operation()); assertInstanceOf(SpellInstruction.Sustain.class, contained.instructions().getLast());
        assertInstanceOf(SpellInstruction.Release.class, heat.instructions().getLast());
    }
    @Test void mixturesPreserveBothForms() {
        var w = new ArcaneWorking(List.of(node("a", 0, 0, Forms.HEAT), node("b", 1, 0, Forms.MOTION)), List.of(edge("a", "b")), List.of());
        assertEquals(List.of(new FormParticipation(Forms.HEAT, 0.5), new FormParticipation(Forms.MOTION, 0.5)), program(w).forms());
    }
    @Test void analysisAndCompilationAreDeterministicAcrossInputOrder() {
        var w = star(Forms.HEAT, true, true, true);
        var reversedNodes = new ArrayList<>(w.nodes()); Collections.reverse(reversedNodes);
        var reversedEdges = new ArrayList<>(w.strokes()); Collections.reverse(reversedEdges);
        var other = new ArcaneWorking(reversedNodes, reversedEdges, w.boundaries());
        assertEquals(analyzer.analyze(w), analyzer.analyze(other));
        assertEquals(program(w), program(other)); assertEquals(program(w).hashCode(), program(other).hashCode());
    }
    @Test void programsValidateVersionGrammarAndImmutability() {
        var p = program(line(Forms.HEAT)); assertEquals(1, p.schemaVersion());
        assertInstanceOf(SpellInstruction.Invoke.class, p.instructions().get(0));
        assertInstanceOf(SpellInstruction.Operate.class, p.instructions().get(1));
        assertInstanceOf(SpellInstruction.Shape.class, p.instructions().get(2));
        var mutable = new ArrayList<>(p.instructions()); var copy = new SpellProgram(1, mutable); mutable.clear(); assertEquals(p, copy);
        assertThrows(UnsupportedOperationException.class, () -> p.instructions().clear());
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(2, p.instructions()));
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(1, List.of(new SpellInstruction.Release())));
        var wrong = new ArrayList<>(p.instructions()); wrong.set(3, new SpellInstruction.Sustain());
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(1, wrong));
        assertThrows(IllegalArgumentException.class, () -> new RecordedSpell(2, "x", p));
        assertThrows(IllegalArgumentException.class, () -> new RecordedSpell(1, "", p));
    }
    @Test void burdenTracksStructureAndPersistenceRatherThanSpellNames() {
        var model = new BurdenModel(); var released = model.calculate(program(line(Forms.HEAT)));
        var fixed = model.calculate(program(star(Forms.HEAT, true, true, false)));
        assertEquals(released, model.calculate(program(line(Forms.MOTION))));
        assertEquals(released.intensity(), fixed.intensity()); assertTrue(fixed.complexity() > released.complexity());
        assertEquals(0, released.persistence()); assertTrue(fixed.persistence() > 0);
    }
    @Test void sameProgramHasDifferentContainmentAcrossVessels() {
        var p = program(line(Forms.HEAT)); var burden = new BurdenModel().calculate(p);
        var wand = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of(Forms.HEAT));
        var incompatible = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of(Forms.MOTION));
        assertTrue(wand.evaluate(p, burden).viable()); assertEquals(List.of(ContainmentReport.Issue.FORM), incompatible.evaluate(p, burden).issues());
        var fixed = program(star(Forms.HEAT, true, true, false)); var fixedBurden = new BurdenModel().calculate(fixed);
        assertFalse(wand.evaluate(fixed, fixedBurden).viable());
        assertTrue(new VesselProfile(new SpellBurden(3, 10, 10), false, Set.of()).evaluate(fixed, fixedBurden).viable());
    }
}
