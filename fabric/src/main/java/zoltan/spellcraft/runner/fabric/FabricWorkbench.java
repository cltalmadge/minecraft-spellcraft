package zoltan.spellcraft.runner.fabric;

import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import zoltan.spellcraft.workbench.*;

public final class FabricWorkbench {
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("spellcraft", name); }
    public static final ArcaneWorkbenchBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("arcane_workbench"),
        new ArcaneWorkbenchBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.STONE)
            .setId(ResourceKey.create(Registries.BLOCK, id("arcane_workbench")))));
    public static final BlockEntityType<ArcaneWorkbenchBlockEntity> TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("arcane_workbench"),
        new BlockEntityType<>(ArcaneWorkbenchBlockEntity::new, Set.of(BLOCK)));
    public static void initialize() {
        WorkbenchContent.entityType = () -> TYPE;
        Registry.register(BuiltInRegistries.ITEM, id("arcane_workbench"), new BlockItem(BLOCK,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("arcane_workbench")))));
        Registry.register(BuiltInRegistries.ITEM, id("divider"), new DividerItem(new Item.Properties().stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, id("divider")))));
    }
    private FabricWorkbench() {}
}
