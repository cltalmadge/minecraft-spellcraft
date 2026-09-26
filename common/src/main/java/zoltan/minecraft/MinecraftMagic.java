package zoltan.minecraft;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinecraftMagic {
    public static final String MOD_ID = "spellcraft";
    public static final String MOD_NAME = "Spellcraft Prototype";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private MinecraftMagic() {}

    public static void initialize(String platform) {
        Component message = Component.literal(
            "Spellcraft common Minecraft layer initialized through " + platform
        );
        LOGGER.info(message.getString());
    }
}
