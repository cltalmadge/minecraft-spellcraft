package dev.spellcraft.prototype.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultSpellCostModelTest {
    private static final EffectId DAMAGE_HEALTH = EffectId.of("spellcraft:damage_health");

    private final SpellCostModel costModel = new DefaultSpellCostModel();

    private final CostProfile profile = new CostProfile(
        2.0,
        1.15,
        0.10,
        0.25,
        Map.of(
            Delivery.SELF, 0.75,
            Delivery.RAY, 1.10
        )
    );

    @Test
    void sameInputsProduceSameCost() {
        SpellEffectSpec effect = new SpellEffectSpec(DAMAGE_HEALTH, Delivery.RAY, 6, 0, 0);

        assertEquals(
            costModel.calculate(profile, effect),
            costModel.calculate(profile, effect)
        );
    }

    @Test
    void largerMagnitudeNeverReducesCost() {
        int previous = -1;

        for (int magnitude = 1; magnitude <= 20; magnitude++) {
            SpellEffectSpec effect = new SpellEffectSpec(
                DAMAGE_HEALTH,
                Delivery.RAY,
                magnitude,
                0,
                0
            );

            int current = costModel.calculate(profile, effect);
            assertTrue(current >= previous, "cost decreased at magnitude " + magnitude);
            previous = current;
        }
    }

    @Test
    void rayDeliveryCanCostMoreThanSelf() {
        SpellEffectSpec self = new SpellEffectSpec(DAMAGE_HEALTH, Delivery.SELF, 5, 0, 0);
        SpellEffectSpec ray = new SpellEffectSpec(DAMAGE_HEALTH, Delivery.RAY, 5, 0, 0);

        assertTrue(costModel.calculate(profile, ray) > costModel.calculate(profile, self));
    }
}
