package dev.spellcraft.domain.program;

import java.util.ArrayList;
import java.util.Objects;

import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.SpellPattern;

public final class SpellCompiler {
    public SpellProgram compile(SpellPattern pattern) {
        Objects.requireNonNull(pattern);
        var instructions = new ArrayList<SpellInstruction>();
        pattern.forms().terms().forEach(f -> instructions.add(new SpellInstruction.Invoke(f)));
        instructions.add(new SpellInstruction.Operate(pattern.operation()));
        instructions.add(new SpellInstruction.Shape(pattern.geometry()));
        instructions.add(pattern.geometry() instanceof Geometry.Enclosure ? new SpellInstruction.Sustain() : new SpellInstruction.Release());
        return new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, instructions);
    }
}
