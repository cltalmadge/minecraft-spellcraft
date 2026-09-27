package zoltan.spellcraft.workbench;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Commit both positions before neighbor notifications or item consumption. */
public final class ArcaneWorkbenchItem extends BlockItem {
    public ArcaneWorkbenchItem(Block block, Properties properties) { super(block, properties); }
    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var level = context.getLevel(); var front = context.getClickedPos();
        if (getBlock().getStateForPlacement(context) == null) return false;
        var back = ArcaneWorkbenchLayout.counterpartPos(front, state);
        var previous = level.getBlockState(front);
        var controller = state.setValue(ArcaneWorkbenchBlock.PART, WorkbenchPart.BACK);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(front, state, flags)) return false;
        if (!level.setBlock(back, controller, flags)) {
            level.setBlock(front, previous, flags);
            return false;
        }
        level.updateNeighborsAt(front, getBlock(), null); level.updateNeighborsAt(back, getBlock(), null);
        state.updateNeighbourShapes(level, front, Block.UPDATE_ALL);
        controller.updateNeighbourShapes(level, back, Block.UPDATE_ALL);
        return true;
    }
}
