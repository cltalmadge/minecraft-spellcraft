package zoltan.spellcraft.runtime;

import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** All aim and origin data are read from authoritative server state. */
public record MinecraftSpellContext(ServerLevel level, Player caster) {
    public MinecraftSpellContext { Objects.requireNonNull(level); Objects.requireNonNull(caster); }
}
