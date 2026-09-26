package zoltan.spellcraft.persistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.spellcraft.domain.form.*;
import dev.spellcraft.domain.material.MaterialId;
import dev.spellcraft.domain.material.MaterialProfile;
import dev.spellcraft.domain.working.*;
import dev.spellcraft.domain.pattern.*;
import dev.spellcraft.domain.program.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellCodecsTest {
    private SpellLocus locus(String id, LocusRole role, String material, FormId... forms) {
        return new SpellLocus(new LocusId(id), role, new MaterialId("test:" + material),
            new FormExpression(Arrays.stream(forms).map(f -> new FormParticipation(f, 1)).toList()));
    }

    private LocusRelation relation(String from, String to) { return new LocusRelation(new LocusId(from), new LocusId(to)); }

    private RecordedSpell recording(Geometry geometry, NumericalPrinciple number) {
        var loci = new ArrayList<SpellLocus>();
        var relations = new ArrayList<LocusRelation>();
        Geometry topology = geometry instanceof Geometry.Enclosure enclosure ? enclosure.interior() : geometry;
        switch (number) {
            case MONAD -> {
                loci.add(locus("x", LocusRole.FOCUS, "blaze", Forms.HEAT, Forms.MOTION));
                if (topology instanceof Geometry.Radial radial) {
                    for (var id : List.of("a", "b", "c", "d")) {
                        loci.add(locus(id, LocusRole.ANCHOR, "anchor"));
                        relations.add(radial.outward() ? relation("x", id) : relation(id, "x"));
                    }
                }
            }
            case DYAD, TRIAD -> {
                loci.add(locus("a", LocusRole.SOURCE, "blaze", Forms.HEAT));
                loci.add(locus("c", LocusRole.RECIPIENT, "iron"));
                if (number == NumericalPrinciple.TRIAD) {
                    loci.add(locus("b", LocusRole.MEDIATOR, "copper", Forms.MOTION));
                    relations.add(relation("a", "b"));
                    relations.add(relation("b", "c"));
                } else relations.add(relation("a", "c"));
            }
            case TETRAD -> {
                loci.add(locus("x", LocusRole.ANCHOR, "center"));
                for (var id : List.of("a", "b", "c", "d")) {
                    loci.add(locus(id, LocusRole.STABILIZER, "blaze", Forms.HEAT));
                    relations.add(relation(id, "x"));
                }
            }
        }
        var pattern = new SpellPattern(new SpellStructure(loci, relations), number, geometry);
        return new RecordedSpell(RecordedSpell.CURRENT_SCHEMA_VERSION, "An observation", new SpellCompiler().compile(pattern));
    }

    private List<RecordedSpell> recordings() {
        var recordings = new ArrayList<RecordedSpell>();
        for (var enclosed : List.of(false, true)) {
            for (var g : List.of(new Geometry.Point(), new Geometry.Radial(true), new Geometry.Radial(false)))
                recordings.add(recording(enclosed ? new Geometry.Enclosure(g) : g, NumericalPrinciple.MONAD));
            for (var n : List.of(NumericalPrinciple.DYAD, NumericalPrinciple.TRIAD))
                recordings.add(recording(enclosed ? new Geometry.Enclosure(new Geometry.Line()) : new Geometry.Line(), n));
            recordings.add(recording(enclosed ? new Geometry.Enclosure(new Geometry.Intersection()) : new Geometry.Intersection(), NumericalPrinciple.TETRAD));
        }
        return recordings;
    }

    private JsonObject encode(RecordedSpell recording) {
        return SpellCodecs.RECORDED.encodeStart(JsonOps.INSTANCE, recording).getOrThrow().getAsJsonObject();
    }

    @Test void everySupportedMotifRoundTripsIncludingUnstableContainment() {
        for (var original : recordings()) {
            var json = encode(original);
            assertEquals(original, SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).getOrThrow());
            assertFalse(json.toString().contains("dev.spellcraft"));
            assertFalse(json.toString().contains("position"));
            assertEquals(3, json.getAsJsonObject("program").get("schema_version").getAsInt());
        }
    }

    @Test void triadRoundTripRetainsMediatorMaterialAndFormOwnership() {
        var original = recording(new Geometry.Line(), NumericalPrinciple.TRIAD);
        var decoded = SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, encode(original)).getOrThrow();
        var structure = decoded.program().structure();
        assertEquals(new MaterialId("test:copper"), structure.single(LocusRole.MEDIATOR).material());
        assertEquals(List.of(new FormParticipation(Forms.MOTION, 1)), structure.single(LocusRole.MEDIATOR).expressedForms().terms());
        assertEquals(List.of(new FormParticipation(Forms.HEAT, 1)), structure.single(LocusRole.SOURCE).expressedForms().terms());
        assertTrue(structure.single(LocusRole.RECIPIENT).expressedForms().terms().isEmpty());
        assertEquals(List.of(relation("a", "b"), relation("b", "c")), structure.relations());
    }

    @Test void reversedDyadRemainsDifferentAfterRoundTrip() {
        var forward = recording(new Geometry.Line(), NumericalPrinciple.DYAD);
        var reversed = forward.program().structure().loci().stream().map(l -> new SpellLocus(l.id(),
            l.role() == LocusRole.SOURCE ? LocusRole.RECIPIENT : LocusRole.SOURCE, l.material(), l.expressedForms())).toList();
        var reverseProgram = new SpellProgram(SpellProgram.CURRENT_SCHEMA_VERSION, new SpellStructure(reversed, List.of(relation("c", "a"))), forward.program().instructions());
        var reverse = new RecordedSpell(1, forward.name(), reverseProgram);
        var decoded = SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, encode(reverse)).getOrThrow();
        assertEquals(reverse, decoded);
        assertNotEquals(forward.program(), decoded.program());
        assertEquals(new MaterialId("test:iron"), decoded.program().structure().single(LocusRole.SOURCE).material());
        assertTrue(decoded.program().invokedForms().isEmpty());
    }

    @Test void rejectsUnknownVersionsTagsAndMalformedGrammar() {
        var original = encode(recording(new Geometry.Line(), NumericalPrinciple.DYAD));
        var outer = original.deepCopy(); outer.addProperty("schema_version", 2);
        assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, outer).error().isPresent());
        for (int version : List.of(1, 2, 4)) {
            var inner = original.deepCopy(); inner.getAsJsonObject("program").addProperty("schema_version", version);
            assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, inner).error().isPresent());
        }
        for (String bad : List.of("unknown", "invoke", "sustain")) {
            var json = original.deepCopy();
            json.getAsJsonObject("program").getAsJsonArray("instructions").get(0).getAsJsonObject().addProperty("kind", bad);
            assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).error().isPresent());
        }
        assertTrue(SpellCodecs.PARTICIPATION.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"form\":\"spellcraft:heat\",\"strength\":2}")).error().isPresent());
        assertTrue(SpellCodecs.FORM.parse(JsonOps.INSTANCE, JsonParser.parseString("\"invalid\"")).error().isPresent());
    }

    @Test void invalidRolesReferencesAndContributionsReturnCodecErrors() {
        var original = encode(recording(new Geometry.Line(), NumericalPrinciple.TRIAD));
        var mutations = List.<Consumer<JsonObject>>of(
            s -> s.getAsJsonArray("loci").get(0).getAsJsonObject().addProperty("role", "unknown"),
            s -> s.getAsJsonArray("loci").get(1).getAsJsonObject().addProperty("role", "source"),
            s -> s.getAsJsonArray("loci").get(1).getAsJsonObject().addProperty("id", "a"),
            s -> s.getAsJsonArray("loci").get(1).getAsJsonObject().addProperty("material", "invalid"),
            s -> s.getAsJsonArray("relations").get(0).getAsJsonObject().addProperty("to", "missing"),
            s -> s.getAsJsonArray("relations").get(0).getAsJsonObject().addProperty("to", "a"),
            s -> s.getAsJsonArray("relations").add(s.getAsJsonArray("relations").get(0).deepCopy()),
            s -> s.getAsJsonArray("loci").get(1).getAsJsonObject().getAsJsonArray("expressed_forms").add(
                s.getAsJsonArray("loci").get(1).getAsJsonObject().getAsJsonArray("expressed_forms").get(0).deepCopy()),
            s -> s.getAsJsonArray("loci").get(0).getAsJsonObject().getAsJsonArray("expressed_forms").get(0).getAsJsonObject().addProperty("strength", Double.NaN)
        );
        for (var mutation : mutations) {
            var json = original.deepCopy();
            mutation.accept(json.getAsJsonObject("program").getAsJsonObject("structure"));
            assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).error().isPresent(), json.toString());
        }
    }

    @Test void rejectsForgedSustainOnUnstableEnclosure() {
        var json = encode(recording(new Geometry.Enclosure(new Geometry.Line()), NumericalPrinciple.DYAD));
        json.getAsJsonObject("program").getAsJsonArray("instructions").get(2).getAsJsonObject().addProperty("kind", "sustain");
        assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test void componentStreamCodecRoundTripsAllSemanticStructures() {
        var codec = ByteBufCodecs.fromCodec(SpellCodecs.RECORDED);
        for (var original : recordings()) {
            var buffer = Unpooled.buffer();
            try {
                codec.encode(buffer, original);
                assertEquals(original, codec.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void roundTripPersistsOnlyDiscoveryExpressionAndMaterialIdentity() {
        var material = new MaterialProfile(new MaterialId("test:blaze"), List.of(
            new FormParticipation(Forms.HEAT, 1), new FormParticipation(Forms.MOTION, 0.5)));
        var heat = new FormExpression(List.of(new FormParticipation(Forms.HEAT, 0.7)));
        var node = new WorkingNode(new NodeId("source"), new GridPoint(0, 0), material, heat);
        var pattern = new WorkingAnalyzer().analyze(new ArcaneWorking(List.of(node), List.of(), List.of())).pattern().orElseThrow();
        var original = new RecordedSpell(1, "Selected Heat", new SpellCompiler().compile(pattern));
        var json = encode(original);
        var locus = json.getAsJsonObject("program").getAsJsonObject("structure").getAsJsonArray("loci").get(0).getAsJsonObject();
        assertEquals(Set.of("id", "role", "material", "expressed_forms"), locus.keySet());
        assertEquals("test:blaze", locus.get("material").getAsString());
        assertEquals(1, locus.getAsJsonArray("expressed_forms").size());
        assertFalse(json.toString().contains("spellcraft:motion"));
        var decoded = SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(original, decoded);
        assertEquals(heat, decoded.program().structure().single(LocusRole.FOCUS).expressedForms());
        assertEquals(heat.terms(), decoded.program().invokedForms());
        // The old field is not accepted as an expression, even with a current version tag.
        locus.add("forms", locus.remove("expressed_forms"));
        assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

}
