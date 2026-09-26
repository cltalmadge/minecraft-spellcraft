package dev.spellcraft.domain.program;

import java.util.Objects;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.MagicalOperation;

public sealed interface SpellInstruction {
    record Invoke(FormParticipation participation) implements SpellInstruction {
        public Invoke { Objects.requireNonNull(participation); if (participation.strength() == 0) throw new IllegalArgumentException("Inactive invocation"); }
    }
    record Operate(MagicalOperation operation) implements SpellInstruction {
        public Operate { Objects.requireNonNull(operation); }
    }
    record Shape(Geometry geometry) implements SpellInstruction {
        public Shape { Objects.requireNonNull(geometry); }
    }
    record Release() implements SpellInstruction {}
    record Sustain() implements SpellInstruction {}
}
