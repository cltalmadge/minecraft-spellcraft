package zoltan.spellcraft.runtime;

import java.util.*;
import java.util.function.Consumer;
import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.form.Forms;
import dev.spellcraft.domain.manifestation.SpellBurden;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.NumericalPrinciple;
import dev.spellcraft.domain.pattern.SpellPattern;
import dev.spellcraft.domain.program.SpellCompiler;
import dev.spellcraft.domain.vessel.VesselProfile;
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
                h.assertValueEqual(stack.get(SpellComponents.RECORDED_SPELL), new ItemStack(wand).get(SpellComponents.RECORDED_SPELL), "portable recording");
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
                var pattern = new SpellPattern(new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1))),
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
                var unknown = new SpellCompiler().compile(new SpellPattern(forms, NumericalPrinciple.MONAD, new Geometry.Point()));
                var context = new MinecraftSpellContext(h.getLevel(), player);
                h.assertValueEqual(runtime.execute(unknown, context).status(), ManifestationResult.Status.UNSUPPORTED_FORM, "unknown Form");
                h.assertTrue(!player.isOnFire(), "Known forms must not execute before rejecting unknown ones");
                var fixed = new SpellCompiler().compile(new SpellPattern(new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1))),
                    NumericalPrinciple.TETRAD, new Geometry.Enclosure(new Geometry.Intersection())));
                h.assertValueEqual(runtime.execute(fixed, context).status(), ManifestationResult.Status.UNSUPPORTED_OPERATION, "sustained structure");
                h.assertTrue(!player.isOnFire(), "Sustain must not become a one-shot cast");
                h.succeed();
            },
            "source_and_vessel_gate_execution", h -> {
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                var stack = new ItemStack(wand); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var service = new CastService(); var context = new MinecraftSpellContext(h.getLevel(), player);
                var vessel = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of());
                h.assertValueEqual(service.cast(context, stack, vessel, b -> false).status(), ManifestationResult.Status.INSUFFICIENT_SOURCE, "source gate");
                var incompatible = new VesselProfile(new SpellBurden(3, 6, 0), true, Set.of(Forms.MOTION));
                h.assertValueEqual(service.cast(context, stack, incompatible, b -> true).status(), ManifestationResult.Status.INCOMPATIBLE_VESSEL, "vessel gate");
                h.succeed();
            });
    }
    private static void cast(GameTestHelper h, Item wand, boolean motion, boolean wall) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new Vec3(2.5, 1, 2.5))); player.setYRot(0); player.setXRot(0);
        var target = h.spawnWithNoFreeWill(EntityTypes.HUSK, new Vec3(2.5, 1, 5.5));
        var stack = new ItemStack(wand);
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
