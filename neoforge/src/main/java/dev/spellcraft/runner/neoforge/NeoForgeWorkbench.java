package dev.spellcraft.runner.neoforge;

import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import zoltan.spellcraft.workbench.*;

public final class NeoForgeWorkbench {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("spellcraft");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("spellcraft");
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "spellcraft");
    public static final DeferredBlock<ArcaneWorkbenchBlock> BLOCK = BLOCKS.registerBlock("arcane_workbench", ArcaneWorkbenchBlock::new,
        properties -> properties.strength(2.5f).sound(SoundType.STONE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneWorkbenchBlockEntity>> TYPE = TYPES.register("arcane_workbench",
        () -> new BlockEntityType<>(ArcaneWorkbenchBlockEntity::new, Set.of(BLOCK.get())));
    static {
        ITEMS.registerSimpleBlockItem(BLOCK);
        ITEMS.registerItem("divider", DividerItem::new, properties -> properties.stacksTo(1));
    }
    public static void register(IEventBus bus) {
        WorkbenchContent.entityType = TYPE;
        BLOCKS.register(bus); ITEMS.register(bus); TYPES.register(bus);
    }
    private NeoForgeWorkbench() {}
}
