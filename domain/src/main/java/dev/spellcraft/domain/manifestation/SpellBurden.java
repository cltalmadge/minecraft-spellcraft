package dev.spellcraft.domain.manifestation;


public record SpellBurden(double intensity, double complexity, double persistence) {
    public SpellBurden {
        for (double v : new double[]{intensity, complexity, persistence}) if (!Double.isFinite(v) || v < 0) throw new IllegalArgumentException("Invalid burden");
    }
}
