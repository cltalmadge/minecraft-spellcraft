package zoltan.spellcraft.material;

import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.form.Forms;
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

/** Test-only recordings for isolated runtime checks. Gameplay recordings come from the workbench. */
public final class DiscoveryFixtures {
    private DiscoveryFixtures() {}
    public static RecordedSpell directedHeat() { return directed(Items.BLAZE_POWDER, Forms.HEAT, "Thermal transmission"); }
    public static RecordedSpell directedMotion() { return directed(Items.FEATHER, Forms.MOTION, "Directed impulse"); }
    private static RecordedSpell directed(Item ingredient, FormId form, String name) {
        var resolver = MaterialProfileResolver.bootstrap();
        var material = resolver.resolve(ingredient);
        var recipient = resolver.resolve(Items.IRON_INGOT);
        var a = new NodeId("source"); var b = new NodeId("recipient");
        // Select only the named source quality; iron contributes no Form to this simple transfer.
        var working = new ArcaneWorking(List.of(new WorkingNode(a, new GridPoint(0, 0), material,
            new FormExpression(List.of(new FormParticipation(form, material.participation(form))))),
            new WorkingNode(b, new GridPoint(2, 0), recipient, new FormExpression(List.of()))), List.of(new WorkingStroke(a, b)), List.of());
        var pattern = new WorkingAnalyzer().analyze(working).pattern().orElseThrow();
        return new RecordedSpell(RecordedSpell.CURRENT_SCHEMA_VERSION, name, new SpellCompiler().compile(pattern));
    }
}
