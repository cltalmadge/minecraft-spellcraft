package zoltan.spellcraft.workbench;

import java.util.*;
import java.util.function.Consumer;
import dev.spellcraft.domain.pattern.LocusRole;
import dev.spellcraft.domain.material.MaterialId;
import dev.spellcraft.domain.working.GridPoint;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import zoltan.spellcraft.persistence.SpellComponents;

public final class WorkbenchGameTests {
    private static final BlockPos POS=new BlockPos(4,1,2);
    private record Experiment(GameTestHelper h, ArcaneWorkbenchBlockEntity bench, Player player, Item divider) {
        void click(double x,double z) { h.useBlock(POS,player,new BlockHitResult(Vec3.atLowerCornerOf(bench.getBlockPos()).add(x,1,z),Direction.UP,bench.getBlockPos(),false)); }
        void point(int x,int z) { var p=new GridPoint(x,z);click(WorkbenchCoordinates.x(p),WorkbenchCoordinates.z(p)); }
        void hold(ItemStack stack) { player.setItemInHand(InteractionHand.MAIN_HAND,stack); }
        void place(int x,int z,Item item) { hold(new ItemStack(item));point(x,z); }
        void connect(int ax,int az,int bx,int bz) { hold(new ItemStack(divider));point(ax,az);point(bx,bz); }
        void empty() { hold(ItemStack.EMPTY); }
    }
    private static Experiment start(GameTestHelper h,Block block) {
        h.setBlock(POS,block);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(h.absoluteVec(new Vec3(4.5,1,4.5)));
        return new Experiment(h,(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS)),player,
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("spellcraft","divider")));
    }
    private static void pair(Experiment e) { e.place(1,4,Items.BLAZE_POWDER);e.place(7,4,Items.IRON_INGOT);e.connect(1,4,7,4); }
    public static Map<String,Consumer<GameTestHelper>> tests(Block block,Item wand,Item page) {
        return Map.of(
            "workbench_to_page_to_wand_heat", h -> {
                var e=start(h,block);pair(e);
                h.assertValueEqual(e.bench.state().loci().size(),2,"placed physical materials");
                h.assertTrue(!new ItemStack(page).has(SpellComponents.RECORDED_SPELL),"pages start blank");
                h.assertTrue(!new ItemStack(wand).has(SpellComponents.RECORDED_SPELL),"wands start blank");
                e.empty();long revision=e.bench.state().revision();e.click(.4,.9);
                h.assertValueEqual(e.bench.state().revision(),revision,"activation does not mutate structure");
                h.assertValueEqual(WorkbenchFeedback.classify(e.bench.analysis()),WorkbenchFeedback.UNCONTAINED,"real diagnostics");
                e.hold(new ItemStack(page));e.click(.85,.9);h.assertTrue(e.player.getMainHandItem().isEmpty(),"page ownership transfers");
                e.empty();e.click(.6,.9);
                var spell=e.bench.state().page().get(SpellComponents.RECORDED_SPELL);h.assertTrue(spell!=null,"recorded by compiler");
                h.assertValueEqual(spell.program().structure().single(LocusRole.SOURCE).material(),new MaterialId("minecraft:blaze_powder"),"physical source preserved");
                var saved=e.bench.saveWithFullMetadata(h.getLevel().registryAccess());
                h.setBlock(POS,net.minecraft.world.level.block.Blocks.AIR);h.setBlock(POS,block);
                var restored=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
                restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
                h.assertValueEqual(restored.state().page().get(SpellComponents.RECORDED_SPELL),spell,"recording survives BE reload");
                h.assertValueEqual(restored.analysis().analysis(),e.bench.analysis().analysis(),"analysis recomputed on load");
                e=new Experiment(h,restored,e.player,e.divider);e.empty();e.click(.85,.9);
                ItemStack recorded=ItemStack.EMPTY;
                for (int i=0;i<e.player.getInventory().getContainerSize();i++) {
                    var candidate=e.player.getInventory().getItem(i);if(candidate.has(SpellComponents.RECORDED_SPELL)) recorded=candidate.copy();
                }
                h.assertTrue(!recorded.isEmpty() && e.bench.state().page().isEmpty(),"page recovered");
                e.hold(new ItemStack(wand));e.player.setItemInHand(InteractionHand.OFF_HAND,recorded);e.player.setShiftKeyDown(true);
                wand.use(h.getLevel(),e.player,InteractionHand.MAIN_HAND);
                h.assertValueEqual(e.player.getMainHandItem().get(SpellComponents.RECORDED_SPELL),spell,"same recording bound through wand interaction");
                e.player.setShiftKeyDown(false);e.player.setPos(h.absoluteVec(new Vec3(2.5,1,2.5)));e.player.setYRot(0);e.player.setXRot(0);
                var target=h.spawnWithNoFreeWill(EntityTypes.HUSK,new Vec3(2.5,1,5.5));
                wand.use(h.getLevel(),e.player,InteractionHand.MAIN_HAND);
                h.assertTrue(target.isOnFire(),"workbench-produced Heat manifests through the vessel/runtime");h.succeed();
            },
            "workbench_material_recovery_and_break", h -> {
                var e=start(h,block);pair(e);e.hold(new ItemStack(Items.FEATHER,2));e.point(1,4);
                h.assertValueEqual(e.player.getMainHandItem().getCount(),2,"occupied placement does not consume");
                e.empty();e.player.setShiftKeyDown(true);e.point(1,4);e.player.setShiftKeyDown(false);
                h.assertValueEqual(e.player.getInventory().countItem(Items.BLAZE_POWDER),1,"material recovered");
                h.assertTrue(e.bench.state().relations().isEmpty(),"dangling strokes removed");
                e.hold(new ItemStack(page));e.click(.85,.9);h.destroyBlock(POS);
                h.assertItemEntityCountIs(Items.IRON_INGOT,POS,2,1);h.assertItemEntityCountIs(page,POS,2,1);h.succeed();
            },
            "workbench_full_inventory_drops_recovery", h -> {
                var e=start(h,block);e.place(1,4,Items.BLAZE_POWDER);
                for (int i=0;i<36;i++) e.player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                e.player.setShiftKeyDown(true);
                // Empty offhand allows a recovery interaction with a completely full main inventory.
                e.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                var p=new GridPoint(1,4);e.bench.interact(e.player,InteractionHand.OFF_HAND,Direction.UP,
                    Vec3.atLowerCornerOf(e.bench.getBlockPos()).add(WorkbenchCoordinates.x(p),1,WorkbenchCoordinates.z(p)));
                h.assertTrue(e.bench.state().loci().isEmpty(),"removed with full inventory");
                h.assertItemEntityCountIs(Items.BLAZE_POWDER,new BlockPos(4,1,4),2,1);h.succeed();
            },
            "workbench_direction_enclosure_and_erasure", h -> {
                var e=start(h,block);pair(e);var state=e.bench.state();
                e.hold(new ItemStack(e.divider));e.point(1,4);e.player.setShiftKeyDown(true);e.point(7,4);e.player.setShiftKeyDown(false);
                h.assertTrue(state.relations().isEmpty(),"directed stroke erased");
                e.connect(7,4,1,4);
                h.assertValueEqual(e.bench.analysis().analysis().pattern().orElseThrow().structure().single(LocusRole.SOURCE).material(),new MaterialId("minecraft:iron_ingot"),"reverse source");
                e.click(.1,.9);h.assertValueEqual(state.boundary().size(),2,"explicit enclosure membership");
                h.assertValueEqual(WorkbenchFeedback.classify(e.bench.analysis()),WorkbenchFeedback.UNSTABLE,"interpretable unstable enclosure");
                e.hold(new ItemStack(page));e.click(.85,.9);e.empty();e.click(.6,.9);
                h.assertTrue(state.page().has(SpellComponents.RECORDED_SPELL),"unstable interpretable pattern remains recordable");
                e.hold(new ItemStack(e.divider));e.player.setShiftKeyDown(true);e.click(.1,.9);e.player.setShiftKeyDown(false);
                h.assertTrue(state.boundary().isEmpty(),"boundary erased");h.succeed();
            },
            "workbench_authority_and_selection_cleanup", h -> {
                var e=start(h,block);pair(e);long revision=e.bench.state().revision();
                e.connect(1,4,1,4);e.connect(1,4,7,4);
                h.assertValueEqual(e.bench.state().revision(),revision,"self and duplicate strokes rejected");
                e.hold(new ItemStack(e.divider));e.point(1,4);var tool=e.player.getMainHandItem();
                h.assertTrue(tool.has(DividerSelection.TYPE),"transient source selected");
                e.player.setPos(h.absoluteVec(new Vec3(20,1,20)));e.point(7,4);
                h.assertValueEqual(e.bench.state().revision(),revision,"remote mutation rejected");
                e.divider.inventoryTick(tool,h.getLevel(),e.player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                h.assertTrue(!tool.has(DividerSelection.TYPE),"walk-away cancels selection");
                e.player.setPos(h.absoluteVec(new Vec3(4.5,1,4.5)));e.point(1,4);
                var other=new BlockPos(5,1,2);h.setBlock(other,block);
                var otherBench=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(other));
                otherBench.interact(e.player,InteractionHand.MAIN_HAND,Direction.UP,Vec3.atLowerCornerOf(otherBench.getBlockPos()).add(.2,1,.4));
                h.assertTrue(otherBench.state().relations().isEmpty(),"no cross-workbench relation");
                h.destroyBlock(POS);e.divider.inventoryTick(tool,h.getLevel(),e.player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                h.assertTrue(!tool.has(DividerSelection.TYPE),"removed block cancels selection");h.succeed();
            },
            "workbench_recording_guards", h -> {
                var e=start(h,block);e.empty();e.click(.6,.9);h.assertTrue(e.bench.state().page().isEmpty(),"no page cannot record");
                e.hold(new ItemStack(page));e.click(.85,.9);e.empty();e.click(.6,.9);
                h.assertTrue(!e.bench.state().page().has(SpellComponents.RECORDED_SPELL),"empty working cannot record");
                pair(e);e.empty();e.click(.6,.9);var recording=e.bench.state().page().get(SpellComponents.RECORDED_SPELL);
                e.player.setShiftKeyDown(true);e.point(1,4);e.player.setShiftKeyDown(false);e.place(1,4,Items.FEATHER);
                e.empty();e.click(.6,.9);h.assertValueEqual(e.bench.state().page().get(SpellComponents.RECORDED_SPELL),recording,"existing page never overwritten");
                var written=e.bench.state().page();e.empty();e.click(.85,.9);e.hold(written);e.click(.85,.9);
                h.assertTrue(e.bench.state().page().isEmpty(),"recorded page insertion rejected");h.assertTrue(e.player.getMainHandItem().has(SpellComponents.RECORDED_SPELL),"rejected page stays owned");h.succeed();
            }
        );
    }
}
