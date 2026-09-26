package dev.spellcraft.domain.program;

import java.util.HashSet;
import java.util.List;

import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.MagicalOperation;

/** Version 1 grammar: Invoke+, Operate, Shape, Release|Sustain. */
public record SpellProgram(int schemaVersion, List<SpellInstruction> instructions) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_INSTRUCTIONS = 32;
    public SpellProgram {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported program schema");
        instructions = List.copyOf(instructions);
        int end = instructions.size();
        if (end < 4 || end > MAX_INSTRUCTIONS || !(instructions.get(end - 3) instanceof SpellInstruction.Operate operation) ||
            !(instructions.get(end - 2) instanceof SpellInstruction.Shape shape)) throw new IllegalArgumentException("Invalid program grammar");
        var forms = new HashSet<FormId>();
        for (int i = 0; i < end - 3; i++) {
            if (!(instructions.get(i) instanceof SpellInstruction.Invoke invoke) || !forms.add(invoke.participation().form())) throw new IllegalArgumentException("Invalid invocation");
        }
        boolean contained = shape.geometry() instanceof Geometry.Enclosure;
        if (contained && operation.operation() != MagicalOperation.STABILIZE) throw new IllegalArgumentException("Unstable enclosure");
        if (contained ? !(instructions.getLast() instanceof SpellInstruction.Sustain) : !(instructions.getLast() instanceof SpellInstruction.Release)) throw new IllegalArgumentException("Invalid release policy");
    }
    public MagicalOperation operation() { return ((SpellInstruction.Operate) instructions.get(instructions.size() - 3)).operation(); }
    public Geometry geometry() { return ((SpellInstruction.Shape) instructions.get(instructions.size() - 2)).geometry(); }
    public List<FormParticipation> forms() { return instructions.stream().filter(SpellInstruction.Invoke.class::isInstance).map(i -> ((SpellInstruction.Invoke)i).participation()).toList(); }
}
