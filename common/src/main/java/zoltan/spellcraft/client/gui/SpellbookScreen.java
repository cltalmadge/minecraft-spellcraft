package zoltan.spellcraft.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpellbookScreen extends Screen {
    private static final Component EMPTY_MESSAGE =
            Component.translatable("screen.spellcraft.spellbook.empty");

    public SpellbookScreen() {
        super(Component.translatable("screen.spellcraft.spellbook.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int buttonHeight = 20;

        addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.spellcraft.spellbook.close"),
                                button -> onClose()
                        )
                        .bounds(
                                width / 2 - buttonWidth / 2,
                                height / 2 + 30,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (width - font.width(title)) / 2;
        int messageX = (width - font.width(EMPTY_MESSAGE)) / 2;

        graphics.text(
                font,
                title,
                titleX,
                height / 2 - 40,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                font,
                EMPTY_MESSAGE,
                messageX,
                height / 2 - 10,
                0xFFB0B0B0,
                true
        );
    }
}