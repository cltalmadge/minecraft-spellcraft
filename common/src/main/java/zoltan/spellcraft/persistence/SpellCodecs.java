package zoltan.spellcraft.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.spellcraft.domain.form.FormId;
import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.pattern.Geometry;
import dev.spellcraft.domain.pattern.MagicalOperation;
import dev.spellcraft.domain.program.RecordedSpell;
import dev.spellcraft.domain.program.SpellInstruction;
import dev.spellcraft.domain.program.SpellProgram;
import java.util.List;
import java.util.function.Supplier;

/** Explicit tagged schema. Tags are save-format identifiers, never Java class names. */
public final class SpellCodecs {
    private SpellCodecs() {}
    private static <T> DataResult<T> validated(Supplier<T> factory) {
        try { return DataResult.success(factory.get()); }
        catch (IllegalArgumentException | NullPointerException e) { return DataResult.error(() -> e.getMessage() == null ? "Invalid spell value" : e.getMessage()); }
    }
    public static final Codec<FormId> FORM = Codec.STRING.comapFlatMap(s -> validated(() -> new FormId(s)), FormId::value);
    private record ParticipationFields(FormId form, double strength) {}
    public static final Codec<FormParticipation> PARTICIPATION = RecordCodecBuilder.<ParticipationFields>create(i -> i.group(
        FORM.fieldOf("form").forGetter(ParticipationFields::form),
        Codec.DOUBLE.fieldOf("strength").forGetter(ParticipationFields::strength)
    ).apply(i, ParticipationFields::new)).comapFlatMap(f -> validated(() -> new FormParticipation(f.form(), f.strength())),
        f -> new ParticipationFields(f.form(), f.strength()));
    public static final Codec<MagicalOperation> OPERATION = Codec.STRING.comapFlatMap(s -> switch (s) {
        case "concentrate" -> DataResult.success(MagicalOperation.CONCENTRATE);
        case "transfer" -> DataResult.success(MagicalOperation.TRANSFER);
        case "mediate" -> DataResult.success(MagicalOperation.MEDIATE);
        case "stabilize" -> DataResult.success(MagicalOperation.STABILIZE);
        default -> DataResult.error(() -> "Unknown operation: " + s);
    }, op -> switch (op) {
        case CONCENTRATE -> "concentrate"; case TRANSFER -> "transfer";
        case MEDIATE -> "mediate"; case STABILIZE -> "stabilize";
    });
    private static final Codec<Geometry> OPEN_GEOMETRY = Codec.STRING.comapFlatMap(s -> switch (s) {
        case "point" -> DataResult.success(new Geometry.Point());
        case "line" -> DataResult.success(new Geometry.Line());
        case "radial_outward" -> DataResult.success(new Geometry.Radial(true));
        case "radial_inward" -> DataResult.success(new Geometry.Radial(false));
        case "intersection" -> DataResult.success(new Geometry.Intersection());
        default -> DataResult.error(() -> "Unknown geometry: " + s);
    }, g -> switch (g) {
        case Geometry.Point ignored -> "point";
        case Geometry.Line ignored -> "line";
        case Geometry.Radial r -> r.outward() ? "radial_outward" : "radial_inward";
        case Geometry.Intersection ignored -> "intersection";
        case Geometry.Enclosure ignored -> throw new IllegalArgumentException("Nested enclosure");
    });
    private record GeometryFields(Geometry interior, boolean enclosed) {}
    public static final Codec<Geometry> GEOMETRY = RecordCodecBuilder.<GeometryFields>create(i -> i.group(
        OPEN_GEOMETRY.fieldOf("topology").forGetter(GeometryFields::interior),
        Codec.BOOL.fieldOf("enclosed").forGetter(GeometryFields::enclosed)
    ).apply(i, GeometryFields::new)).xmap(f -> f.enclosed() ? new Geometry.Enclosure(f.interior()) : f.interior(),
        g -> g instanceof Geometry.Enclosure e ? new GeometryFields(e.interior(), true) : new GeometryFields(g, false));
    public static final Codec<SpellInstruction> INSTRUCTION = Codec.STRING.dispatch("kind", instruction -> switch (instruction) {
        case SpellInstruction.Invoke ignored -> "invoke";
        case SpellInstruction.Operate ignored -> "operate";
        case SpellInstruction.Shape ignored -> "shape";
        case SpellInstruction.Release ignored -> "release";
        case SpellInstruction.Sustain ignored -> "sustain";
    }, tag -> switch (tag) {
        case "invoke" -> PARTICIPATION.comapFlatMap(f -> validated(() -> new SpellInstruction.Invoke(f)), SpellInstruction.Invoke::participation).fieldOf("participation");
        case "operate" -> OPERATION.xmap(SpellInstruction.Operate::new, SpellInstruction.Operate::operation).fieldOf("operation");
        case "shape" -> GEOMETRY.xmap(SpellInstruction.Shape::new, SpellInstruction.Shape::geometry).fieldOf("geometry");
        case "release" -> com.mojang.serialization.MapCodec.unit(new SpellInstruction.Release());
        case "sustain" -> com.mojang.serialization.MapCodec.unit(new SpellInstruction.Sustain());
        default -> com.mojang.serialization.MapCodec.<SpellInstruction>unit(new SpellInstruction.Release())
            .validate(v -> DataResult.error(() -> "Unknown instruction: " + tag));
    });
    private record ProgramFields(int version, List<SpellInstruction> instructions) {}
    public static final Codec<SpellProgram> PROGRAM = RecordCodecBuilder.<ProgramFields>create(i -> i.group(
        Codec.INT.fieldOf("schema_version").forGetter(ProgramFields::version),
        INSTRUCTION.listOf(4, SpellProgram.MAX_INSTRUCTIONS).fieldOf("instructions").forGetter(ProgramFields::instructions)
    ).apply(i, ProgramFields::new)).comapFlatMap(f -> validated(() -> new SpellProgram(f.version(), f.instructions())),
        p -> new ProgramFields(p.schemaVersion(), p.instructions()));
    private record RecordingFields(int version, String name, SpellProgram program) {}
    public static final Codec<RecordedSpell> RECORDED = RecordCodecBuilder.<RecordingFields>create(i -> i.group(
        Codec.INT.fieldOf("schema_version").forGetter(RecordingFields::version),
        Codec.STRING.fieldOf("name").forGetter(RecordingFields::name),
        PROGRAM.fieldOf("program").forGetter(RecordingFields::program)
    ).apply(i, RecordingFields::new)).comapFlatMap(f -> validated(() -> new RecordedSpell(f.version(), f.name(), f.program())),
        r -> new RecordingFields(r.schemaVersion(), r.name(), r.program()));
}
