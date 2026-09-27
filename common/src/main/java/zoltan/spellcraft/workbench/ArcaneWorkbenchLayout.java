package zoltan.spellcraft.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** FACING points from the player/front toward the back. Canonical v runs from back to front. */
public final class ArcaneWorkbenchLayout {
    private ArcaneWorkbenchLayout() {}
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return controllerPos(pos, state.getValue(ArcaneWorkbenchBlock.FACING), state.getValue(ArcaneWorkbenchBlock.PART));
    }
    public static BlockPos controllerPos(BlockPos pos, Direction facing, WorkbenchPart part) {
        return part == WorkbenchPart.BACK ? pos : pos.relative(facing);
    }
    public static BlockPos counterpartPos(BlockPos pos, BlockState state) {
        return counterpartPos(pos, state.getValue(ArcaneWorkbenchBlock.FACING), state.getValue(ArcaneWorkbenchBlock.PART));
    }
    public static BlockPos counterpartPos(BlockPos pos, Direction facing, WorkbenchPart part) {
        return pos.relative(part == WorkbenchPart.FRONT ? facing : facing.getOpposite());
    }
    public static boolean matches(BlockState state, BlockState other) {
        return state.getBlock() instanceof ArcaneWorkbenchBlock && other.is(state.getBlock())
            && state.getValue(ArcaneWorkbenchBlock.FACING) == other.getValue(ArcaneWorkbenchBlock.FACING)
            && state.getValue(ArcaneWorkbenchBlock.PART) != other.getValue(ArcaneWorkbenchBlock.PART);
    }
    /** Unknown/unloaded is not a valid interactive pair, but must never be treated as destroyed. */
    public static boolean validPair(LevelReader level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ArcaneWorkbenchBlock)) return false;
        var other = counterpartPos(pos, state);
        return level.hasChunkAt(pos) && level.hasChunkAt(other) && matches(state, level.getBlockState(other));
    }
    public static ArcaneWorkbenchBlockEntity resolve(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var state = level.getBlockState(pos);
        if (!validPair(level, pos, state)) return null;
        return level.getBlockEntity(controllerPos(pos, state)) instanceof ArcaneWorkbenchBlockEntity bench ? bench : null;
    }
    public static boolean withinReach(Player player, BlockPos controller, BlockState state) {
        return player.isWithinBlockInteractionRange(controller, 0)
            || player.isWithinBlockInteractionRange(counterpartPos(controller, state), 0);
    }
    /** Normalized complete surface: u=0..1 across, v=0..1 along its two-block depth. */
    public static Vec3 local(BlockPos controller, Direction facing, Vec3 world) {
        var delta = world.subtract(Vec3.atBottomCenterOf(controller));
        var right = facing.getClockWise();
        return new Vec3(.5 + delta.x * right.getStepX() + delta.z * right.getStepZ(), delta.y,
            (.5 - delta.x * facing.getStepX() - delta.z * facing.getStepZ()) / 2);
    }
    public static Vec3 world(BlockPos controller, Direction facing, double u, double y, double v) {
        var right = facing.getClockWise();
        return Vec3.atBottomCenterOf(controller).add(right.getStepX() * (u-.5) - facing.getStepX() * (2*v-.5), y,
            right.getStepZ() * (u-.5) - facing.getStepZ() * (2*v-.5));
    }
    public static Vec3 local(BlockPos pos, BlockState state, Vec3 world) {
        return local(controllerPos(pos, state), state.getValue(ArcaneWorkbenchBlock.FACING), world);
    }
    public static float rotation(Direction facing) {
        return switch (facing) { case NORTH -> 0; case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> throw new IllegalArgumentException("Horizontal facing required"); };
    }
    public static AABB bounds(BlockPos controller, Direction facing) {
        return new AABB(controller).minmax(new AABB(counterpartPos(controller, facing, WorkbenchPart.BACK)))
            .expandTowards(0, .25, 0);
    }
}
