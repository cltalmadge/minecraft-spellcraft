package dev.spellcraft.domain.integration;

import dev.spellcraft.domain.form.*;
import dev.spellcraft.domain.material.*;
import dev.spellcraft.domain.pattern.*;
import dev.spellcraft.domain.program.*;
import dev.spellcraft.domain.working.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormExpressionTest {
    private FormExpression expression(FormId form, double strength) {
        return new FormExpression(List.of(new FormParticipation(form, strength)));
    }
    private WorkingNode node(String id, int x, int y, MaterialProfile material, FormExpression expression) {
        return new WorkingNode(new NodeId(id), new GridPoint(x, y), material, expression);
    }
    private SpellProgram compile(WorkingNode source) {
        var recipient = node("recipient", 1, 0, source.material(), new FormExpression(List.of()));
        var pattern = new WorkingAnalyzer().analyze(new ArcaneWorking(List.of(source, recipient),
            List.of(new WorkingStroke(source.id(), recipient.id())), List.of())).pattern().orElseThrow();
        return new SpellCompiler().compile(pattern);
    }

    @Test void intrinsicPotentialDoesNotBecomeExpressedOrInvokedAndProfileReplacementCannotRewriteProgram() {
        var id = new MaterialId("test:blaze");
        var material = new MaterialProfile(id, List.of(new FormParticipation(Forms.HEAT, 1),
            new FormParticipation(Forms.MOTION, 0.5), new FormParticipation(new FormId("test:light"), 0.2)));
        var heat = expression(Forms.HEAT, 1);
        var source = node("source", 0, 0, material, heat);
        var program = compile(source);
        assertEquals(material, source.material());
        assertEquals(heat, program.structure().single(LocusRole.SOURCE).expressedForms());
        assertEquals(id, program.structure().single(LocusRole.SOURCE).material());
        assertEquals(heat.terms(), program.invokedForms());
        var changed = new MaterialProfile(id, List.of(new FormParticipation(new FormId("test:light"), 1)));
        assertEquals(id, changed.material());
        assertEquals(0, changed.participation(Forms.HEAT));
        assertEquals(heat.terms(), program.participatingForms());
        assertEquals(heat.terms(), program.invokedForms());
        assertEquals(program, compile(source));
    }

    @Test void expressionRejectsAbsentFormsAndExcessStrength() {
        var heat = new MaterialProfile(new MaterialId("test:heat"), expression(Forms.HEAT, 1).terms());
        assertThrows(IllegalArgumentException.class, () -> node("a", 0, 0, heat, expression(Forms.MOTION, 1)));
        var motion = new MaterialProfile(new MaterialId("test:motion"), expression(Forms.MOTION, 0.35).terms());
        assertThrows(IllegalArgumentException.class, () -> node("a", 0, 0, motion, expression(Forms.MOTION, 0.8)));
        assertEquals(expression(Forms.MOTION, 0.2), node("a", 0, 0, motion, expression(Forms.MOTION, 0.2)).expressedForms());
        assertEquals(expression(Forms.MOTION, 0.35), WorkingNode.expressingAll(new NodeId("a"), new GridPoint(0, 0), motion).expressedForms());
    }

    @Test void unexpressedIntrinsicFormsCannotActivateWorkingOrChangeAnchorRoles() {
        var material = new MaterialProfile(new MaterialId("test:blaze"), expression(Forms.HEAT, 1).terms());
        var empty = new FormExpression(List.of());
        var inactive = node("x", 0, 0, material, empty);
        var result = new WorkingAnalyzer().analyze(new ArcaneWorking(List.of(inactive), List.of(), List.of()));
        assertTrue(result.pattern().isEmpty());
        assertEquals(WorkingDiagnostic.Kind.NO_ACTIVE_FORM, result.diagnostics().getFirst().kind());
        var center = node("x", 0, 0, material, expression(Forms.HEAT, 1));
        var nodes = List.of(center, node("a", 1, 0, material, empty), node("b", -1, 0, material, empty),
            node("c", 0, 1, material, empty), node("d", 0, -1, material, empty));
        var edges = nodes.stream().skip(1).map(n -> new WorkingStroke(center.id(), n.id())).toList();
        var pattern = new WorkingAnalyzer().analyze(new ArcaneWorking(nodes, edges, List.of())).pattern().orElseThrow();
        assertEquals(NumericalPrinciple.MONAD, pattern.principle());
        assertEquals(4, pattern.structure().withRole(LocusRole.ANCHOR).size());
        assertEquals(expression(Forms.HEAT, 1).terms(), new SpellCompiler().compile(pattern).invokedForms());
    }
}
