package zoltan.spellcraft.items;

import net.minecraft.world.item.ItemStack;
import zoltan.spellcraft.persistence.SpellComponents;
import zoltan.spellcraft.workbench.WorkbenchContent;

public final class SpellPageBinding {
    private SpellPageBinding() {}
    public static boolean bind(ItemStack wand, ItemStack page) {
        var spell = page.get(SpellComponents.RECORDED_SPELL);
        if (!(wand.getItem() instanceof WandItem) || !WorkbenchContent.isPage(page) || spell == null) return false;
        wand.set(SpellComponents.RECORDED_SPELL, spell); return true;
    }
}
