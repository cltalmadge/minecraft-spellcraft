package dev.spellcraft.domain.vessel;

import dev.spellcraft.domain.manifestation.SpellBurden;
import dev.spellcraft.domain.program.SpellProgram;

public interface VesselModel {
    ContainmentReport evaluate(SpellProgram program, SpellBurden burden);
}
