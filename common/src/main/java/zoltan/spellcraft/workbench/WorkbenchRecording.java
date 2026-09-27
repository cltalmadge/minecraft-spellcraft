package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.program.RecordedSpell;
import dev.spellcraft.domain.program.SpellCompiler;

public final class WorkbenchRecording {
    private WorkbenchRecording() {}
    public static boolean record(ArcaneWorkbenchState state, WorkbenchWorkingAdapter.Snapshot current) {
        if (current.unresolved() || current.analysis().pattern().isEmpty()) return false;
        var program = new SpellCompiler().compile(current.analysis().pattern().orElseThrow());
        return state.record(new RecordedSpell(RecordedSpell.CURRENT_SCHEMA_VERSION, "Workbench observation", program));
    }
}
