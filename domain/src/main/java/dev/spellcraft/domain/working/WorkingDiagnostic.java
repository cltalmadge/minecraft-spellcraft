package dev.spellcraft.domain.working;

import java.util.Objects;
import java.util.Optional;

import dev.spellcraft.domain.form.FormId;

public record WorkingDiagnostic(Kind kind, Optional<NodeId> node, Optional<FormId> form) {
    public enum Kind { EMPTY_WORKING, NO_ACTIVE_FORM, INCOMPLETE_RELATION, UNSTABLE_STRUCTURE, AMBIGUOUS_STRUCTURE, UNCONTAINED_INFLUENCE, COHERENT }
    public WorkingDiagnostic { Objects.requireNonNull(kind); Objects.requireNonNull(node); Objects.requireNonNull(form); }
    public WorkingDiagnostic(Kind kind) { this(kind, Optional.empty(), Optional.empty()); }
}
