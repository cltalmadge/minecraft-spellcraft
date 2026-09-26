package zoltan.spellcraft.persistence;

import dev.spellcraft.domain.program.RecordedSpell;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class SpellComponents {
    /** Both loaders register this same immutable type before constructing item defaults. */
    public static final DataComponentType<RecordedSpell> RECORDED_SPELL = DataComponentType.<RecordedSpell>builder()
        .persistent(SpellCodecs.RECORDED)
        .networkSynchronized(ByteBufCodecs.fromCodec(SpellCodecs.RECORDED))
        .build();
    private SpellComponents() {}
}
