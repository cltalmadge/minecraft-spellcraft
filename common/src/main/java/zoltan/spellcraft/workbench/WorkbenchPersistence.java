package zoltan.spellcraft.workbench;

import com.mojang.serialization.Codec;
import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.working.GridPoint;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import zoltan.minecraft.MinecraftMagic;
import zoltan.spellcraft.persistence.SpellCodecs;

final class WorkbenchPersistence {
    private static final Codec<java.util.List<dev.spellcraft.domain.form.FormParticipation>> EXPRESSION = strict(SpellCodecs.PARTICIPATION.listOf());
    private static final Codec<java.util.List<Long>> BOUNDARY = strict(Codec.LONG.listOf());
    private WorkbenchPersistence() {}
    // ValueInput accepts partial codec results. A partial expression would invent different magic.
    private static <A> Codec<A> strict(Codec<A> codec) {
        return Codec.of(codec, new com.mojang.serialization.Decoder<A>() {
            @Override public <T> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<A, T>> decode(
                com.mojang.serialization.DynamicOps<T> ops, T input) {
                var result = codec.decode(ops, input);
                if (result.error().isPresent()) return com.mojang.serialization.DataResult.error(() -> result.error().orElseThrow().message());
                return result;
            }
        });
    }
    static void save(ArcaneWorkbenchState state, ValueOutput out) {
        out.putLong("next_id", state.nextId()); out.putLong("revision", state.revision());
        var nodes = out.childrenList("loci");
        for (var n : state.loci()) {
            var v = nodes.addChild(); v.putLong("id", n.id()); v.putInt("x", n.point().x()); v.putInt("z", n.point().y());
            v.store("item", ItemStack.CODEC, n.item());
            v.store("expression", EXPRESSION, n.expression().terms()); v.putBoolean("resolved", n.resolved());
        }
        var relations = out.childrenList("relations");
        state.relations().forEach(r -> { var v = relations.addChild(); v.putLong("from", r.from()); v.putLong("to", r.to()); });
        out.store("boundary", BOUNDARY, state.boundary());
        if (!state.page().isEmpty()) out.store("page", ItemStack.CODEC, state.page());
        out.store("recovery", ItemStack.CODEC.listOf(), state.recovery());
    }
    static ArcaneWorkbenchState load(ValueInput in) {
        var state = new ArcaneWorkbenchState();
        for (var v : in.childrenListOrEmpty("loci")) {
            var item = v.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (item.isEmpty()) { warn("Undecodable material stack"); continue; }
            try {
                var terms = v.read("expression", EXPRESSION);
                if (terms.isEmpty()) warn("Invalid expression retained as unresolved material");
                var n = new ArcaneWorkbenchState.Locus(v.getLongOr("id", -1), new GridPoint(v.getIntOr("x", -1), v.getIntOr("z", -1)),
                    item, new FormExpression(terms.orElse(List.of())), terms.isPresent() && v.getBooleanOr("resolved", false));
                int before = state.loci().size(); state.restoreLocus(n);
                if (before == state.loci().size()) warn("Invalid locus moved to recovery tray");
            } catch (IllegalArgumentException e) { state.recover(item); warn("Invalid expression moved to recovery tray"); }
        }
        for (var v : in.childrenListOrEmpty("relations"))
            if (!state.connect(v.getLongOr("from", -1), v.getLongOr("to", -1))) warn("Discarded invalid relation");
        var boundary = in.read("boundary", BOUNDARY).orElse(List.of());
        if (!boundary.isEmpty() && !state.enclose(boundary)) warn("Discarded invalid boundary");
        state.restorePage(in.read("page", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        in.read("recovery", ItemStack.CODEC.listOf()).orElse(List.of()).forEach(state::recover);
        state.restoreCounters(in.getLongOr("next_id", 0), in.getLongOr("revision", 0));
        return state;
    }
    private static void warn(String message) { MinecraftMagic.LOGGER.warn("Arcane workbench: {}", message); }
}
