package zoltan.spellcraft.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** Synchronized, deliberately nonpersistent tool intent. Session identifies this loaded BE instance. */
public record DividerSelection(BlockPos pos, String dimension, long locus, String session, long expires) {
    public static final Codec<DividerSelection> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(DividerSelection::pos),
        Codec.STRING.fieldOf("dimension").forGetter(DividerSelection::dimension),
        Codec.LONG.fieldOf("locus").forGetter(DividerSelection::locus),
        Codec.STRING.fieldOf("session").forGetter(DividerSelection::session),
        Codec.LONG.fieldOf("expires").forGetter(DividerSelection::expires)
    ).apply(i, DividerSelection::new));
    public static final DataComponentType<DividerSelection> TYPE = DataComponentType.<DividerSelection>builder()
        .networkSynchronized(ByteBufCodecs.fromCodec(CODEC)).build();
}
