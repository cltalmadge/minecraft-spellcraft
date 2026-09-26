package dev.spellcraft.prototype.domain;

public final class DefaultSpellCostModel implements SpellCostModel {
    private static final double TICKS_PER_SECOND = 20.0;

    @Override
    public int calculate(CostProfile profile, SpellEffectSpec effect) {
        int magnitude = Math.max(1, effect.magnitude());
        int durationTicks = Math.max(0, effect.durationTicks());
        double radius = Math.max(0.0, effect.radius());

        double magnitudeTerm = Math.pow(magnitude, profile.magnitudeExponent());
        double durationSeconds = durationTicks / TICKS_PER_SECOND;
        double durationTerm = 1.0 + durationSeconds * profile.durationWeightPerSecond();
        double radiusTerm = 1.0 + radius * profile.radiusWeight();

        double raw = profile.baseCost()
            * magnitudeTerm
            * durationTerm
            * radiusTerm
            * profile.multiplierFor(effect.delivery());

        if (!Double.isFinite(raw) || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Calculated spell cost is out of range");
        }

        return Math.max(0, (int) Math.ceil(raw));
    }
}
