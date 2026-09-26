package dev.spellcraft.domain.program;

import java.util.List;
import java.util.Objects;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.LocusRole;
import dev.spellcraft.domain.pattern.MagicalOperation;
import dev.spellcraft.domain.pattern.SpellStructure;

/** Version 3 preserves explicit discovery-time Form expressions on declared loci. */
public record SpellProgram(int schemaVersion, SpellStructure structure, List<SpellInstruction> instructions) {

    public static final int CURRENT_SCHEMA_VERSION = 3;
    public SpellProgram {
        if (schemaVersion != CURRENT_SCHEMA_VERSION){ throw new IllegalArgumentException("Unsupported program schema"); }
        Objects.requireNonNull(structure);

        instructions = List.copyOf(instructions);

        if (instructions.size() != 3 || !(instructions.get(0) instanceof SpellInstruction.Operate(
                MagicalOperation operation1
        ))
            || !(instructions.get(1) instanceof SpellInstruction.Shape(Geometry geometry))) throw new IllegalArgumentException("Invalid program grammar");
        structure.validate(operation1, geometry);
        boolean sustainable = geometry instanceof Geometry.Enclosure
            && operation1 == MagicalOperation.STABILIZE && structure.hasEquivalentStabilizers();
        if (sustainable ? !(instructions.getLast() instanceof SpellInstruction.Sustain)
            : !(instructions.getLast() instanceof SpellInstruction.Release)) throw new IllegalArgumentException("Invalid release policy");
    }
    public MagicalOperation operation() { return ((SpellInstruction.Operate) instructions.get(0)).operation(); }
    public Geometry geometry() { return ((SpellInstruction.Shape) instructions.get(1)).geometry(); }

    /** Every Form semantically present anywhere in the pattern, without aggregation.
     * Structural accounting only; manifestation preflight and basic affinity use invokedForms(). */
    public List<FormParticipation> participatingForms() {
        return structure.loci().stream().flatMap(l -> l.expressedForms().terms().stream()).toList();
    }

    /** Only the bound source/focus supplies the released influence in the current runtime. */
    public List<FormParticipation> invokedForms() {
        return switch (operation()) {
            case CONCENTRATE -> structure.single(LocusRole.FOCUS).expressedForms().terms();
            case TRANSFER, MEDIATE -> structure.single(LocusRole.SOURCE).expressedForms().terms();
            case STABILIZE -> List.of(); // Temporal manifestation remains unsupported.
        };
    }
}
