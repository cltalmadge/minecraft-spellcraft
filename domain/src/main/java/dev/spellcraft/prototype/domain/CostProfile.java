package dev.spellcraft.prototype.domain;

import java.util.Map;
import java.util.Objects;

public record CostProfile(
    double baseCost,
    double magnitudeExponent,
    double durationWeightPerSecond,
    double radiusWeight,
    Map<Delivery, Double> deliveryMultipliers
) {
    public CostProfile {
        if (!Double.isFinite(baseCost) || baseCost < 0) {
            throw new IllegalArgumentException("baseCost must be finite and non-negative");
        }
        if (!Double.isFinite(magnitudeExponent) || magnitudeExponent < 0) {
            throw new IllegalArgumentException("magnitudeExponent must be finite and non-negative");
        }
        if (!Double.isFinite(durationWeightPerSecond) || durationWeightPerSecond < 0) {
            throw new IllegalArgumentException("durationWeightPerSecond must be finite and non-negative");
        }
        if (!Double.isFinite(radiusWeight) || radiusWeight < 0) {
            throw new IllegalArgumentException("radiusWeight must be finite and non-negative");
        }

        Objects.requireNonNull(deliveryMultipliers, "deliveryMultipliers");
        deliveryMultipliers = Map.copyOf(deliveryMultipliers);
    }

    public double multiplierFor(Delivery delivery) {
        Double multiplier = deliveryMultipliers.get(delivery);
        if (multiplier == null) {
            throw new IllegalArgumentException("No cost multiplier for delivery: " + delivery);
        }
        return multiplier;
    }
}
