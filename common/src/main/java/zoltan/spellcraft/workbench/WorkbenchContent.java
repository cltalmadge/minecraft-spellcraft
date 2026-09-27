package zoltan.spellcraft.workbench;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

/** Loader registrations supply the type; gameplay stays in common. */
public final class WorkbenchContent {
    public static Supplier<BlockEntityType<ArcaneWorkbenchBlockEntity>> entityType;
    private WorkbenchContent() {}
    public static boolean isPage(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.fromNamespaceAndPath("spellcraft", "spell_page"));
    }
}
