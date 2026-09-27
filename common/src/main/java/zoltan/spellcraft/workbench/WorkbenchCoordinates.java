package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.working.GridPoint;
import java.util.Optional;

/** Fixed north-aligned drafting area. The south strip is reserved for physical controls. */
public final class WorkbenchCoordinates {
    public static final double MIN = 0.05, WIDTH = 0.90, DEPTH = 0.72;
    private WorkbenchCoordinates() {}
    public static Optional<GridPoint> grid(double x, double z, boolean top) {
        if (!top || !Double.isFinite(x) || !Double.isFinite(z) || x < MIN || x >= MIN + WIDTH || z < MIN || z >= MIN + DEPTH)
            return Optional.empty();
        return Optional.of(new GridPoint(Math.min(8, (int)((x - MIN) * 9 / WIDTH)), Math.min(8, (int)((z - MIN) * 9 / DEPTH))));
    }
    public static boolean valid(GridPoint p) { return p.x() >= 0 && p.x() < 9 && p.y() >= 0 && p.y() < 9; }
    public static double x(GridPoint p) { return MIN + (p.x() + 0.5) * WIDTH / 9; }
    public static double z(GridPoint p) { return MIN + (p.y() + 0.5) * DEPTH / 9; }
    public enum Control { NONE, ENCLOSURE, ACTIVATE, RECORD, PAGE }
    public static Control control(double x, double z, boolean top) {
        if (!top || !Double.isFinite(x) || !Double.isFinite(z) || x < 0 || x > 1 || z < 0.80 || z > 1) return Control.NONE;
        return x < .25 ? Control.ENCLOSURE : x < .50 ? Control.ACTIVATE : x < .70 ? Control.RECORD : Control.PAGE;
    }
}
