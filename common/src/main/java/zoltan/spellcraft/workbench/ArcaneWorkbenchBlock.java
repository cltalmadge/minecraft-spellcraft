package zoltan.spellcraft.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArcaneWorkbenchBlock extends BaseEntityBlock {
    public ArcaneWorkbenchBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneWorkbenchBlockEntity(pos, state); }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity bench ? bench.interact(player, hand, hit.getDirection(), hit.getLocation()) : InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity bench ? bench.interact(player, InteractionHand.MAIN_HAND, hit.getDirection(), hit.getLocation()) : InteractionResult.PASS;
    }
}
