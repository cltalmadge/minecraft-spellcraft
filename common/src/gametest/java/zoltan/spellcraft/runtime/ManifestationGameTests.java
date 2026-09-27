package zoltan.spellcraft.runtime;

import java.util.*;
import java.util.function.Consumer;
import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.form.Forms;
import dev.spellcraft.domain.manifestation.SpellBurden;
import dev.spellcraft.domain.material.MaterialId;
import dev.spellcraft.domain.material.MaterialProfile;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.LocusId;
import dev.spellcraft.domain.pattern.LocusRelation;
import dev.spellcraft.domain.pattern.LocusRole;
import dev.spellcraft.domain.pattern.NumericalPrinciple;
import dev.spellcraft.domain.pattern.SpellLocus;
import dev.spellcraft.domain.pattern.SpellPattern;
import dev.spellcraft.domain.pattern.SpellStructure;
import dev.spellcraft.domain.program.SpellCompiler;
import dev.spellcraft.domain.program.SpellProgram;
import dev.spellcraft.domain.vessel.VesselProfile;
import dev.spellcraft.domain.working.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import zoltan.spellcraft.material.DiscoveryFixtures;
import zoltan.spellcraft.persistence.SpellComponents;

public final class ManifestationGameTests {
    public static Map<String, Consumer<GameTestHelper>> tests(Item wand, Item page) {
        return Map.of(
            "heat_from_recorded_item", h -> cast(h, wand, false, false),
            "motion_from_same_item", h -> cast(h, wand, true, false),
            "wall_blocks_transmission", h -> cast(h, wand, false, true),
            "page_preserves_without_manifesting", h -> {
                var stack = new ItemStack(page);
                stack.set(SpellComponents.RECORDED_SPELL, DiscoveryFixtures.directedHeat());
                h.assertValueEqual(stack.get(SpellComponents.RECORDED_SPELL), DiscoveryFixtures.directedHeat(), "portable recording");
                var ops = h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
                var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
                h.assertValueEqual(restored.get(SpellComponents.RECORDED_SPELL), stack.get(SpellComponents.RECORDED_SPELL), "saved item recording");
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                page.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
                h.assertTrue(!player.isOnFire(), "A page must not provide manifestation power");
                h.succeed();
            },
            "radial_heat_reaches_multiple_recipients", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(h.absoluteVec(new Vec3(5, 1, 5)));
                var first = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(3, 1, 5));
                var second = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(7, 1, 5));
                var pattern = centered(new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1))),
                    NumericalPrinciple.MONAD, new Geometry.Radial(true));
                var runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler()));
                var result = runtime.execute(new SpellCompiler().compile(pattern), new MinecraftSpellContext(h.getLevel(), player));
                h.assertValueEqual(result.status(), ManifestationResult.Status.APPLIED, "radial release");
                h.assertTrue(first.isOnFire() && second.isOnFire(), "Radial geometry distributes Heat");
                h.assertTrue(!player.isOnFire(), "Radial emanation excludes its origin");
                h.succeed();
            },
            "unsupported_semantics_have_no_partial_effect", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                var runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler()));
                var forms = new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1), new FormParticipation(new FormId("test:unknown"), 1)));
                var unknown = new SpellCompiler().compile(centered(forms, NumericalPrinciple.MONAD, new Geometry.Point()));
                var context = new MinecraftSpellContext(h.getLevel(), player);
                h.assertValueEqual(runtime.execute(unknown, context).status(), ManifestationResult.Status.UNSUPPORTED_FORM, "unknown Form");
                h.assertTrue(!player.isOnFire(), "Known forms must not execute before rejecting unknown ones");
                var fixed = new SpellCompiler().compile(centered(new FormExpression(List.of(new FormParticipation(new FormId("test:unknown"), 1))),
                    NumericalPrinciple.TETRAD, new Geometry.Enclosure(new Geometry.Intersection())));
                h.assertValueEqual(runtime.execute(fixed, context).status(), ManifestationResult.Status.UNSUPPORTED_OPERATION, "sustained structure");
                h.assertTrue(!player.isOnFire(), "Sustain must not become a one-shot cast");
                var dyad = directed(false, Forms.MOTION);
                var enclosed = new SpellCompiler().compile(new SpellPattern(dyad.structure(), NumericalPrinciple.DYAD,
                    new Geometry.Enclosure(new Geometry.Line())));
                h.assertValueEqual(runtime.execute(enclosed, context).status(), ManifestationResult.Status.UNSUPPORTED_OPERATION, "enclosure remains unsupported");
                var completeRuntime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler(), new MotionManifestationHandler()));
                h.assertValueEqual(completeRuntime.execute(enclosed, context).status(), ManifestationResult.Status.UNSUPPORTED_OPERATION, "unstable containment");
                var triad = new SpellStructure(List.of(
                    locus("a", LocusRole.SOURCE, "blaze", new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1)))),
                    locus("b", LocusRole.MEDIATOR, "copper", new FormExpression(List.of(new FormParticipation(new FormId("test:unknown"), 1)))),
                    locus("c", LocusRole.RECIPIENT, "iron", new FormExpression(List.of()))),
                    List.of(new LocusRelation(new LocusId("a"), new LocusId("b")), new LocusRelation(new LocusId("b"), new LocusId("c"))));
                var mediated = new SpellCompiler().compile(new SpellPattern(triad, NumericalPrinciple.TRIAD, new Geometry.Line()));
                h.assertValueEqual(completeRuntime.execute(mediated, context).status(), ManifestationResult.Status.UNSUPPORTED_OPERATION, "mediation remains unsupported");
                h.assertTrue(!player.isOnFire(), "Rejected structures have no partial manifestation");
                h.succeed();
            },
            "dyad_direction_changes_world_response", h -> {
                var runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler(), new MotionManifestationHandler()));
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5))); player.setYRot(0); player.setXRot(0);
                var heatTarget = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(2.5, 1, 5.5));
                var context = new MinecraftSpellContext(h.getLevel(), player);
                var forward = directed(false, Forms.MOTION);
                var binding = MinecraftSpellBinding.resolve(forward, context);
                h.assertValueEqual(binding.originLocus().id(), new LocusId("a"), "Heat source binds to caster");
                h.assertValueEqual(binding.recipientLocus().orElseThrow().id(), new LocusId("b"), "Motion recipient binds to target");
                h.assertValueEqual(binding.origin(), player.getEyePosition(), "source world position");
                h.assertValueEqual(binding.targets(), List.of(heatTarget), "recipient world entity");
                h.assertValueEqual(runtime.execute(forward, context).status(), ManifestationResult.Status.APPLIED, "forward transfer");
                h.assertTrue(heatTarget.isOnFire(), "Heat belongs to the source");
                h.assertTrue(heatTarget.getDeltaMovement().lengthSqr() == 0, "Recipient Motion is not invoked");

                player.setPos(h.absoluteVec(new Vec3(6.5, 1, 2.5)));
                var motionTarget = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(6.5, 1, 5.5));
                var reverse = directed(true, Forms.MOTION);
                h.assertTrue(!forward.equals(reverse), "Reversing the relationship changes the program");
                var reverseBinding = MinecraftSpellBinding.resolve(reverse, context);
                h.assertValueEqual(reverseBinding.originLocus().id(), new LocusId("b"), "Motion now binds to source");
                h.assertValueEqual(reverseBinding.recipientLocus().orElseThrow().id(), new LocusId("a"), "Heat now binds to recipient");
                h.assertValueEqual(runtime.execute(reverse, context).status(), ManifestationResult.Status.APPLIED, "reverse transfer");
                h.assertTrue(motionTarget.getDeltaMovement().z > 0.5, "Reversed relation invokes source Motion");
                h.assertTrue(!motionTarget.isOnFire(), "Recipient Heat is not invoked");
                h.succeed();
            },
            "unknown_recipient_does_not_block_heat_and_unknown_source_rejects_atomically", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5))); player.setYRot(0); player.setXRot(0);
                var target = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(2.5, 1, 5.5));
                var runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler()));
                var context = new MinecraftSpellContext(h.getLevel(), player);
                var mixed = new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1),
                    new FormParticipation(new FormId("test:unknown"), 1)));
                var structure = new SpellStructure(List.of(locus("a", LocusRole.SOURCE, "blaze", mixed),
                    locus("b", LocusRole.RECIPIENT, "iron", new FormExpression(List.of()))),
                    List.of(new LocusRelation(new LocusId("a"), new LocusId("b"))));
                var unsupported = new SpellCompiler().compile(new SpellPattern(structure, NumericalPrinciple.DYAD, new Geometry.Line()));
                var rejected = runtime.execute(unsupported, context);
                h.assertValueEqual(rejected.status(), ManifestationResult.Status.UNSUPPORTED_FORM, "unknown invoked source");
                h.assertValueEqual(rejected.applications(), 0, "atomic rejection");
                h.assertTrue(!target.isOnFire() && !player.isOnFire(), "No partial Heat application");
                var applied = runtime.execute(directed(false, new FormId("test:unknown")), context);
                h.assertValueEqual(applied.status(), ManifestationResult.Status.APPLIED, "unknown recipient needs no handler");
                h.assertValueEqual(applied.applications(), 1, "only Heat invoked");
                h.assertTrue(target.isOnFire(), "Heat reaches the concrete recipient");
                player.setYRot(180);
                h.assertValueEqual(runtime.execute(directed(false, new FormId("test:unknown")), context).status(),
                    ManifestationResult.Status.NO_TARGET, "missing target is distinct from unsupported Form");
                h.succeed();
            },
            "inactive_source_does_not_invoke_recipient_forms", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5))); player.setYRot(0); player.setXRot(0);
                var target = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(2.5, 1, 5.5));
                var runtime = new MinecraftSpellRuntime(List.of(new HeatManifestationHandler()));
                var result = runtime.execute(directed(true), new MinecraftSpellContext(h.getLevel(), player));
                h.assertValueEqual(result.status(), ManifestationResult.Status.NO_SOURCE_FORM, "inactive source");
                h.assertTrue(!target.isOnFire(), "Heat on a recipient does not supply the source");
                h.succeed();
            },
            "source_and_vessel_gate_execution", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                var stack = new ItemStack(wand); stack.set(SpellComponents.RECORDED_SPELL, DiscoveryFixtures.directedHeat()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var service = new CastService(); var context = new MinecraftSpellContext(h.getLevel(), player);
                var vessel = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of());
                h.assertValueEqual(service.cast(context, stack, vessel, b -> false).status(), ManifestationResult.Status.INSUFFICIENT_SOURCE, "source gate");
                var incompatible = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of(Forms.MOTION));
                h.assertValueEqual(service.cast(context, stack, incompatible, b -> true).status(), ManifestationResult.Status.INCOMPATIBLE_VESSEL, "vessel gate");
                h.succeed();
            });
    }

    private static SpellLocus locus(String id, LocusRole role, String material, FormExpression forms) {
        return new SpellLocus(new LocusId(id), role, new MaterialId("test:" + material), forms);
    }

    private static SpellPattern centered(FormExpression forms, NumericalPrinciple number, Geometry geometry) {
        boolean tetrad = number == NumericalPrinciple.TETRAD;
        var empty = new FormExpression(List.of());
        var loci = new ArrayList<SpellLocus>();
        var relations = new ArrayList<LocusRelation>();
        loci.add(locus("x", tetrad ? LocusRole.ANCHOR : LocusRole.FOCUS, "center", tetrad ? empty : forms));
        if (!(geometry instanceof Geometry.Point)) for (var id : List.of("a", "b", "c", "d")) {
            loci.add(locus(id, tetrad ? LocusRole.STABILIZER : LocusRole.ANCHOR, "arm", tetrad ? forms : empty));
            relations.add(new LocusRelation(new LocusId("x"), new LocusId(id)));
        }
        return new SpellPattern(new SpellStructure(loci, relations), number, geometry);
    }

    private static SpellProgram directed(boolean reverse, FormId... recipientForms) {
        var source = WorkingNode.expressingAll(new NodeId("a"), new GridPoint(0, 0),
            new MaterialProfile(new MaterialId("test:blaze"), List.of(new FormParticipation(Forms.HEAT, 1))));
        var recipient = WorkingNode.expressingAll(new NodeId("b"), new GridPoint(1, 0), new MaterialProfile(new MaterialId("test:iron"),
            Arrays.stream(recipientForms).map(f -> new FormParticipation(f, 1)).toList()));
        var stroke = reverse ? new WorkingStroke(recipient.id(), source.id()) : new WorkingStroke(source.id(), recipient.id());
        var pattern = new WorkingAnalyzer().analyze(new ArcaneWorking(List.of(source, recipient), List.of(stroke), List.of())).pattern().orElseThrow();
        return new SpellCompiler().compile(pattern);
    }

    private static void cast(GameTestHelper h, Item wand, boolean motion, boolean wall) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5))); player.setYRot(0); player.setXRot(0);
        var target = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(2.5, 1, 5.5));
        var stack = new ItemStack(wand); stack.set(SpellComponents.RECORDED_SPELL, DiscoveryFixtures.directedHeat());
        if (motion) stack.set(SpellComponents.RECORDED_SPELL, DiscoveryFixtures.directedMotion());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (wall) for (int y = 1; y <= 3; y++) h.setBlock(2, y, 4, Blocks.STONE);
        h.assertTrue(!target.isOnFire(), "Target starts unheated");
        wand.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        if (wall) {
            h.assertTrue(!target.isOnFire(), "Heat must not pass through a wall");
        } else if (motion) {
            h.assertTrue(target.getDeltaMovement().z > 0.5, "Motion follows directed geometry");
            h.assertTrue(!target.isOnFire(), "Motion does not apply Heat");
        } else {
            h.assertTrue(target.isOnFire(), "Compiled Heat ignites the recipient");
        }
        if (!wall) {
            h.assertTrue(player.getCooldowns().isOnCooldown(stack), "Successful cast imposes recovery");
            var before = target.getDeltaMovement();
            wand.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertValueEqual(target.getDeltaMovement(), before, "repeated use is gated");
        }
        h.succeed();
    }
}
