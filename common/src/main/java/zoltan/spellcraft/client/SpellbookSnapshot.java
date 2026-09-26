package zoltan.spellcraft.client;

import java.util.List;
import java.util.Objects;

/**
 * Immutable, read-only snapshot of everything the spellbook UI needs to render.
 *
 * <p>This is the client-facing data boundary that later networking can populate. It
 * carries no loader-specific types and no gameplay rules — it is a presentation/read
 * model only. The collection input is made immutable so callers cannot mutate shared
 * state after construction.
 *
 * <pre>
 * future server player state
 *         ↓
 * SpellbookSnapshot
 *         ↓
 * future network payload
 *         ↓
 * SpellbookScreen
 * </pre>
 */
public record SpellbookSnapshot(
        List<SpellbookEntrySnapshot> spells
) {
    public SpellbookSnapshot {
        Objects.requireNonNull(spells, "spells");
        spells = List.copyOf(spells);
    }
}
