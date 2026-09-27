package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.working.GridPoint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchCoordinatesTest {
    @Test void centersAndEdgesMapDeterministically() {
        for (int x=0;x<9;x++) for (int z=0;z<9;z++) {
            var p = new GridPoint(x,z);
            assertEquals(p,WorkbenchCoordinates.grid(WorkbenchCoordinates.x(p),WorkbenchCoordinates.z(p),true).orElseThrow());
        }
        assertEquals(new GridPoint(0,0),WorkbenchCoordinates.grid(.05,.05,true).orElseThrow());
        assertEquals(new GridPoint(8,8),WorkbenchCoordinates.grid(.94999,.76999,true).orElseThrow());
        assertEquals(new GridPoint(4,4),WorkbenchCoordinates.grid(.5,.41,true).orElseThrow());
        assertEquals(0,WorkbenchCoordinates.grid(.149999,.1,true).orElseThrow().x());
        assertEquals(1,WorkbenchCoordinates.grid(.150001,.1,true).orElseThrow().x());
    }
    @Test void rejectsSidesControlsAndNonfiniteCoordinates() {
        assertTrue(WorkbenchCoordinates.grid(.5,.5,false).isEmpty());
        assertTrue(WorkbenchCoordinates.grid(-.01,.5,true).isEmpty());
        assertTrue(WorkbenchCoordinates.grid(.5,.9,true).isEmpty());
        assertTrue(WorkbenchCoordinates.grid(Double.NaN,.5,true).isEmpty());
        assertEquals(WorkbenchCoordinates.Control.ENCLOSURE,WorkbenchCoordinates.control(.1,.9,true));
        assertEquals(WorkbenchCoordinates.Control.ACTIVATE,WorkbenchCoordinates.control(.4,.9,true));
        assertEquals(WorkbenchCoordinates.Control.RECORD,WorkbenchCoordinates.control(.6,.9,true));
        assertEquals(WorkbenchCoordinates.Control.PAGE,WorkbenchCoordinates.control(.8,.9,true));
        assertEquals(WorkbenchCoordinates.Control.NONE,WorkbenchCoordinates.control(.8,.9,false));
    }
}
