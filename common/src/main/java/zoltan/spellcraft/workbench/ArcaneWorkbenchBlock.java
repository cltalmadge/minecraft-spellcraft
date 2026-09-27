package zoltan.spellcraft.workbench;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ArcaneWorkbenchBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<WorkbenchPart> PART = EnumProperty.create("part", WorkbenchPart.class);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
        Block.box(0, 14, 0, 16, 16, 16), Block.box(1, 0, 1, 4, 14, 4), Block.box(12, 0, 1, 15, 14, 4),
        Block.box(1, 4, 1, 15, 6, 4)));

    public ArcaneWorkbenchBlock(Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
        // BACK preserves old prototype BE data until the incomplete structure is safely dismantled.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, WorkbenchPart.BACK));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == WorkbenchPart.BACK ? new ArcaneWorkbenchBlockEntity(pos, state) : null;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos();
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(PART, WorkbenchPart.FRONT);
        var other = ArcaneWorkbenchLayout.counterpartPos(pos, state);
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
            || !level.hasChunkAt(other) || !level.isInWorldBounds(other) || !level.getWorldBorder().isWithinBounds(other)
            || !level.getBlockState(pos).canBeReplaced(context) || !level.getBlockState(other).canBeReplaced(BlockPlaceContext.at(context, other, Direction.UP))) return null;
        var player = context.getPlayer();
        if (player != null && (!level.mayInteract(player, other) || !player.mayUseItemAt(other, Direction.UP, context.getItemInHand()))) return null;
        return level.isUnobstructed(state.setValue(PART, WorkbenchPart.BACK), other, CollisionContext.placementContext(player)) ? state : null;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(PART) == WorkbenchPart.BACK ? state.getValue(FACING) : state.getValue(FACING).getOpposite());
    }
    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var bench = ArcaneWorkbenchLayout.resolve(level, pos);
        return bench == null ? InteractionResult.PASS : bench.interact(player, hand, hit.getDirection(), hit.getLocation());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 20);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos otherPos, BlockState other, RandomSource random) {
        // Do not read absent chunks, nor tear down midway through the two placement writes.
        if (otherPos.equals(ArcaneWorkbenchLayout.counterpartPos(pos, state))) ticks.scheduleTick(pos, this, 1);
        return state;
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var other = ArcaneWorkbenchLayout.counterpartPos(pos, state);
        if (level.hasChunkAt(other) && !ArcaneWorkbenchLayout.matches(state, level.getBlockState(other))) {
            level.destroyBlock(pos, state.getValue(PART) == WorkbenchPart.BACK);
        } else {
            // Scheduled ticks persist across unload, including the structural half with no BE.
            level.scheduleTick(pos, this, 20);
        }
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(PART) == WorkbenchPart.FRONT) {
            var other = ArcaneWorkbenchLayout.counterpartPos(pos, state);
            if (level.hasChunkAt(other) && ArcaneWorkbenchLayout.matches(state, level.getBlockState(other))) level.destroyBlock(other, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        var other = ArcaneWorkbenchLayout.counterpartPos(pos, state);
        // The initiating position is already gone. Recursion cannot find a matching pair.
        // Only BACK has loot and contents; FRONT removal delegates their normal removal to BACK.
        if (level.hasChunkAt(other) && ArcaneWorkbenchLayout.matches(state, level.getBlockState(other)))
            level.destroyBlock(other, state.getValue(PART) == WorkbenchPart.FRONT);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
