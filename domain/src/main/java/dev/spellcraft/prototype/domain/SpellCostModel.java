package dev.spellcraft.prototype.domain;

public interface SpellCostModel {
    int calculate(CostProfile profile, SpellEffectSpec effect);
}
