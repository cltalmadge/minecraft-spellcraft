package zoltan.spellcraft.workbench;

import dev.spellcraft.domain.form.*;
import dev.spellcraft.domain.material.*;
import dev.spellcraft.domain.pattern.*;
import dev.spellcraft.domain.working.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.*;
import zoltan.spellcraft.material.MaterialProfileResolver;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchStateTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup()).forEach(c -> c.apply());
    }
    private final MaterialProfileResolver profiles = MaterialProfileResolver.bootstrap();
    private ArcaneWorkbenchState.Locus add(ArcaneWorkbenchState s, int x, int z, Item item) {
        var stack=new ItemStack(item); var expression=WorkbenchWorkingAdapter.initialExpression(profiles.resolve(stack));
        return s.place(new GridPoint(x,z),stack,expression.forms(),expression.resolved()).orElseThrow();
    }
    private WorkbenchWorkingAdapter.Snapshot analyze(ArcaneWorkbenchState s) { return WorkbenchWorkingAdapter.analyze(s,profiles); }
    @Test void stableIdsOccupancyOwnershipAndCleanup() {
        var s=new ArcaneWorkbenchState(); var a=add(s,1,4,Items.BLAZE_POWDER); var b=add(s,7,4,Items.IRON_INGOT);
        assertEquals(2,s.revision());
        assertTrue(s.place(a.point(),new ItemStack(Items.FEATHER),new FormExpression(List.of()),true).isEmpty());
        assertEquals(2,s.revision());
        assertTrue(s.connect(a.id(),b.id())); assertFalse(s.connect(a.id(),b.id()));
        assertFalse(s.connect(a.id(),a.id())); assertFalse(s.connect(a.id(),99));
        assertTrue(s.connect(b.id(),a.id()));
        assertTrue(s.enclose(List.of(a.id(),b.id())));
        a.item().shrink(1); assertEquals(1,s.locus(a.id()).orElseThrow().item().getCount());
        assertTrue(s.remove(a.id()).is(Items.BLAZE_POWDER));
        assertTrue(s.relations().isEmpty()); assertTrue(s.boundary().isEmpty());
        var c=add(s,1,4,Items.FEATHER); assertTrue(c.id()>b.id()); assertEquals(b.id(),s.at(b.point()).orElseThrow().id());
        assertFalse(s.enclose(List.of(c.id()))); assertFalse(s.enclose(List.of(c.id(),99L)));
        assertEquals(2,s.drain().size()); assertTrue(s.drain().isEmpty());
    }
    @Test void physicalMonadDyadTriadAndReversePreserveRoles() {
        var s=new ArcaneWorkbenchState(); var a=add(s,1,4,Items.BLAZE_POWDER);
        assertEquals(NumericalPrinciple.MONAD,analyze(s).analysis().pattern().orElseThrow().principle());
        assertEquals(WorkbenchFeedback.COHERENT,WorkbenchFeedback.classify(analyze(s)));
        var b=add(s,4,4,Items.IRON_INGOT);
        assertEquals(WorkbenchFeedback.INCOMPLETE,WorkbenchFeedback.classify(analyze(s)));
        s.connect(a.id(),b.id()); var pattern=analyze(s).analysis().pattern().orElseThrow();
        assertEquals(NumericalPrinciple.DYAD,pattern.principle());
        assertEquals(new MaterialId("minecraft:blaze_powder"),pattern.structure().single(LocusRole.SOURCE).material());
        assertEquals(WorkbenchFeedback.UNCONTAINED,WorkbenchFeedback.classify(analyze(s)));
        s.disconnect(a.id(),b.id());s.connect(b.id(),a.id());
        assertEquals(new MaterialId("minecraft:iron_ingot"),analyze(s).analysis().pattern().orElseThrow().structure().single(LocusRole.SOURCE).material());
        s.disconnect(b.id(),a.id());s.connect(a.id(),b.id()); var c=add(s,7,4,Items.FEATHER);s.connect(b.id(),c.id());
        assertEquals(NumericalPrinciple.TRIAD,analyze(s).analysis().pattern().orElseThrow().principle());
        assertEquals(new MaterialId("minecraft:iron_ingot"),analyze(s).analysis().pattern().orElseThrow().structure().single(LocusRole.MEDIATOR).material());
        var working=analyze(s).working().orElseThrow();
        assertEquals(new WorkingStroke(WorkbenchWorkingAdapter.nodeId(a.id()),WorkbenchWorkingAdapter.nodeId(b.id())),working.strokes().getFirst());
        assertEquals(a.point(),working.nodes().getFirst().position());
    }
    @Test void spokeTetradAndUnstableEnclosureRemainInterpretable() {
        var s=new ArcaneWorkbenchState(); var center=add(s,4,4,Items.IRON_INGOT);
        assertEquals(WorkbenchFeedback.INERT,WorkbenchFeedback.classify(analyze(s)));
        for (var p:List.of(new GridPoint(1,4),new GridPoint(7,4),new GridPoint(4,1),new GridPoint(4,7))) {
            var arm=add(s,p.x(),p.y(),Items.BLAZE_POWDER); s.connect(arm.id(),center.id());
        }
        assertEquals(NumericalPrinciple.TETRAD,analyze(s).analysis().pattern().orElseThrow().principle());
        s.enclose(s.loci().stream().map(ArcaneWorkbenchState.Locus::id).toList());
        assertTrue(analyze(s).analysis().pattern().isPresent());
        var pair=new ArcaneWorkbenchState();var a=add(pair,1,4,Items.BLAZE_POWDER);var b=add(pair,7,4,Items.IRON_INGOT);
        pair.connect(a.id(),b.id());pair.enclose(List.of(a.id(),b.id()));
        assertEquals(WorkbenchFeedback.UNSTABLE,WorkbenchFeedback.classify(analyze(pair)));
        assertTrue(analyze(pair).analysis().pattern().isPresent());
        assertEquals(2,analyze(pair).working().orElseThrow().boundaries().getFirst().enclosed().size());
        pair.connect(b.id(),a.id());assertEquals(WorkbenchFeedback.AMBIGUOUS,WorkbenchFeedback.classify(analyze(pair)));
    }
    @Test void unresolvedAndChangedProfilesNeverRewriteExpression() {
        var mixed=new MaterialProfile(new MaterialId("minecraft:blaze_powder"),List.of(new FormParticipation(Forms.HEAT,1),new FormParticipation(Forms.MOTION,.5)));
        var expression=WorkbenchWorkingAdapter.initialExpression(mixed); assertFalse(expression.resolved());assertTrue(expression.forms().terms().isEmpty());
        var s=new ArcaneWorkbenchState();s.place(new GridPoint(0,0),new ItemStack(Items.BLAZE_POWDER),expression.forms(),false);
        assertTrue(analyze(s).unresolved());assertEquals(WorkbenchFeedback.UNRESOLVED,WorkbenchFeedback.classify(analyze(s)));
        s=new ArcaneWorkbenchState();var n=add(s,0,0,Items.BLAZE_POWDER);
        var changed=new MaterialProfileResolver(Map.of());
        assertTrue(WorkbenchWorkingAdapter.analyze(s,changed).unresolved());
        assertEquals(n.expression(),s.loci().getFirst().expression());assertTrue(s.loci().getFirst().item().is(Items.BLAZE_POWDER));
    }
    @Test void persistencePreservesFactsAndRepairsBadReferencesWithoutLosingMaterials() {
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var s=new ArcaneWorkbenchState();var a=add(s,1,4,Items.BLAZE_POWDER);var b=add(s,7,4,Items.IRON_INGOT);
        s.connect(a.id(),b.id());s.enclose(List.of(a.id(),b.id()));
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registries);WorkbenchPersistence.save(s,out);var tag=out.buildResult();
        var restored=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,registries,tag));
        assertEquals(s.revision(),restored.revision());assertEquals(s.relations(),restored.relations());assertEquals(s.boundary(),restored.boundary());
        assertEquals(analyze(s),analyze(restored));assertTrue(add(restored,4,4,Items.FEATHER).id()>b.id());
        var relation=tag.getListOrEmpty("relations").getCompoundOrEmpty(0);relation.putLong("to",999);
        var badLocus=tag.getListOrEmpty("loci").getCompoundOrEmpty(1);badLocus.putInt("x",1);
        restored=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,registries,tag));
        assertEquals(1,restored.loci().size());assertTrue(restored.relations().isEmpty());assertTrue(restored.boundary().isEmpty());
        assertEquals(2,restored.drain().size());
    }
    @Test void corruptExpressionCannotBecomeAPartialValidSpell() {
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var state=new ArcaneWorkbenchState();add(state,4,4,Items.BLAZE_POWDER);
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registries);WorkbenchPersistence.save(state,out);
        var tag=out.buildResult();var expression=tag.getListOrEmpty("loci").getCompoundOrEmpty(0).getListOrEmpty("expression");
        var bad=expression.getCompoundOrEmpty(0).copy();bad.putDouble("strength",2);expression.add(bad);
        var restored=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,registries,tag));
        assertEquals(1,restored.loci().size());assertTrue(analyze(restored).unresolved());
        assertTrue(analyze(restored).analysis().pattern().isEmpty());assertTrue(restored.remove(0).is(Items.BLAZE_POWDER));
    }

}
