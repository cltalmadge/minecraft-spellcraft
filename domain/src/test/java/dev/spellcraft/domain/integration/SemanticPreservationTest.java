package dev.spellcraft.domain.integration;

import dev.spellcraft.domain.form.*;
import dev.spellcraft.domain.material.*;
import dev.spellcraft.domain.manifestation.BurdenModel;
import dev.spellcraft.domain.pattern.*;
import dev.spellcraft.domain.program.*;
import dev.spellcraft.domain.working.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static dev.spellcraft.domain.working.WorkingDiagnostic.Kind.*;

class SemanticPreservationTest {
    private final WorkingAnalyzer analyzer = new WorkingAnalyzer();
    private final SpellCompiler compiler = new SpellCompiler();

    private WorkingNode node(String id, int x, int y, String material, FormId... forms) {
        return WorkingNode.expressingAll(new NodeId(id), new GridPoint(x, y), new MaterialProfile(new MaterialId("test:" + material),
            Arrays.stream(forms).map(f -> new FormParticipation(f, 1)).toList()));
    }

    private WorkingStroke edge(String from, String to) { return new WorkingStroke(new NodeId(from), new NodeId(to)); }

    private ArcaneWorking dyad(boolean reverse) {
        var nodes = List.of(node("a", 0, 0, "blaze", Forms.HEAT), node("b", 1, 0, "iron", Forms.MOTION));
        return new ArcaneWorking(nodes, List.of(reverse ? edge("b", "a") : edge("a", "b")), List.of());
    }

    private ArcaneWorking triad(String mediatorMaterial, FormId... mediatorForms) {
        return new ArcaneWorking(List.of(node("a", 0, 0, "blaze", Forms.HEAT), node("b", 1, 0, mediatorMaterial, mediatorForms),
            node("c", 2, 0, "iron")), List.of(edge("a", "b"), edge("b", "c")), List.of());
    }

    private ArcaneWorking tetrad(boolean incomplete) {
        var nodes = List.of(node("x", 0, 0, "center"), node("a", 0, 1, "blaze", Forms.HEAT),
            node("b", 1, 0, "blaze", Forms.HEAT), node("c", 0, -1, "blaze", Forms.HEAT),
            incomplete ? node("d", -1, 0, "empty") : node("d", -1, 0, "blaze", Forms.HEAT));
        return new ArcaneWorking(nodes, List.of(edge("a", "x"), edge("b", "x"), edge("c", "x"), edge("d", "x")),
            List.of(new WorkingBoundary(nodes.stream().map(WorkingNode::id).toList())));
    }

    private SpellPattern pattern(ArcaneWorking working) { return analyzer.analyze(working).pattern().orElseThrow(); }
    private SpellProgram program(ArcaneWorking working) { return compiler.compile(pattern(working)); }

    @Test void reversalChangesActualRoleAndFormOwnershipInPatternsAndPrograms() {
        var forward = pattern(dyad(false));
        var reverse = pattern(dyad(true));
        assertEquals(forward.geometry(), reverse.geometry());
        assertEquals(forward.principle(), reverse.principle());
        assertNotEquals(forward, reverse);
        assertEquals(new MaterialId("test:blaze"), forward.structure().single(LocusRole.SOURCE).material());
        assertEquals(new MaterialId("test:iron"), reverse.structure().single(LocusRole.SOURCE).material());
        assertEquals(List.of(new FormParticipation(Forms.HEAT, 1)), compiler.compile(forward).invokedForms());
        assertEquals(List.of(new FormParticipation(Forms.MOTION, 1)), compiler.compile(reverse).invokedForms());
        assertNotEquals(compiler.compile(forward), compiler.compile(reverse));
        assertEquals(new LocusId("b"), forward.structure().single(LocusRole.RECIPIENT).id());
        assertEquals(List.of(new LocusRelation(new LocusId("b"), new LocusId("a"))), reverse.structure().relations());
    }

    @Test void inactiveRecipientAndReversedInactiveSourceRemainDistinct() {
        var nodes = List.of(node("a", 0, 0, "blaze", Forms.HEAT), node("b", 1, 0, "iron"));
        var forward = program(new ArcaneWorking(nodes, List.of(edge("a", "b")), List.of()));
        var reverse = program(new ArcaneWorking(nodes, List.of(edge("b", "a")), List.of()));
        assertTrue(forward.structure().single(LocusRole.RECIPIENT).expressedForms().terms().isEmpty());
        assertFalse(forward.invokedForms().isEmpty());
        assertTrue(reverse.invokedForms().isEmpty());
        assertNotEquals(forward, reverse);
    }

    @Test void triadPreservesSourceMediatorRecipientMaterialAndForms() {
        var working = triad("copper", Forms.MOTION);
        var p = pattern(working);
        var program = compiler.compile(p);
        assertEquals(p.structure(), program.structure());
        assertEquals(NumericalPrinciple.TRIAD, p.principle());
        assertEquals(List.of(new FormParticipation(Forms.HEAT, 1)), program.invokedForms());
        assertEquals(List.of(new FormParticipation(Forms.HEAT, 1)), program.structure().single(LocusRole.SOURCE).expressedForms().terms());
        assertEquals(List.of(new FormParticipation(Forms.MOTION, 1)), program.structure().single(LocusRole.MEDIATOR).expressedForms().terms());
        assertEquals(new MaterialId("test:copper"), program.structure().single(LocusRole.MEDIATOR).material());
        assertTrue(program.structure().single(LocusRole.RECIPIENT).expressedForms().terms().isEmpty());
        assertEquals(List.of(new LocusRelation(new LocusId("a"), new LocusId("b")),
            new LocusRelation(new LocusId("b"), new LocusId("c"))), program.structure().relations());
        assertNotEquals(program, program(triad("copper", Forms.HEAT)));
        assertNotEquals(program(triad("copper")), program(triad("glass")), "Even inactive mediator material is meaningful");
    }

    @Test void tetradPreservesInactiveCenterAndEachStabilizer() {
        var p = pattern(tetrad(false));
        assertTrue(p.stabilized());
        assertTrue(compiler.compile(p).invokedForms().isEmpty());
        assertTrue(p.structure().single(LocusRole.ANCHOR).expressedForms().terms().isEmpty());
        assertEquals(new LocusId("x"), p.structure().single(LocusRole.ANCHOR).id());
        assertEquals(4, p.structure().withRole(LocusRole.STABILIZER).size());
        for (var locus : p.structure().withRole(LocusRole.STABILIZER))
            assertEquals(List.of(new FormParticipation(Forms.HEAT, 1)), locus.expressedForms().terms());
        assertEquals(p.structure(), compiler.compile(p).structure());
        assertEquals(List.of(new WorkingDiagnostic(COHERENT)), analyzer.analyze(tetrad(false)).diagnostics());
        assertInstanceOf(SpellInstruction.Sustain.class, compiler.compile(p).instructions().getLast());
    }

    @Test void missingStabilizerContributionIsInterpretableButCannotSustain() {
        var result = analyzer.analyze(tetrad(true));
        assertTrue(result.pattern().isPresent());
        assertFalse(result.pattern().orElseThrow().stabilized());
        assertEquals(List.of(new WorkingDiagnostic(UNSTABLE_STRUCTURE)), result.diagnostics());
        assertInstanceOf(SpellInstruction.Release.class, compiler.compile(result.pattern().orElseThrow()).instructions().getLast());
    }

    @Test void enclosedDyadHasMeaningWithoutStability() {
        var w = dyad(false);
        var enclosed = new ArcaneWorking(w.nodes(), w.strokes(), List.of(new WorkingBoundary(w.nodes().stream().map(WorkingNode::id).toList())));
        var result = analyzer.analyze(enclosed);
        var p = result.pattern().orElseThrow();
        assertInstanceOf(Geometry.Enclosure.class, p.geometry());
        assertEquals(pattern(w).structure(), p.structure());
        assertEquals(List.of(new WorkingDiagnostic(UNSTABLE_STRUCTURE)), result.diagnostics());
        assertFalse(p.stabilized());
        assertInstanceOf(SpellInstruction.Release.class, compiler.compile(p).instructions().getLast());
    }

    @Test void inputOrderNeverChangesRolesRelationsCompilationOrBurden() {
        for (var w : List.of(dyad(false), dyad(true), triad("copper", Forms.MOTION), tetrad(false), tetrad(true))) {
            var nodes = new ArrayList<>(w.nodes()); Collections.reverse(nodes);
            var strokes = new ArrayList<>(w.strokes()); Collections.reverse(strokes);
            var boundaries = w.boundaries().stream().map(b -> new WorkingBoundary(b.enclosed().reversed())).toList();
            var reordered = new ArcaneWorking(nodes, strokes, boundaries);
            assertEquals(analyzer.analyze(w), analyzer.analyze(reordered));
            assertEquals(program(w), program(reordered));
            assertEquals(program(w).hashCode(), program(reordered).hashCode());
            assertEquals(new BurdenModel().calculate(program(w)), new BurdenModel().calculate(program(reordered)));
        }
    }

    @Test void semanticStructureIsImmutableAndRejectsDuplicateOrUnknownReferences() {
        var structure = pattern(dyad(false)).structure();
        var loci = new ArrayList<>(structure.loci()); var edges = new ArrayList<>(structure.relations());
        var copy = new SpellStructure(loci, edges); loci.clear(); edges.clear();
        assertEquals(structure, copy);
        assertThrows(UnsupportedOperationException.class, () -> copy.loci().clear());
        assertThrows(UnsupportedOperationException.class, () -> copy.relations().clear());
        assertThrows(IllegalArgumentException.class, () -> new SpellStructure(List.of(structure.loci().getFirst(), structure.loci().getFirst()), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SpellStructure(structure.loci(), List.of(structure.relations().getFirst(), structure.relations().getFirst())));
        assertThrows(IllegalArgumentException.class, () -> new SpellStructure(structure.loci(), List.of(new LocusRelation(new LocusId("a"), new LocusId("missing")))));
    }

    @Test void programRejectsIncoherentRolesDirectionsAndMissingMediator() {
        var p = program(dyad(false));
        var reversedEdges = new SpellStructure(p.structure().loci(), List.of(new LocusRelation(new LocusId("b"), new LocusId("a"))));
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, reversedEdges, p.instructions()));
        var roles = p.structure().loci().stream().map(l -> new SpellLocus(l.id(), LocusRole.SOURCE, l.material(), l.expressedForms())).toList();
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, new SpellStructure(roles, p.structure().relations()), p.instructions()));
        var mediate = List.<SpellInstruction>of(new SpellInstruction.Operate(MagicalOperation.MEDIATE), new SpellInstruction.Shape(new Geometry.Line()), new SpellInstruction.Release());
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, p.structure(), mediate));
    }

    @Test void programRejectsInvalidStabilizationAndForgedSustain() {
        var fixed = program(tetrad(false));
        var relations = new ArrayList<>(fixed.structure().relations());
        relations.set(0, new LocusRelation(new LocusId("a"), new LocusId("b")));
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, new SpellStructure(fixed.structure().loci(), relations), fixed.instructions()));
        var unstable = program(tetrad(true));
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, unstable.structure(), fixed.instructions()));
        var p = program(dyad(false));
        var sustain = List.<SpellInstruction>of(new SpellInstruction.Operate(MagicalOperation.TRANSFER),
            new SpellInstruction.Shape(new Geometry.Enclosure(new Geometry.Line())), new SpellInstruction.Sustain());
        assertThrows(IllegalArgumentException.class, () -> new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, p.structure(), sustain));
    }
    @Test void vesselAffinityFollowsInvokedSourceWhenDirectionReverses() {
        var vessel = new dev.spellcraft.domain.vessel.VesselProfile(
            new dev.spellcraft.domain.manifestation.SpellBurden(10, 10, 10), true, Set.of(Forms.HEAT));
        var forward = program(dyad(false));
        var reverse = program(dyad(true));
        assertTrue(vessel.evaluate(forward, new BurdenModel().calculate(forward)).viable());
        assertEquals(List.of(dev.spellcraft.domain.vessel.ContainmentReport.Issue.FORM),
            vessel.evaluate(reverse, new BurdenModel().calculate(reverse)).issues());
    }

    @Test void burdenIsFiniteForInactiveSourceAndStructuralOnlyStabilizers() {
        var nodes = List.of(node("a", 0, 0, "empty"), node("b", 1, 0, "blaze", Forms.HEAT));
        var inactiveSource = program(new ArcaneWorking(nodes, List.of(edge("a", "b")), List.of()));
        for (var p : List.of(inactiveSource, program(tetrad(false)), program(tetrad(true)))) {
            assertTrue(p.invokedForms().isEmpty());
            var burden = new BurdenModel().calculate(p);
            assertEquals(1, burden.intensity());
            assertTrue(Double.isFinite(burden.complexity()));
            assertTrue(Double.isFinite(burden.persistence()));
        }
        assertThrows(IllegalArgumentException.class, () -> new SpellStructure(List.of(
            new SpellLocus(new LocusId("empty"), LocusRole.FOCUS, new MaterialId("test:empty"),
                new FormExpression(List.of()))), List.of()));
    }

}
