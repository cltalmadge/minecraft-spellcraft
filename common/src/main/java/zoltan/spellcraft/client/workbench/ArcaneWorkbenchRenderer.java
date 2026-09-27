package zoltan.spellcraft.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import zoltan.spellcraft.workbench.*;

/** Extract immutable drawing facts and resolved item models; submit never touches the live BE. */
public final class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity, ArcaneWorkbenchRenderer.State> {
    record Placed(ItemStackRenderState item, double x, double z, float scale) {}
    record Line(double x1, double z1, double x2, double z2, int color, double width) {}
    public static final class State extends BlockEntityRenderState {
        List<Placed> items = List.of(); List<Line> lines = List.of();
    }
    private final ItemModelResolver resolver;
    public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) { resolver = context.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(ArcaneWorkbenchBlockEntity bench, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(bench, state, partialTicks, camera, breaking);
        var items = new ArrayList<Placed>(); var lines = new ArrayList<Line>(); var board = bench.state();
        for (var n : board.loci()) addItem(items, n.item(), WorkbenchCoordinates.x(n.point()), WorkbenchCoordinates.z(n.point()), .105f, bench);
        if (!board.page().isEmpty()) addItem(items, board.page(), .85, .89, .17f, bench);
        for (var r : board.relations()) {
            var a = board.locus(r.from()).orElseThrow().point(); var b = board.locus(r.to()).orElseThrow().point();
            double ax = WorkbenchCoordinates.x(a), az = WorkbenchCoordinates.z(a), bx = WorkbenchCoordinates.x(b), bz = WorkbenchCoordinates.z(b);
            double dx = bx-ax, dz = bz-az, length = Math.hypot(dx, dz), nx = -dz/length, nz = dx/length;
            // Offset reciprocal strokes so both arrows remain legible.
            ax += nx*.008; az += nz*.008; bx += nx*.008; bz += nz*.008;
            lines.add(new Line(ax, az, bx, bz, 0xffe6b45a, .005));
            double tx = ax+(bx-ax)*.68, tz = az+(bz-az)*.68;
            lines.add(new Line(tx, tz, tx-dx/length*.035+nx*.020, tz-dz/length*.035+nz*.020, 0xfff3d993, .008));
            lines.add(new Line(tx, tz, tx-dx/length*.035-nx*.020, tz-dz/length*.035-nz*.020, 0xfff3d993, .008));
        }
        if (!board.boundary().isEmpty()) {
            double minX=1, minZ=1, maxX=0, maxZ=0;
            for (long id : board.boundary()) {
                var p = board.locus(id).orElseThrow().point(); double x=WorkbenchCoordinates.x(p), z=WorkbenchCoordinates.z(p);
                minX=Math.min(minX,x); minZ=Math.min(minZ,z); maxX=Math.max(maxX,x); maxZ=Math.max(maxZ,z);
            }
            rectangle(lines,minX-.045,minZ-.035,maxX+.045,maxZ+.035,0xff70c7bd);
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                var selected = minecraft.player.getItemInHand(hand).get(DividerSelection.TYPE);
                if (selected != null && selected.pos().equals(bench.getBlockPos()) && selected.dimension().equals(bench.getLevel().dimension().identifier().toString()))
                    board.locus(selected.locus()).ifPresent(n -> ring(lines, WorkbenchCoordinates.x(n.point()), WorkbenchCoordinates.z(n.point()), 0xffffca50));
            }
            if (minecraft.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(bench.getBlockPos())) {
                var local = hit.getLocation().subtract(Vec3.atLowerCornerOf(bench.getBlockPos()));
                WorkbenchCoordinates.grid(local.x, local.z, hit.getDirection() == Direction.UP).ifPresent(p ->
                    ring(lines, WorkbenchCoordinates.x(p), WorkbenchCoordinates.z(p), board.at(p).isPresent() ? 0xffe5f5e7 : 0xff81978e));
            }
        }
        state.items = List.copyOf(items); state.lines = List.copyOf(lines);
    }
    private void addItem(List<Placed> items, ItemStack stack, double x, double z, float scale, ArcaneWorkbenchBlockEntity bench) {
        var model = new ItemStackRenderState();
        resolver.updateForTopItem(model, stack, ItemDisplayContext.FIXED, bench.getLevel(), null, (int)bench.getBlockPos().asLong());
        items.add(new Placed(model, x, z, scale));
    }
    private static void ring(List<Line> lines,double x,double z,int color) { rectangle(lines,x-.044,z-.034,x+.044,z+.034,color); }
    private static void rectangle(List<Line> lines,double x1,double z1,double x2,double z2,int color) {
        lines.add(new Line(x1,z1,x2,z1,color,.004)); lines.add(new Line(x2,z1,x2,z2,color,.004));
        lines.add(new Line(x2,z2,x1,z2,color,.004)); lines.add(new Line(x1,z2,x1,z1,color,.004));
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (var p : state.items) {
            pose.pushPose(); pose.translate(p.x(),1.065,p.z()); pose.rotateDegrees(Axis.XP,90); pose.scale(p.scale(),p.scale(),p.scale());
            p.item().submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0); pose.popPose();
        }
        var lines = state.lines;
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (transform, buffer) -> {
            for (var l : lines) {
                double dx=l.x2()-l.x1(), dz=l.z2()-l.z1(), length=Math.hypot(dx,dz);
                double nx=-dz/length*l.width()/2, nz=dx/length*l.width()/2;
                buffer.addVertex(transform,(float)(l.x1()+nx),1.012f,(float)(l.z1()+nz)).setColor(l.color());
                buffer.addVertex(transform,(float)(l.x2()+nx),1.012f,(float)(l.z2()+nz)).setColor(l.color());
                buffer.addVertex(transform,(float)(l.x2()-nx),1.012f,(float)(l.z2()-nz)).setColor(l.color());
                buffer.addVertex(transform,(float)(l.x1()-nx),1.012f,(float)(l.z1()-nz)).setColor(l.color());
            }
        });
    }
}
