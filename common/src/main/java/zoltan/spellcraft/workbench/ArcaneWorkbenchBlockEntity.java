package zoltan.spellcraft.workbench;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import zoltan.spellcraft.material.MaterialProfileResolver;

public final class ArcaneWorkbenchBlockEntity extends BlockEntity {
    private ArcaneWorkbenchState state = new ArcaneWorkbenchState();
    private final String session = UUID.randomUUID().toString();
    private WorkbenchWorkingAdapter.Snapshot snapshot;
    private long lastActivation = Long.MIN_VALUE;
    public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState blockState) { super(WorkbenchContent.entityType.get(), pos, blockState); reanalyze(); }
    /** Read-only outside the workbench package; server interaction owns mutations and changed(). */
    public ArcaneWorkbenchState state() { return state; }
    public String session() { return session; }
    public WorkbenchWorkingAdapter.Snapshot analysis() { return snapshot; }
    public void reanalyze() { snapshot = WorkbenchWorkingAdapter.analyze(state, MaterialProfileResolver.bootstrap()); }
    public boolean authorized(Player player) {
        return level instanceof ServerLevel && !isRemoved() && !player.isSpectator() && player.mayBuild()
            && player.level() == level && player.isWithinBlockInteractionRange(worldPosition, 0)
            && level.getBlockEntity(worldPosition) == this;
    }
    public InteractionResult interact(Player player, InteractionHand hand, Direction face, Vec3 hit) {
        if (face != Direction.UP) return InteractionResult.PASS;
        if (level == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!authorized(player)) return InteractionResult.FAIL;
        var local = hit.subtract(Vec3.atLowerCornerOf(worldPosition));
        if (Math.abs(local.y - 1) > .01) return InteractionResult.FAIL;
        long before = state.revision();
        WorkbenchInteractions.handle(this, player, hand, local.x, local.z);
        if (state.revision() != before) changed();
        return InteractionResult.SUCCESS;
    }
    void changed() {
        reanalyze(); setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    boolean activate(Player player) {
        if (!authorized(player) || (lastActivation != Long.MIN_VALUE && level.getGameTime() - lastActivation < 10)) return false;
        reanalyze(); lastActivation = level.getGameTime();
        WorkbenchFeedback.classify(snapshot).emit((ServerLevel)level, worldPosition, state);
        if (snapshot.unresolved()) WorkbenchInteractions.message(player, "unresolved");
        return true;
    }
    boolean record(Player player) {
        if (!authorized(player)) return false;
        reanalyze(); return WorkbenchRecording.record(state, snapshot);
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); WorkbenchPersistence.save(state, out); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); state = WorkbenchPersistence.load(in); reanalyze(); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        if (level instanceof ServerLevel) state.drain().forEach(item -> net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item));
        super.preRemoveSideEffects(pos, blockState);
    }
}
