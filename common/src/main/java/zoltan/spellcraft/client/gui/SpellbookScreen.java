package zoltan.spellcraft.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import zoltan.spellcraft.client.SpellbookEntrySnapshot;
import zoltan.spellcraft.client.SpellbookSnapshot;

public final class SpellbookScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int BOX_HEIGHT = 110;
    private static final int BOX_MARGIN = 6;
    private static final int SCROLL_BTN_W = 14;
    private static final int SCROLL_BTN_H = 16;
    private static final int SCROLL_STEP = ROW_HEIGHT * 2;

    private static final String EMPTY_STATE_TEXT = "No spells learned yet.";

    /**
     * Immutable, read-only snapshot supplied by the caller. The screen renders from
     * this and never owns the spell definitions themselves.
     */
    private final @NonNull SpellbookSnapshot snapshot;

    private record DetailLine(FormattedCharSequence text, int color) {}

    /**
     * Screen-local UI selection state. Null when there is no selection (for example an
     * empty snapshot), in which case the empty-state text is rendered instead of details.
     */
    private SpellbookEntrySnapshot selectedEntry;

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

    public SpellbookScreen(@NonNull SpellbookSnapshot snapshot) {
        super(Component.translatable("screen.spellcraft.spellbook.title"));
        this.snapshot = snapshot;
        // Default to the first entry when there is one to select. An empty snapshot
        // leaves selectedEntry null so the empty-state path renders.
        if (!snapshot.spells().isEmpty()) {
            this.selectedEntry = snapshot.spells().getFirst();
        }
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

        List<SpellbookEntrySnapshot> spells = snapshot.spells();
        for (int i = 0; i < spells.size(); i++) {
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
        return boxTop - 20 - snapshot.spells().size() * ROW_HEIGHT;
    }

    private int titleY() {
        return Math.max(8, listTop() - 22);
    }

    private int rowX() {
        return (width - rowWidth()) / 2;
    }

    private int rowWidth() {
        return snapshot.spells().stream().mapToInt(s -> font.width(s.name())).max().orElse(100);
    }

    private int rowYFor(int index) {
        return listTop() + index * ROW_HEIGHT;
    }

    private void selectSpell(int index) {
        List<SpellbookEntrySnapshot> spells = snapshot.spells();
        if (index >= 0 && index < spells.size()) {
            selectedEntry = spells.get(index);
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
        // No selection (e.g. empty snapshot): nothing to render in the detail box.
        if (selectedEntry == null) {
            return;
        }

        addDetail(selectedEntry.name(), 0xFFFFFFFF);
        for (String detailLine : selectedEntry.detailLines()) {
            addDetail(detailLine, 0xFFB0B0B0);
        }

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
        for (SpellbookEntrySnapshot spell : snapshot.spells()) {
            int color = (spell == selectedEntry) ? 0xFFFFFFFF : 0xFFB0B0B0;
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

        // No selection: render the empty-state text instead of any details.
        if (selectedEntry == null) {
            int textX = (width - font.width(EMPTY_STATE_TEXT)) / 2;
            int textY = boxTop + (BOX_HEIGHT - font.lineHeight) / 2;
            graphics.text(font, Component.literal(EMPTY_STATE_TEXT), textX, textY, 0xFFB0B0B0, true);
            return;
        }

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
