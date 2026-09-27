package zoltan.spellcraft.workbench;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class DividerItem extends Item {
    public DividerItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ArcaneWorkbenchBlockEntity bench) {
            return bench.interact(context.getPlayer(), context.getHand(), context.getClickedFace(), context.getClickLocation());
        }
        return InteractionResult.PASS;
    }
    @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        var selection = stack.get(DividerSelection.TYPE);
        if (selection == null) return;
        if (!(owner instanceof Player player) || !selection.dimension().equals(level.dimension().identifier().toString())
            || selection.expires() < level.getGameTime() || !player.isWithinBlockInteractionRange(selection.pos(), 0)
            || !level.hasChunkAt(selection.pos())
            || !(level.getBlockEntity(selection.pos()) instanceof ArcaneWorkbenchBlockEntity bench)
            || !bench.session().equals(selection.session()) || bench.state().locus(selection.locus()).isEmpty())
            stack.remove(DividerSelection.TYPE);
    }
}
