package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.material.MaterialProfile;
import dev.spellcraft.domain.working.*;
import java.util.*;
import zoltan.spellcraft.material.MaterialProfileResolver;

public final class WorkbenchWorkingAdapter {
    public record Expression(FormExpression forms, boolean resolved) {}
    public record Snapshot(Optional<ArcaneWorking> working, AnalysisResult analysis, boolean unresolved) {}
    private WorkbenchWorkingAdapter() {}
    public static Expression initialExpression(MaterialProfile profile) {
        var active = profile.forms().stream().filter(f -> f.strength() > 0).toList();
        return new Expression(new FormExpression(active.size() == 1 ? active : List.of()), active.size() <= 1);
    }
    public static NodeId nodeId(long id) { return new NodeId("locus_" + id); }
    public static Snapshot analyze(ArcaneWorkbenchState state, MaterialProfileResolver profiles) {
        var nodes = new ArrayList<WorkingNode>();
        for (var locus : state.loci()) {
            var profile = profiles.resolve(locus.item());
            if (!locus.resolved() || locus.expression().terms().stream().anyMatch(f -> f.strength() > profile.participation(f.form())))
                return new Snapshot(Optional.empty(), new AnalysisResult(Optional.empty(), List.of()), true);
            nodes.add(new WorkingNode(nodeId(locus.id()), locus.point(), profile, locus.expression()));
        }
        var working = new ArcaneWorking(nodes,
            state.relations().stream().map(r -> new WorkingStroke(nodeId(r.from()), nodeId(r.to()))).toList(),
            state.boundary().isEmpty() ? List.of() : List.of(new WorkingBoundary(state.boundary().stream().map(WorkbenchWorkingAdapter::nodeId).toList())));
        return new Snapshot(Optional.of(working), new WorkingAnalyzer().analyze(working), false);
    }
}
