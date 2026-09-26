package dev.spellcraft.domain.vessel;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.manifestation.SpellBurden;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.program.SpellProgram;

/** Affinities constrain only invoked Forms (empty accepts all); directional vessels prefer points and lines. */
public record VesselProfile(SpellBurden limits, boolean directional, Set<FormId> affinities) implements VesselModel {
    public VesselProfile { Objects.requireNonNull(limits); affinities = Set.copyOf(affinities); }
    public ContainmentReport evaluate(SpellProgram program, SpellBurden burden) {
        var issues = new ArrayList<ContainmentReport.Issue>();
        if (burden.intensity() > limits.intensity()) issues.add(ContainmentReport.Issue.INTENSITY);
        if (burden.complexity() > limits.complexity()) issues.add(ContainmentReport.Issue.COMPLEXITY);
        if (burden.persistence() > limits.persistence()) issues.add(ContainmentReport.Issue.PERSISTENCE);
        if (directional && !(program.geometry() instanceof Geometry.Point || program.geometry() instanceof Geometry.Line)) issues.add(ContainmentReport.Issue.GEOMETRY);
        if (!affinities.isEmpty() && program.invokedForms().stream().anyMatch(f -> !affinities.contains(f.form()))) issues.add(ContainmentReport.Issue.FORM);
        return new ContainmentReport(issues);
    }
}
