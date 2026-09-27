package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.working.GridPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcaneWorkbenchLayoutTest {
    private static final BlockPos CONTROLLER = new BlockPos(-16, 70, 15);
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    @Test void everyPartResolvesSameControllerAcrossChunkEdges() {
        for (var facing : FACINGS) {
            var front = CONTROLLER.relative(facing.getOpposite());
            assertEquals(CONTROLLER, ArcaneWorkbenchLayout.controllerPos(front, facing, WorkbenchPart.FRONT));
            assertEquals(CONTROLLER, ArcaneWorkbenchLayout.controllerPos(CONTROLLER, facing, WorkbenchPart.BACK));
            assertEquals(front, ArcaneWorkbenchLayout.counterpartPos(CONTROLLER, facing, WorkbenchPart.BACK));
            assertEquals(CONTROLLER, ArcaneWorkbenchLayout.counterpartPos(front, facing, WorkbenchPart.FRONT));
        }
    }
    @Test void physicalCentersHaveExpectedCanonicalOrientation() {
        for (var facing : FACINGS) {
            assertLocal(.5, .25, ArcaneWorkbenchLayout.local(CONTROLLER, facing, Vec3.atBottomCenterOf(CONTROLLER).add(0,1,0)));
            var front = CONTROLLER.relative(facing.getOpposite());
            assertLocal(.5, .75, ArcaneWorkbenchLayout.local(CONTROLLER, facing, Vec3.atBottomCenterOf(front).add(0,1,0)));
            var right = Vec3.atBottomCenterOf(CONTROLLER).add(facing.getClockWise().getStepX()*.25,1,facing.getClockWise().getStepZ()*.25);
            assertLocal(.75, .25, ArcaneWorkbenchLayout.local(CONTROLLER, facing, right));
        }
    }
    @Test void allGridCentersSurviveWorldRotation() {
        for (var facing : FACINGS) for (int x=0;x<9;x++) for (int z=0;z<9;z++) {
            var point = new GridPoint(x,z);
            var world = ArcaneWorkbenchLayout.world(CONTROLLER,facing,WorkbenchCoordinates.x(point),1,WorkbenchCoordinates.z(point));
            var local = ArcaneWorkbenchLayout.local(CONTROLLER,facing,world);
            assertEquals(point,WorkbenchCoordinates.grid(local.x,local.z,true).orElseThrow());
            assertTrue(ArcaneWorkbenchLayout.bounds(CONTROLLER,facing).contains(world));
        }
    }
    @Test void halfTransitionIsContinuousAndNotANewGrid() {
        for (var facing : FACINGS) for (double v : new double[]{.5-1e-7,.5,.5+1e-7}) {
            var world = ArcaneWorkbenchLayout.world(CONTROLLER,facing,.5,1,v);
            var local = ArcaneWorkbenchLayout.local(CONTROLLER,facing,world);
            assertLocal(.5,v,local);
            assertEquals(new GridPoint(4,5),WorkbenchCoordinates.grid(local.x,local.z,true).orElseThrow());
        }
    }
    @Test void normalizedEdgesRejectOutsideSurfaceAndControls() {
        for (var facing : FACINGS) for (double u : new double[]{-.01,1.01}) {
            var local=ArcaneWorkbenchLayout.local(CONTROLLER,facing,ArcaneWorkbenchLayout.world(CONTROLLER,facing,u,1,.4));
            assertTrue(WorkbenchCoordinates.grid(local.x,local.z,true).isEmpty());
            assertEquals(WorkbenchCoordinates.Control.NONE,WorkbenchCoordinates.control(local.x,.9,true));
        }
    }
    private static void assertLocal(double u,double v,Vec3 actual) {
        assertEquals(u,actual.x,1e-10);assertEquals(1,actual.y,1e-10);assertEquals(v,actual.z,1e-10);
    }
}
