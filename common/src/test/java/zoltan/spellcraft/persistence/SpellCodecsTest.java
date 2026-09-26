package zoltan.spellcraft.persistence;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.spellcraft.domain.form.FormExpression;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.form.Forms;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.NumericalPrinciple;
import dev.spellcraft.domain.pattern.SpellPattern;
import dev.spellcraft.domain.program.RecordedSpell;
import dev.spellcraft.domain.program.SpellCompiler;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SpellCodecsTest {
    private RecordedSpell recording(Geometry geometry, NumericalPrinciple number) {
        var pattern = new SpellPattern(new FormExpression(List.of(new FormParticipation(Forms.HEAT, 1), new FormParticipation(Forms.MOTION, 0.25))), number, geometry);
        return new RecordedSpell(1, "An observation", new SpellCompiler().compile(pattern));
    }
    @Test void allGeometriesAndOperationsRoundTrip() {
        var geometries = List.of(new Geometry.Point(), new Geometry.Line(), new Geometry.Radial(true), new Geometry.Radial(false), new Geometry.Intersection(), new Geometry.Enclosure(new Geometry.Intersection()));
        for (var geometry : geometries) for (var number : NumericalPrinciple.values()) {
            if (geometry instanceof Geometry.Enclosure && number != NumericalPrinciple.TETRAD) continue;
            var original = recording(geometry, number);
            var json = SpellCodecs.RECORDED.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).getOrThrow());
            assertFalse(json.toString().contains("dev.spellcraft"));
        }
    }
    @Test void rejectsUnknownVersionsTagsAndMalformedGrammar() {
        var original = SpellCodecs.RECORDED.encodeStart(JsonOps.INSTANCE, recording(new Geometry.Line(), NumericalPrinciple.DYAD)).getOrThrow();
        var outer = original.deepCopy().getAsJsonObject(); outer.addProperty("schema_version", 2);
        assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, outer).error().isPresent());
        var inner = original.deepCopy().getAsJsonObject(); inner.getAsJsonObject("program").addProperty("schema_version", 2);
        assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, inner).error().isPresent());
        for (String bad : List.of("unknown", "sustain")) {
            var json = original.deepCopy().getAsJsonObject();
            json.getAsJsonObject("program").getAsJsonArray("instructions").get(0).getAsJsonObject().addProperty("kind", bad);
            assertTrue(SpellCodecs.RECORDED.parse(JsonOps.INSTANCE, json).error().isPresent());
        }
        assertTrue(SpellCodecs.PROGRAM.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"schema_version\":1,\"instructions\":[]}")).error().isPresent());
        assertTrue(SpellCodecs.PARTICIPATION.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"form\":\"spellcraft:heat\",\"strength\":2}")).error().isPresent());
        assertTrue(SpellCodecs.FORM.parse(JsonOps.INSTANCE, JsonParser.parseString("\"invalid\"")).error().isPresent());
    }
    @Test void componentStreamCodecRoundTrips() {
        var original = recording(new Geometry.Line(), NumericalPrinciple.DYAD);
        var codec = ByteBufCodecs.fromCodec(SpellCodecs.RECORDED);
        var buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, original);
            assertEquals(original, codec.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
