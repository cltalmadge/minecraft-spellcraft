package zoltan.spellcraft.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

public final class SpellbookScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int BOX_HEIGHT = 110;
    private static final int BOX_MARGIN = 6;
    private static final int SCROLL_BTN_W = 14;
    private static final int SCROLL_BTN_H = 16;
    private static final int SCROLL_STEP = ROW_HEIGHT * 2;

    /**
     * Screen-local placeholder record. Presentation data only; not an authoritative
     * gameplay model and not persisted or synced.
     */
    private record PlaceholderSpell(
            String name,
            String effect,
            String magnitude,
            String duration,
            String delivery,
            int cost
    ) {}

    private static final List<PlaceholderSpell> PLACEHOLDER_SPELLS = List.of(
            new PlaceholderSpell("Flare", "Damage Health", "6", null, "Ray", 12),
            new PlaceholderSpell("Heal Minor Wounds", "Restore Health", "5", null, "Self", 10),
            new PlaceholderSpell("Fleet Step", "Fortify Speed", "10", "20s", "Self", 14)
    );

    private record DetailLine(FormattedCharSequence text, int color) {}

    private PlaceholderSpell selectedSpell = PLACEHOLDER_SPELLS.getFirst();

    // Fixed layout geometry (recomputed on init/resize).
    private int boxX;
    private int boxTop;
    private int boxWidth;
    private int contentX;
    private int contentWidth;
    private int contentTop;
    private int viewportHeight;

    // Scrolling state.
    private double scrollOffset;
    private int maxScroll;
    private List<DetailLine> detailLines = new ArrayList<>();
    private Button upButton;
    private Button downButton;

    public SpellbookScreen() {
        super(Component.translatable("screen.spellcraft.spellbook.title"));
    }

    @Override
    protected void init() {
        computeLayout();
        rebuildDetailLines();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int closeX = width / 2 - buttonWidth / 2;
        int closeY = height - 40;

        addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.spellcraft.spellbook.close"),
                                _ -> onClose()
                        )
                        .bounds(closeX, closeY, buttonWidth, buttonHeight)
                        .build()
        );

        for (int i = 0; i < PLACEHOLDER_SPELLS.size(); i++) {
            final int index = i;
            addRenderableWidget(
                    Button.builder(
                                    Component.empty(),
                                    _ -> selectSpell(index)
                            )
                            .bounds(rowX(), rowYFor(i), rowWidth(), ROW_HEIGHT)
                            .build()
            );
        }

        addScrollButtons();
    }

    private void addScrollButtons() {
        if (upButton != null) {
            return;
        }

        int sbX = boxX + boxWidth - 4 - SCROLL_BTN_W;
        int centerY = contentTop + viewportHeight / 2;

        upButton = Button.builder(
                        Component.literal("▲"),
                        _ -> scrollUp()
                )
                .bounds(sbX, centerY - SCROLL_BTN_H - 1, SCROLL_BTN_W, SCROLL_BTN_H)
                .build();
        downButton = Button.builder(
                        Component.literal("▼"),
                        _ -> scrollDown()
                )
                .bounds(sbX, centerY + 1, SCROLL_BTN_W, SCROLL_BTN_H)
                .build();

        addRenderableWidget(upButton);
        addRenderableWidget(downButton);
    }

    private void computeLayout() {
        boxWidth = Math.clamp(width / 2, 220, width - 24);
        boxX = (width - boxWidth) / 2;
        // Anchored just above the fixed Close button so the box can never push it off-screen.
        boxTop = (height - 40) - 24 - BOX_HEIGHT;

        int margin = BOX_MARGIN;
        contentX = boxX + margin;
        contentWidth = boxWidth - 2 * margin - SCROLL_BTN_W - 4;
        contentTop = boxTop + margin;
        viewportHeight = BOX_HEIGHT - 2 * margin;
    }

    private int listTop() {
        return boxTop - 20 - PLACEHOLDER_SPELLS.size() * ROW_HEIGHT;
    }

    private int titleY() {
        return Math.max(8, listTop() - 22);
    }

    private int rowX() {
        return (width - font.width("Heal Minor Wounds")) / 2;
    }

    private int rowWidth() {
        return font.width("Heal Minor Wounds");
    }

    private int rowYFor(int index) {
        return listTop() + index * ROW_HEIGHT;
    }

    private void selectSpell(int index) {
        if (index >= 0 && index < PLACEHOLDER_SPELLS.size()) {
            selectedSpell = PLACEHOLDER_SPELLS.get(index);
            scrollOffset = 0;
            rebuildDetailLines();
        }
    }

    private void scrollUp() {
        scrollOffset -= SCROLL_STEP;
        clampScroll();
    }

    private void scrollDown() {
        scrollOffset += SCROLL_STEP;
        clampScroll();
    }

    private void clampScroll() {
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
    }

    private void rebuildDetailLines() {
        detailLines = new ArrayList<>();
        addDetail(selectedSpell.name(), 0xFFFFFFFF);
        addDetail(selectedSpell.effect(), 0xFFE0E0E0);
        addDetail("Magnitude: " + selectedSpell.magnitude(), 0xFFB0B0B0);
        if (selectedSpell.duration() != null) {
            addDetail("Duration: " + selectedSpell.duration(), 0xFFB0B0B0);
        }
        addDetail("Delivery: " + selectedSpell.delivery(), 0xFFB0B0B0);
        addDetail("Cost: " + selectedSpell.cost(), 0xFFB0B0B0);

        int contentHeight = detailLines.size() * font.lineHeight;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    private void addDetail(String logical, int color) {
        for (FormattedCharSequence segment : font.split(Component.literal(logical), contentWidth)) {
            detailLines.add(new DetailLine(segment, color));
        }
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleX = (width - font.width(title)) / 2;
        graphics.text(
                font,
                title,
                titleX,
                titleY(),
                0xFFFFFFFF,
                true
        );

        int spellY = rowYFor(0);
        for (PlaceholderSpell spell : PLACEHOLDER_SPELLS) {
            int color = (spell == selectedSpell) ? 0xFFFFFFFF : 0xFFB0B0B0;
            graphics.text(
                    font,
                    Component.literal(spell.name()),
                    rowX(),
                    spellY,
                    color,
                    true
            );
            spellY += ROW_HEIGHT;
        }

        renderDetailBox(graphics);
    }

    private void renderDetailBox(GuiGraphicsExtractor graphics) {
        graphics.fill(boxX, boxTop, boxX + boxWidth, boxTop + BOX_HEIGHT, 0x99000000);
        graphics.outline(boxX, boxTop, boxWidth, BOX_HEIGHT, 0xFFD8D8D8);

        if (detailLines.isEmpty()) {
            return;
        }

        int lineHeight = font.lineHeight;
        graphics.enableScissor(contentX, contentTop, contentX + contentWidth, contentTop + viewportHeight);
        int firstVisible = (int) (scrollOffset / lineHeight);
        int visibleCount = viewportHeight / lineHeight + 1;
        for (int i = firstVisible; i < Math.min(detailLines.size(), firstVisible + visibleCount); i++) {
            DetailLine line = detailLines.get(i);
            int y = contentTop + i * lineHeight - (int) scrollOffset;
            graphics.text(font, line.text(), contentX, y, line.color(), true);
        }
        graphics.disableScissor();

        if (upButton != null) {
            upButton.visible = maxScroll > 0;
            downButton.visible = maxScroll > 0;
        }
    }
}
