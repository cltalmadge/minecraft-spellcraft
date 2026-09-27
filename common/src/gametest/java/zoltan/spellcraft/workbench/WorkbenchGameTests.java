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
    // These laboratory tests use Rotation.NONE. Keep clicks relative to the test origin.
    private record Experiment(GameTestHelper h, ArcaneWorkbenchBlockEntity bench, Player player, Item divider) {
        void click(double x,double z) { h.useBlock(bench.getBlockPos().subtract(h.absolutePos(BlockPos.ZERO)),player,new BlockHitResult(Vec3.atLowerCornerOf(bench.getBlockPos()).add(x,1,z),Direction.UP,bench.getBlockPos(),false)); }
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
        var tests = new LinkedHashMap<String,Consumer<GameTestHelper>>();
        tests.put("workbench_serialization_round_trip", h -> serialization(h, block, page));
        tests.put("workbench_page_recovery", h -> pageRecovery(h, block, page));
        tests.put("workbench_chunk_unload_reload", h -> chunkReload(h, block, page));
        tests.putAll(Map.of(
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
                revision=e.bench.state().revision();e.empty();e.click(.85,.9);
                h.assertValueEqual(e.bench.state().revision(),revision+1,"page retrieval changes revision once");
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
                h.assertValueEqual(e.bench.state().revision(),revision,"selection does not mutate the workstation");
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
        ));
        return tests;
    }
    private static Experiment recorded(GameTestHelper h, Block block, Item page) {
        var e = start(h, block); pair(e);
        e.hold(new ItemStack(e.divider)); e.click(.1,.9);
        long revision=e.bench.state().revision();
        e.hold(new ItemStack(page)); e.click(.85,.9);
        h.assertValueEqual(e.bench.state().revision(),revision+1,"page insertion changes revision once");
        e.empty(); e.click(.6,.9);
        h.assertValueEqual(e.bench.state().revision(),revision+2,"recording changes revision once");
        h.assertTrue(e.bench.state().page().has(SpellComponents.RECORDED_SPELL), "fixture recording succeeded");
        return e;
    }
    private static void assertState(GameTestHelper h, ArcaneWorkbenchState expected, ArcaneWorkbenchState actual) {
        h.assertValueEqual(actual.loci().size(), expected.loci().size(), "locus count");
        for (int i=0; i<expected.loci().size(); i++) {
            var a=expected.loci().get(i); var b=actual.loci().get(i);
            h.assertValueEqual(b.id(), a.id(), "stable ID");
            h.assertValueEqual(b.point(), a.point(), "grid position");
            h.assertTrue(ItemStack.matches(a.item(), b.item()), "material stack and components");
            h.assertValueEqual(b.expression(), a.expression(), "expressed Forms");
            h.assertValueEqual(b.resolved(), a.resolved(), "resolution state");
        }
        h.assertValueEqual(actual.relations(), expected.relations(), "directed relations");
        h.assertValueEqual(actual.boundary(), expected.boundary(), "boundary membership");
        h.assertTrue(ItemStack.matches(actual.page(), expected.page()), "page and RecordedSpell component");
        h.assertValueEqual(actual.nextId(), expected.nextId(), "next ID including removed loci");
        h.assertValueEqual(actual.revision(), expected.revision(), "saved revision");
        h.assertValueEqual(actual.recovery().size(), expected.recovery().size(), "recovery count");
        for (int i=0; i<expected.recovery().size(); i++)
            h.assertTrue(ItemStack.matches(actual.recovery().get(i), expected.recovery().get(i)), "recovery stack");
    }
    private static void serialization(GameTestHelper h, Block block, Item page) {
        var e=recorded(h,block,page); var state=e.bench.state();
        // Direct fixture construction is confined to this same-package persistence test.
        var material=new ItemStack(Items.FEATHER);
        material.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Persistence specimen"));
        state.place(new GridPoint(4,1), material, new dev.spellcraft.domain.form.FormExpression(List.of()), false);
        var removed=state.place(new GridPoint(4,7), new ItemStack(Items.IRON_INGOT), new dev.spellcraft.domain.form.FormExpression(List.of()), true).orElseThrow();
        state.remove(removed.id());
        state.enclose(state.loci().stream().map(ArcaneWorkbenchState.Locus::id).toList());
        state.recover(new ItemStack(Items.DIAMOND,3)); e.bench.changed();
        var saved=e.bench.saveWithFullMetadata(h.getLevel().registryAccess());
        // Detached BE: no placed block is destroyed, no items are dropped or reintroduced.
        var decoded=new ArcaneWorkbenchBlockEntity(e.bench.getBlockPos(), block.defaultBlockState());
        decoded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        assertState(h,state,decoded.state());
        h.assertValueEqual(decoded.analysis(),e.bench.analysis(),"analysis recomputed from restored physical facts");
        h.assertTrue(decoded.analysis().unresolved(),"unresolved locus stays unresolved");
        h.succeed();
    }
    private static void pageRecovery(GameTestHelper h, Block block, Item page) {
        var e=recorded(h,block,page);
        var saved=e.bench.saveWithFullMetadata(h.getLevel().registryAccess());
        for (int count : List.of(1,2,100)) {
            var tag=saved.copy(); tag.getCompoundOrEmpty("page").putInt("count",count);
            var loaded=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
            h.assertValueEqual(loaded.revision(),e.bench.state().revision(),"repair preserves saved revision");
            if (count == 1) h.assertTrue(ItemStack.matches(loaded.page(),e.bench.state().page()),"valid recorded page restored");
            else {
                h.assertTrue(loaded.page().isEmpty(),"multiple pages leave receptacle empty");
                h.assertValueEqual(loaded.recovery().getFirst().getCount(),count,"entire corrupt count preserved");
                h.assertValueEqual(loaded.recovery().getFirst().get(SpellComponents.RECORDED_SPELL),e.bench.state().page().get(SpellComponents.RECORDED_SPELL),"recovered recording preserved");
                var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
                WorkbenchPersistence.save(loaded,out);
                assertState(h,loaded,WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult())));
            }
        }
        var malformed=saved.copy();
        var expression=malformed.getCompoundOrEmpty("page").getCompoundOrEmpty("components")
            .getCompoundOrEmpty("spellcraft:recorded_spell").getCompoundOrEmpty("program")
            .getCompoundOrEmpty("structure").getListOrEmpty("loci").getCompoundOrEmpty(0).getListOrEmpty("expressed_forms");
        h.assertTrue(!expression.isEmpty(),"corruption targets a real saved magical expression");
        var bad=expression.getCompoundOrEmpty(0).copy();bad.putDouble("strength",2);expression.add(bad);
        var rejected=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),malformed));
        h.assertTrue(rejected.page().isEmpty(),"partial recorded expression cannot become a usable page");
        h.assertTrue(rejected.recovery().stream().noneMatch(i -> i.has(SpellComponents.RECORDED_SPELL)),"partial recording cannot escape through recovery");
        var invalidComponents=saved.copy();
        invalidComponents.getCompoundOrEmpty("page").getCompoundOrEmpty("components").putInt("minecraft:max_damage",10);
        invalidComponents.getCompoundOrEmpty("page").getCompoundOrEmpty("components").putInt("minecraft:max_stack_size",64);
        rejected=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),invalidComponents));
        h.assertTrue(rejected.page().isEmpty(),"incompatible components rejected");
        h.assertValueEqual(rejected.recovery().size(),1,"decoded incompatible components preserved in recovery");
        var blank=saved.copy(); blank.getCompoundOrEmpty("page").remove("components");
        var loaded=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),blank));
        h.assertTrue(loaded.page().is(page) && !loaded.page().has(SpellComponents.RECORDED_SPELL),"blank page restored");
        var invalid=saved.copy(); invalid.getCompoundOrEmpty("page").putString("id","minecraft:diamond");
        loaded=WorkbenchPersistence.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),invalid));
        h.assertTrue(loaded.page().isEmpty(),"wrong item rejected");
        h.assertTrue(loaded.recovery().getFirst().is(Items.DIAMOND),"wrong item preserved");
        // Put the repaired contents through actual block removal, including the recovery tray.
        e.bench.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),invalid));
        h.destroyBlock(POS);
        h.assertItemEntityCountIs(Items.DIAMOND,POS,2,1);
        h.assertItemEntityCountIs(Items.BLAZE_POWDER,POS,2,1);
        h.assertItemEntityCountIs(Items.IRON_INGOT,POS,2,1);
        h.succeed();
    }
    private static void chunkReload(GameTestHelper h, Block block, Item page) {
        // GameTest structure chunks are held loaded. Use an isolated chunk outside that ticket radius.
        var pos=h.absolutePos(POS).offset(1024,0,1024);
        var chunks=h.getLevel().getChunkSource();
        var chunkPos=new net.minecraft.world.level.ChunkPos(pos.getX() >> 4,pos.getZ() >> 4);
        chunks.getChunk(chunkPos.x(),chunkPos.z(),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        h.getLevel().setBlock(pos,block.defaultBlockState(),Block.UPDATE_ALL);
        var player=h.makeMockPlayer(GameType.SURVIVAL); player.setPos(Vec3.atLowerCornerOf(pos).add(.5,0,2.5));
        var original=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);
        var e=new Experiment(h,original,player,BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("spellcraft","divider")));
        pair(e); e.hold(new ItemStack(e.divider));e.click(.1,.9);
        e.hold(new ItemStack(page));e.click(.85,.9);e.empty();e.click(.6,.9);
        h.assertTrue(original.state().page().has(SpellComponents.RECORDED_SPELL),"live remote bench recorded");
        var originalChunk=h.getLevel().getChunkAt(pos);
        var expected=original.state(); var analysis=original.analysis();
        // Flush through the real chunk map and region storage. The temporary UNKNOWN ticket expires naturally.
        chunks.save(true);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(original.isRemoved(),"normal unload retired the original block entity");
            h.assertTrue(!chunks.hasChunk(chunkPos.x(),chunkPos.z()),"chunk is no longer loaded");
        }).thenExecute(() -> {
            // Entity storage loads separately from block data; hold a temporary loading ticket.
            chunks.addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunkPos,0);
            h.getLevel().getChunkAt(pos);
        }).thenWaitUntil(() -> {
            h.assertTrue(h.getLevel().areEntitiesLoaded(chunkPos.pack()),"reloaded chunk and entity storage ready");
        }).thenExecute(() -> {
            var restoredChunk=h.getLevel().getChunkAt(pos);
            var restored=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(restoredChunk != originalChunk,"new chunk instance read from storage");
            h.assertTrue(restored != null && restored != original,"normal restoration constructed a new BE");
            h.assertTrue(!restored.session().equals(original.session()),"new loaded session");
            assertState(h,expected,restored.state());
            h.assertValueEqual(original.state().loci().size(),2,"unload leaves original materials undrained");
            h.assertTrue(original.state().page().has(SpellComponents.RECORDED_SPELL),"unload leaves original page undrained");
            h.assertValueEqual(restored.analysis(),analysis,"load recomputes analysis");
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(3));
            h.assertTrue(drops.isEmpty(),"unloading never drops stored materials or page");
            // Remove the remote test fixture after assertions; normal removal should drop exactly once.
            h.getLevel().destroyBlock(pos,false);
            var removedDrops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(3));
            h.assertValueEqual(removedDrops.stream().mapToInt(d -> d.getItem().getCount()).sum(),3,"materials and recorded page dropped exactly once");
            removedDrops.forEach(net.minecraft.world.entity.Entity::discard);
            chunks.removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunkPos,0);
        }).thenSucceed();
    }

}
