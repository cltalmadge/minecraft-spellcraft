package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.working.GridPoint;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import zoltan.spellcraft.persistence.SpellComponents;

/** Physical facts only. Item ownership is transferred through copies, never exposed for mutation. */
public final class ArcaneWorkbenchState {
    public record Locus(long id, GridPoint point, ItemStack item, FormExpression expression, boolean resolved) {
        public Locus { item = item.copy(); }
        @Override public ItemStack item() { return item.copy(); }
    }
    public record Relation(long from, long to) {}
    private final List<Locus> loci = new ArrayList<>();
    private final List<Relation> relations = new ArrayList<>();
    private List<Long> boundary = List.of();
    private final List<ItemStack> recovery = new ArrayList<>();
    private ItemStack page = ItemStack.EMPTY;
    private long nextId, revision;
    public List<Locus> loci() { return List.copyOf(loci); }
    public List<Relation> relations() { return List.copyOf(relations); }
    public List<Long> boundary() { return boundary; }
    public ItemStack page() { return page.copy(); }
    public long revision() { return revision; }
    long nextId() { return nextId; }
    List<ItemStack> recovery() { return recovery.stream().map(ItemStack::copy).toList(); }
    public Optional<Locus> at(GridPoint p) { return loci.stream().filter(n -> n.point().equals(p)).findFirst(); }
    public Optional<Locus> locus(long id) { return loci.stream().filter(n -> n.id() == id).findFirst(); }
    public Optional<Locus> place(GridPoint p, ItemStack item, FormExpression expression, boolean resolved) {
        if (!WorkbenchCoordinates.valid(p) || at(p).isPresent() || item.isEmpty() || nextId == Long.MAX_VALUE) return Optional.empty();
        var locus = new Locus(nextId++, p, item.copyWithCount(1), expression, resolved);
        loci.add(locus); boundary = List.of(); revision++; return Optional.of(locus);
    }
    public ItemStack remove(long id) {
        var found = locus(id);
        if (found.isEmpty()) return ItemStack.EMPTY;
        loci.remove(found.get()); relations.removeIf(r -> r.from() == id || r.to() == id);
        boundary = List.of(); revision++; return found.get().item();
    }
    public boolean connect(long from, long to) {
        var r = new Relation(from, to);
        if (from == to || locus(from).isEmpty() || locus(to).isEmpty() || relations.contains(r)) return false;
        relations.add(r); revision++; return true;
    }
    public boolean disconnect(long from, long to) {
        if (!relations.remove(new Relation(from, to))) return false;
        revision++; return true;
    }
    public boolean enclose(List<Long> ids) {
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size()
            || !new HashSet<>(ids).equals(new HashSet<>(loci.stream().map(Locus::id).toList()))) return false;
        var sorted = ids.stream().sorted().toList();
        if (boundary.equals(sorted)) return false;
        boundary = sorted; revision++; return true;
    }
    public boolean clearBoundary() {
        if (boundary.isEmpty()) return false;
        boundary = List.of(); revision++; return true;
    }
    public boolean insertPage(ItemStack stack) {
        if (!page.isEmpty() || !WorkbenchContent.isPage(stack) || stack.has(SpellComponents.RECORDED_SPELL)) return false;
        page = stack.copyWithCount(1); revision++; return true;
    }
    public ItemStack takePage() {
        var result = page; if (!page.isEmpty()) { page = ItemStack.EMPTY; revision++; } return result;
    }
    boolean record(dev.spellcraft.domain.program.RecordedSpell spell) {
        if (page.isEmpty() || !WorkbenchContent.isPage(page) || page.has(SpellComponents.RECORDED_SPELL)) return false;
        page.set(SpellComponents.RECORDED_SPELL, spell); revision++; return true;
    }
    public List<ItemStack> drain() {
        var items = new ArrayList<>(loci.stream().map(Locus::item).toList());
        if (!page.isEmpty()) items.add(page);
        items.addAll(recovery); recovery.clear(); loci.clear(); relations.clear(); boundary = List.of(); page = ItemStack.EMPTY; revision++;
        return items;
    }
    // Persistence repair preserves misplaced/duplicate material stacks in a recoverable tray.
    void restoreLocus(Locus n) {
        if (n.id() < 0 || n.id() == Long.MAX_VALUE || !WorkbenchCoordinates.valid(n.point()) || locus(n.id()).isPresent() || at(n.point()).isPresent()) {
            recover(n.item()); return;
        }
        loci.add(n); nextId = Math.max(nextId, n.id() + 1);
    }
    void recover(ItemStack item) { if (!item.isEmpty()) recovery.add(item.copy()); }
    void restorePage(ItemStack item) { page = item; }
    void restoreCounters(long next, long rev) { nextId = Math.max(nextId, Math.max(0, next)); revision = Math.max(0, rev); }
}
