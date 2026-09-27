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
        void click(double x,double z) {
            var world=ArcaneWorkbenchLayout.world(bench.getBlockPos(),bench.getBlockState().getValue(ArcaneWorkbenchBlock.FACING),x,1,z);
            var clicked=BlockPos.containing(world.x,world.y-.01,world.z);
            h.useBlock(clicked.subtract(h.absolutePos(BlockPos.ZERO)),player,new BlockHitResult(world,Direction.UP,clicked,false));
        }
        void point(int x,int z) { var p=new GridPoint(x,z);click(WorkbenchCoordinates.x(p),WorkbenchCoordinates.z(p)); }
        void hold(ItemStack stack) { player.setItemInHand(InteractionHand.MAIN_HAND,stack); }
        void place(int x,int z,Item item) { hold(new ItemStack(item));point(x,z); }
        void connect(int ax,int az,int bx,int bz) { hold(new ItemStack(divider));point(ax,az);point(bx,bz); }
        void empty() { hold(ItemStack.EMPTY); }
    }
    private static Experiment start(GameTestHelper h,Block block) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(h.absoluteVec(new Vec3(4.5,1,4.5)));
        placeWorkbench(h, block, player, h.absolutePos(POS), Direction.NORTH);
        return new Experiment(h,(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS)),player,
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("spellcraft","divider")));
    }
    private static void placeWorkbench(GameTestHelper h, Block block, Player player, BlockPos controller, Direction facing) {
        var front=ArcaneWorkbenchLayout.counterpartPos(controller,facing,WorkbenchPart.BACK);
        player.setYRot(facing.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));
        var hit=new BlockHitResult(Vec3.atBottomCenterOf(front),Direction.UP,front,false);
        var result=((BlockItem)block.asItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),hit));
        h.assertTrue(result.consumesAction(),"one item places workstation");
        h.assertTrue(player.getMainHandItem().isEmpty(),"one item consumed");
        h.assertTrue(ArcaneWorkbenchLayout.validPair(h.getLevel(),controller,h.getLevel().getBlockState(controller)),"complete pair placed");
        h.assertTrue(h.getLevel().getBlockEntity(front)==null,"front has no block entity");
    }
    private static void pair(Experiment e) { e.place(1,6,Items.BLAZE_POWDER);e.place(7,2,Items.IRON_INGOT);e.connect(1,6,7,2); }
    public static Map<String,Consumer<GameTestHelper>> tests(Block block,Item wand,Item page) {
        var tests = new LinkedHashMap<String,Consumer<GameTestHelper>>();
        for (var facing : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            tests.put("workbench_orientation_"+facing.getSerializedName(), h -> orientation(h,block,facing));
        }
        for (var part : WorkbenchPart.values()) {
            tests.put("workbench_break_"+part.getSerializedName(), h -> dismantle(h,block,page,part,false));
            tests.put("workbench_creative_break_"+part.getSerializedName(), h -> dismantle(h,block,page,part,true));
        }
        tests.put("workbench_blocked_placement", h -> blockedPlacement(h,block));
        tests.put("workbench_world_border_placement", h -> {
            var border=h.getLevel().getWorldBorder();double oldX=border.getCenterX(),oldZ=border.getCenterZ(),oldSize=border.getSize();
            var front=h.absolutePos(POS.south());
            var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(Vec3.atBottomCenterOf(front).add(0,0,1.5));player.setYRot(180);
            try {
                border.setCenter(front.getX()+.5,front.getZ()+.5);border.setSize(1.5);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));
                var context=new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),new BlockHitResult(Vec3.atBottomCenterOf(front),Direction.UP,front,false));
                h.assertTrue(!((BlockItem)block.asItem()).place(context).consumesAction(),"second position outside border rejects placement");
                h.assertValueEqual(player.getMainHandItem().getCount(),1,"border failure preserves item");
                h.assertTrue(h.getLevel().getBlockState(front).isAir() && h.getLevel().getBlockState(h.absolutePos(POS)).isAir(),"border failure leaves neither half");
            } finally { border.setCenter(oldX,oldZ);border.setSize(oldSize); }
            h.succeed();
        });
        tests.put("workbench_replaceable_placement", h -> {
            h.setBlock(POS,net.minecraft.world.level.block.Blocks.SHORT_GRASS);
            var e=start(h,block);h.assertTrue(e.bench!=null,"vegetation replaced by controller");h.succeed();
        });
        for (var explodedPart : WorkbenchPart.values()) tests.put("workbench_explosion_"+explodedPart.getSerializedName(), h -> {
            var e=start(h,block);pair(e);e.hold(new ItemStack(page));e.click(.85,.9);
            // Use the real explosion block callback, without blast damage destroying dropped items.
            var explosion=new net.minecraft.world.level.ServerExplosion(h.getLevel(),null,null,null,
                Vec3.atCenterOf(e.bench.getBlockPos()),4,false,net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
            var captured=new ArrayList<ItemStack>();
            var front=ArcaneWorkbenchLayout.counterpartPos(e.bench.getBlockPos(),e.bench.getBlockState());
            var first=explodedPart==WorkbenchPart.FRONT ? front : e.bench.getBlockPos();
            var second=explodedPart==WorkbenchPart.FRONT ? e.bench.getBlockPos() : front;
            h.getLevel().getBlockState(first).onExplosionHit(h.getLevel(),first,explosion,(stack,pos)->captured.add(stack));
            h.getLevel().getBlockState(second).onExplosionHit(h.getLevel(),second,explosion,(stack,pos)->captured.add(stack));
            int normalLoot=explodedPart==WorkbenchPart.FRONT ? 1 : 0;
            h.assertValueEqual(captured.stream().filter(stack->stack.is(block.asItem())).mapToInt(ItemStack::getCount).sum(),1-normalLoot,"one item across explosion and counterpart removal");
            assertDismantled(h,e,block,page,normalLoot);h.succeed();
        });
        tests.put("workbench_invalid_pair", h -> {
            var e=start(h,block);pair(e);
            var front=ArcaneWorkbenchLayout.counterpartPos(e.bench.getBlockPos(),e.bench.getBlockState());
            h.getLevel().setBlock(front,h.getLevel().getBlockState(front).setValue(ArcaneWorkbenchBlock.FACING,Direction.EAST),Block.UPDATE_ALL);
            long revision=e.bench.state().revision();e.place(4,4,Items.DIAMOND);
            h.assertValueEqual(e.bench.state().revision(),revision,"invalid pair rejects mutation");
            h.startSequence().thenExecuteAfter(22,()-> {
                h.assertTrue(h.getLevel().getBlockState(front).isAir() && h.getLevel().getBlockState(e.bench.getBlockPos()).isAir(),"mismatched halves removed");
                h.assertItemEntityCountIs(block.asItem(),POS,3,1);
            }).thenSucceed();
        });
        tests.put("workbench_serialization_round_trip", h -> serialization(h, block, page));
        tests.put("workbench_page_recovery", h -> pageRecovery(h, block, page));
        tests.put("workbench_chunk_unload_reload", h -> chunkReload(h, block, page, false));
        tests.put("workbench_chunk_front_first_reload", h -> chunkReload(h, block, page, true));
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
                var e=start(h,block);pair(e);e.hold(new ItemStack(Items.FEATHER,2));e.point(1,6);
                h.assertValueEqual(e.player.getMainHandItem().getCount(),2,"occupied placement does not consume");
                e.empty();e.player.setShiftKeyDown(true);e.point(1,6);e.player.setShiftKeyDown(false);
                h.assertValueEqual(e.player.getInventory().countItem(Items.BLAZE_POWDER),1,"material recovered");
                h.assertTrue(e.bench.state().relations().isEmpty(),"dangling strokes removed");
                e.hold(new ItemStack(page));e.click(.85,.9);h.destroyBlock(POS);
                h.assertItemEntityCountIs(Items.IRON_INGOT,POS,2,1);h.assertItemEntityCountIs(page,POS,2,1);h.succeed();
            },
            "workbench_full_inventory_drops_recovery", h -> {
                var e=start(h,block);e.place(1,6,Items.BLAZE_POWDER);
                for (int i=0;i<36;i++) e.player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                e.player.setShiftKeyDown(true);
                // Empty offhand allows a recovery interaction with a completely full main inventory.
                e.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                var p=new GridPoint(1,6);e.bench.interact(e.player,InteractionHand.OFF_HAND,Direction.UP,
                    ArcaneWorkbenchLayout.world(e.bench.getBlockPos(),e.bench.getBlockState().getValue(ArcaneWorkbenchBlock.FACING),WorkbenchCoordinates.x(p),1,WorkbenchCoordinates.z(p)));
                h.assertTrue(e.bench.state().loci().isEmpty(),"removed with full inventory");
                h.assertItemEntityCountIs(Items.BLAZE_POWDER,new BlockPos(4,1,4),2,1);h.succeed();
            },
            "workbench_direction_enclosure_and_erasure", h -> {
                var e=start(h,block);pair(e);var state=e.bench.state();
                e.hold(new ItemStack(e.divider));e.point(1,6);e.player.setShiftKeyDown(true);e.point(7,2);e.player.setShiftKeyDown(false);
                h.assertTrue(state.relations().isEmpty(),"directed stroke erased");
                e.connect(7,2,1,6);
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
                e.connect(1,6,1,6);e.connect(1,6,7,2);
                h.assertValueEqual(e.bench.state().revision(),revision,"self and duplicate strokes rejected");
                e.hold(new ItemStack(e.divider));e.point(1,6);var tool=e.player.getMainHandItem();
                h.assertTrue(tool.has(DividerSelection.TYPE),"transient source selected");
                h.assertValueEqual(e.bench.state().revision(),revision,"selection does not mutate the workstation");
                e.player.setPos(h.absoluteVec(new Vec3(20,1,20)));e.point(7,2);
                h.assertValueEqual(e.bench.state().revision(),revision,"remote mutation rejected");
                e.divider.inventoryTick(tool,h.getLevel(),e.player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                h.assertTrue(!tool.has(DividerSelection.TYPE),"walk-away cancels selection");
                e.player.setPos(h.absoluteVec(new Vec3(4.5,1,4.5)));e.point(1,6);
                var other=new BlockPos(5,1,2);placeWorkbench(h,block,e.player,h.absolutePos(other),Direction.NORTH);
                e.hold(tool);
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
                e.player.setShiftKeyDown(true);e.point(1,6);e.player.setShiftKeyDown(false);e.place(1,6,Items.FEATHER);
                e.empty();e.click(.6,.9);h.assertValueEqual(e.bench.state().page().get(SpellComponents.RECORDED_SPELL),recording,"existing page never overwritten");
                var written=e.bench.state().page();e.empty();e.click(.85,.9);e.hold(written);e.click(.85,.9);
                h.assertTrue(e.bench.state().page().isEmpty(),"recorded page insertion rejected");h.assertTrue(e.player.getMainHandItem().has(SpellComponents.RECORDED_SPELL),"rejected page stays owned");h.succeed();
            }
        ));
        return tests;
    }
    private static void orientation(GameTestHelper h,Block block,Direction facing) {
        var controller=h.absolutePos(new BlockPos(3,1,3));
        var front=ArcaneWorkbenchLayout.counterpartPos(controller,facing,WorkbenchPart.BACK);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(ArcaneWorkbenchLayout.world(controller,facing,.5,0,1.3));
        placeWorkbench(h,block,player,controller,facing);
        h.assertValueEqual(h.getLevel().getBlockState(front).getValue(ArcaneWorkbenchBlock.FACING),facing,"front facing");
        h.assertValueEqual(h.getLevel().getBlockState(controller).getValue(ArcaneWorkbenchBlock.FACING),facing,"back facing");
        var e=new Experiment(h,(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(controller),player,
            BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("spellcraft","divider")));
        pair(e);
        h.assertTrue(e.bench.state().at(new GridPoint(1,6)).isPresent(),"front physical hit maps canonical source");
        h.assertTrue(e.bench.state().at(new GridPoint(7,2)).isPresent(),"back physical hit maps canonical recipient");
        h.assertValueEqual(e.bench.state().relations().size(),1,"cross-half Divider relationship");
        var expected=new ArcaneWorkbenchState();
        var blaze=WorkbenchWorkingAdapter.initialExpression(zoltan.spellcraft.material.MaterialProfileResolver.bootstrap().resolve(new ItemStack(Items.BLAZE_POWDER)));
        var iron=WorkbenchWorkingAdapter.initialExpression(zoltan.spellcraft.material.MaterialProfileResolver.bootstrap().resolve(new ItemStack(Items.IRON_INGOT)));
        var a=expected.place(new GridPoint(1,6),new ItemStack(Items.BLAZE_POWDER),blaze.forms(),blaze.resolved()).orElseThrow();
        var b=expected.place(new GridPoint(7,2),new ItemStack(Items.IRON_INGOT),iron.forms(),iron.resolved()).orElseThrow();expected.connect(a.id(),b.id());
        h.assertValueEqual(e.bench.analysis(),WorkbenchWorkingAdapter.analyze(expected,zoltan.spellcraft.material.MaterialProfileResolver.bootstrap()),"rotation preserves complete semantic analysis");
        e.hold(new ItemStack(e.divider));e.point(1,6);player.setShiftKeyDown(true);e.point(7,2);player.setShiftKeyDown(false);
        e.connect(7,2,1,6);
        h.assertValueEqual(e.bench.analysis().analysis().pattern().orElseThrow().structure().single(LocusRole.SOURCE).material(),new MaterialId("minecraft:iron_ingot"),"reverse relationship across halves");
        e.empty();player.setShiftKeyDown(true);e.point(1,6);e.empty();e.point(7,2);
        h.assertTrue(e.bench.state().loci().isEmpty(),"recover through both physical parts");h.succeed();
    }
    private static void blockedPlacement(GameTestHelper h,Block block) {
        var front=h.absolutePos(POS.south());var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(4.5,1,4.5)));player.setYRot(Direction.NORTH.toYRot());
        for (var obstacle : new Block[]{net.minecraft.world.level.block.Blocks.STONE,net.minecraft.world.level.block.Blocks.GLASS,block}) {
            h.setBlock(POS,obstacle);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block,2));
            var context=new net.minecraft.world.item.context.BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),new BlockHitResult(Vec3.atBottomCenterOf(front),Direction.UP,front,false));
            h.assertTrue(!((BlockItem)block.asItem()).place(context).consumesAction(),"blocked second position rejects item placement");
            h.assertValueEqual(player.getMainHandItem().getCount(),2,"failed placement preserves item");
            h.assertTrue(h.getLevel().getBlockState(front).isAir() && h.getLevel().getBlockEntity(front)==null,"no partial front/BE");
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(POS)).is(obstacle),"obstacle retained");
        }
        h.succeed();
    }
    private static void dismantle(GameTestHelper h,Block block,Item page,WorkbenchPart part,boolean creative) {
        var e=start(h,block);pair(e);e.hold(new ItemStack(page));e.click(.85,.9);
        e.bench.state().recover(new ItemStack(Items.DIAMOND,3));e.bench.changed();
        var target=part==WorkbenchPart.BACK ? e.bench.getBlockPos() : ArcaneWorkbenchLayout.counterpartPos(e.bench.getBlockPos(),e.bench.getBlockState());
        var breaker=h.makeMockPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        (creative ? GameType.CREATIVE : GameType.SURVIVAL).updatePlayerAbilities(breaker.getAbilities());
        var state=h.getLevel().getBlockState(target);
        state.getBlock().playerWillDestroy(h.getLevel(),target,state,breaker);
        h.getLevel().destroyBlock(target,!creative);
        assertDismantled(h,e,block,page,creative ? 0 : 1);
        h.assertItemEntityCountIs(Items.DIAMOND,POS,3,3);
        h.assertTrue(e.bench.state().recovery().isEmpty(),"recovery drained once");h.succeed();
    }
    private static void assertDismantled(GameTestHelper h,Experiment e,Block block,Item page,int benches) {
        var front=ArcaneWorkbenchLayout.counterpartPos(e.bench.getBlockPos(),e.bench.getBlockState());
        for (var pos : List.of(e.bench.getBlockPos(),front)) {
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"both parts removed");
            h.assertTrue(h.getLevel().getBlockEntity(pos)==null,"no remaining BE");
        }
        h.assertItemEntityCountIs(block.asItem(),POS,3,benches);
        h.assertItemEntityCountIs(Items.BLAZE_POWDER,POS,3,1);
        h.assertItemEntityCountIs(Items.IRON_INGOT,POS,3,1);
        h.assertItemEntityCountIs(page,POS,3,1);
        h.assertTrue(e.bench.state().loci().isEmpty() && e.bench.state().page().isEmpty(),"controller drained once");
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
    private static void chunkReload(GameTestHelper h, Block block, Item page, boolean frontFirst) {
        // GameTest structure chunks are held loaded. Use an isolated chunk outside that ticket radius.
        int distance=frontFirst ? 2048 : 1024;
        var remote=h.absolutePos(POS).offset(distance,0,distance);
        var pos=new BlockPos(remote.getX(),remote.getY(),(remote.getZ() & ~15)+15);
        var front=ArcaneWorkbenchLayout.counterpartPos(pos,Direction.NORTH,WorkbenchPart.BACK);
        var frontChunkPos=new net.minecraft.world.level.ChunkPos(front.getX() >> 4,front.getZ() >> 4);
        var chunks=h.getLevel().getChunkSource();
        var chunkPos=new net.minecraft.world.level.ChunkPos(pos.getX() >> 4,pos.getZ() >> 4);
        chunks.getChunk(chunkPos.x(),chunkPos.z(),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        chunks.getChunk(frontChunkPos.x(),frontChunkPos.z(),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        var player=h.makeMockPlayer(GameType.SURVIVAL); player.setPos(Vec3.atLowerCornerOf(pos).add(.5,0,2.5));
        placeWorkbench(h,block,player,pos,Direction.NORTH);
        var original=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);
        var e=new Experiment(h,original,player,BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("spellcraft","divider")));
        pair(e); e.hold(new ItemStack(e.divider));e.click(.1,.9);
        e.hold(new ItemStack(page));e.click(.85,.9);e.empty();e.click(.6,.9);
        h.assertTrue(original.state().page().has(SpellComponents.RECORDED_SPELL),"live remote bench recorded");
        var originalChunk=h.getLevel().getChunkAt(pos);
        original.state().recover(new ItemStack(Items.DIAMOND,3));original.changed();
        var expected=original.state(); var analysis=original.analysis();
        // Flush through the real chunk map and region storage. The temporary UNKNOWN ticket expires naturally.
        chunks.save(true);
        h.startSequence().thenWaitUntil(() -> {
            h.assertTrue(original.isRemoved(),"normal unload retired the original block entity");
            h.assertTrue(!chunks.hasChunk(chunkPos.x(),chunkPos.z()),"controller chunk is no longer loaded");
            h.assertTrue(!chunks.hasChunk(frontChunkPos.x(),frontChunkPos.z()),"front chunk is no longer loaded");
        }).thenExecute(() -> {
            // Load one half first through the real region/BE deserializer, in both orders.
            var first=frontFirst ? front : pos;var absent=frontFirst ? chunkPos : frontChunkPos;
            h.getLevel().getChunkAt(first);
            h.assertTrue(!chunks.hasChunk(absent.x(),absent.z()),"counterpart remains absent on first-half load");
            var restored=h.getLevel().getBlockState(first);
            ((ArcaneWorkbenchBlock)block).tick(restored,h.getLevel(),first,h.getLevel().getRandom());
            h.assertTrue(h.getLevel().getBlockState(first).is(block),"unloaded counterpart is not mistaken for destruction");
            h.assertTrue(!chunks.hasChunk(absent.x(),absent.z()),"validation never force-loads counterpart");
            chunks.save(true);
            h.assertTrue(!chunks.hasChunk(absent.x(),absent.z()),"saving one half does not load the other");
            // Entity storage loads separately; retain temporary tickets while verifying both halves.
            chunks.addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunkPos,0);
            chunks.addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,frontChunkPos,0);
            h.getLevel().getChunkAt(front);h.getLevel().getChunkAt(pos);
        }).thenWaitUntil(() -> {
            h.assertTrue(h.getLevel().areEntitiesLoaded(chunkPos.pack()) && h.getLevel().areEntitiesLoaded(frontChunkPos.pack()),"both reloaded chunks and entity storage ready");
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
            h.assertTrue(ArcaneWorkbenchLayout.resolve(h.getLevel(),ArcaneWorkbenchLayout.counterpartPos(pos,restored.getBlockState()))==restored,"front resolves restored controller");
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(3));
            h.assertTrue(drops.isEmpty(),"unloading never drops stored materials or page");
            // Remove the remote test fixture after assertions; normal removal should drop exactly once.
            h.getLevel().destroyBlock(pos,false);
            var removedDrops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(3));
            h.assertValueEqual(removedDrops.stream().mapToInt(d -> d.getItem().getCount()).sum(),6,"materials, recovery and recorded page dropped exactly once");
            removedDrops.forEach(net.minecraft.world.entity.Entity::discard);
            chunks.removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,chunkPos,0);
            chunks.removeTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,frontChunkPos,0);
        }).thenSucceed();
    }

}
