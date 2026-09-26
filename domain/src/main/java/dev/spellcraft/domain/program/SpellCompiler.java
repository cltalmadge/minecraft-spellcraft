package dev.spellcraft.domain.program;

import java.util.ArrayList;
import java.util.Objects;

import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.SpellPattern;

public final class SpellCompiler {
    public SpellProgram compile(SpellPattern pattern) {
        Objects.requireNonNull(pattern);
        var instructions = new ArrayList<SpellInstruction>();
        instructions.add(new SpellInstruction.Operate(pattern.operation()));
        instructions.add(new SpellInstruction.Shape(pattern.geometry()));
        instructions.add(pattern.geometry() instanceof Geometry.Enclosure && pattern.stabilized()
            ? new SpellInstruction.Sustain() : new SpellInstruction.Release());
        return new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, pattern.structure(), instructions);
    }
}
