package zoltan.spellcraft.client;

import java.util.List;
import java.util.Objects;

/**
 * Read-only, presentation-only view of a single spell as shown in the spellbook UI.
 *
 * <p>This is a transport-friendly data boundary, not an authoritative gameplay model:
 * it carries only what the screen needs to render, and its collection input is made
 * immutable so callers cannot mutate shared state after construction.
 */
public record SpellbookEntrySnapshot(
        String name,
        List<String> detailLines
) {
    public SpellbookEntrySnapshot {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(detailLines, "detailLines");
        detailLines = List.copyOf(detailLines);
    }
}
