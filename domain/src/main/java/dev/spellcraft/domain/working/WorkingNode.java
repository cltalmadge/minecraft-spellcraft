package dev.spellcraft.domain.working;

import java.util.Objects;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.material.MaterialProfile;

/** A material's intrinsic potential and the subset contributed to this experiment. */
public record WorkingNode(NodeId id, GridPoint position, MaterialProfile material, FormExpression expressedForms) {
    public WorkingNode {
        Objects.requireNonNull(id);
        Objects.requireNonNull(position);
        Objects.requireNonNull(material);
        Objects.requireNonNull(expressedForms);
        for (var term : expressedForms.terms()) {
            if (term.strength() > material.participation(term.form()))
                throw new IllegalArgumentException("Expression exceeds intrinsic participation: " + term.form().value());
        }
    }

    public static WorkingNode expressingAll(NodeId id, GridPoint position, MaterialProfile material) {
        return new WorkingNode(id, position, material,
            new FormExpression(material.forms().stream().filter(f -> f.strength() > 0).toList()));
    }
}
