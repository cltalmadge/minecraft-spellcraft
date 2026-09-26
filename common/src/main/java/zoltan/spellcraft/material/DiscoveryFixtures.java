package zoltan.spellcraft.material;

import dev.spellcraft.domain.program.RecordedSpell;
import dev.spellcraft.domain.program.SpellCompiler;
import dev.spellcraft.domain.working.ArcaneWorking;
import dev.spellcraft.domain.working.GridPoint;
import dev.spellcraft.domain.working.NodeId;
import dev.spellcraft.domain.working.WorkingAnalyzer;
import dev.spellcraft.domain.working.WorkingNode;
import dev.spellcraft.domain.working.WorkingStroke;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Bootstrap experiment, analyzed once during item registration, never during casting. */
public final class DiscoveryFixtures {
    private DiscoveryFixtures() {}
    public static RecordedSpell directedHeat() { return directed(Items.BLAZE_POWDER, "Thermal transmission"); }
    public static RecordedSpell directedMotion() { return directed(Items.FEATHER, "Directed impulse"); }
    private static RecordedSpell directed(Item ingredient, String name) {
        var material = MaterialProfileResolver.bootstrap().resolve(ingredient);
        var a = new NodeId("source"); var b = new NodeId("recipient");
        var working = new ArcaneWorking(List.of(new WorkingNode(a, new GridPoint(0, 0), material),
            new WorkingNode(b, new GridPoint(2, 0), material)), List.of(new WorkingStroke(a, b)), List.of());
        var pattern = new WorkingAnalyzer().analyze(working).pattern().orElseThrow();
        return new RecordedSpell(RecordedSpell.CURRENT_SCHEMA_VERSION, name, new SpellCompiler().compile(pattern));
    }
}
