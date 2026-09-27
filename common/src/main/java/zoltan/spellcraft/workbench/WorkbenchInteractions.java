package zoltan.spellcraft.workbench;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import zoltan.spellcraft.material.MaterialProfileResolver;

/** Explicit precedence: control strip, Divider, empty-hand recovery, then material placement. */
final class WorkbenchInteractions {
    private WorkbenchInteractions() {}
    static void handle(ArcaneWorkbenchBlockEntity bench, Player player, InteractionHand hand, double x, double z) {
        var state = bench.state(); var held = player.getItemInHand(hand);
        var control = WorkbenchCoordinates.control(x, z, true);
        if (control != WorkbenchCoordinates.Control.NONE) {
            switch (control) {
                case PAGE -> {
                    if (held.isEmpty()) give(player, state.takePage());
                    else if (state.insertPage(held)) held.shrink(1);
                    else message(player, "blank_page");
                }
                case ACTIVATE -> {
                    if (held.isEmpty()) {
                        if (player.isShiftKeyDown() && Boolean.getBoolean("spellcraft.workbench.debug")) inspect(bench, player);
                        else bench.activate(player);
                    }
                }
                case RECORD -> {
                    if (held.isEmpty()) message(player, bench.record(player) ? "recorded" : "cannot_record");
                }
                case ENCLOSURE -> {
                    if (held.getItem() instanceof DividerItem) {
                        held.remove(DividerSelection.TYPE);
                        if (player.isShiftKeyDown()) state.clearBoundary();
                        else state.enclose(state.loci().stream().map(ArcaneWorkbenchState.Locus::id).toList());
                    }
                }
                default -> {}
            }
            return;
        }
        var point = WorkbenchCoordinates.grid(x, z, true);
        if (point.isEmpty()) return;
        var locus = state.at(point.get());
        if (held.getItem() instanceof DividerItem) {
            if (locus.isEmpty()) { held.remove(DividerSelection.TYPE); return; }
            var prior = held.get(DividerSelection.TYPE);
            held.remove(DividerSelection.TYPE);
            if (prior == null || !prior.pos().equals(bench.getBlockPos()) || !prior.session().equals(bench.session())
                || !prior.dimension().equals(player.level().dimension().identifier().toString())
                || prior.expires() < player.level().getGameTime() || state.locus(prior.locus()).isEmpty()) {
                held.set(DividerSelection.TYPE, new DividerSelection(bench.getBlockPos(), player.level().dimension().identifier().toString(),
                    locus.get().id(), bench.session(), player.level().getGameTime() + 600));
                message(player, "selected"); return;
            }
            boolean success = player.isShiftKeyDown() ? state.disconnect(prior.locus(), locus.get().id()) : state.connect(prior.locus(), locus.get().id());
            if (!success) message(player, "relation_failed");
            return;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown() && locus.isPresent()) give(player, state.remove(locus.get().id()));
            return;
        }
        if (locus.isPresent()) { message(player, "occupied"); return; }
        // Unknown materials are deliberately inert anchors; their identity is still preserved.
        var expression = WorkbenchWorkingAdapter.initialExpression(MaterialProfileResolver.bootstrap().resolve(held));
        if (state.place(point.get(), held, expression.forms(), expression.resolved()).isPresent()) held.shrink(1);
    }
    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
    static void message(Player player, String key) { player.sendOverlayMessage(Component.translatable("workbench.spellcraft." + key)); }
    private static void inspect(ArcaneWorkbenchBlockEntity bench, Player player) {
        bench.reanalyze(); var snapshot = bench.analysis();
        var text = new StringBuilder("Workbench revision " + bench.state().revision() + "\n");
        snapshot.analysis().pattern().ifPresent(p -> {
            text.append(p.principle()).append(" / ").append(p.operation()).append(" / ").append(p.geometry()).append("\n");
            p.structure().loci().forEach(l -> text.append(l.role()).append(" ").append(l.material().value()).append(" ").append(l.expressedForms().terms()).append("\n"));
        });
        text.append(snapshot.unresolved() ? "Expression unresolved" : snapshot.analysis().diagnostics());
        player.sendSystemMessage(Component.literal(text.toString()));
    }
}
