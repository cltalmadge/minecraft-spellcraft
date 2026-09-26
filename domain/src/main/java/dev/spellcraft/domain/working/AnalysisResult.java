package dev.spellcraft.domain.working;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.spellcraft.domain.pattern.SpellPattern;

public record AnalysisResult(Optional<SpellPattern> pattern, List<WorkingDiagnostic> diagnostics) {
    public AnalysisResult { Objects.requireNonNull(pattern); diagnostics = List.copyOf(diagnostics); }
}
