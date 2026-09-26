package zoltan.spellcraft.material;

import dev.spellcraft.domain.form.FormParticipation;
import dev.spellcraft.domain.form.Forms;
import dev.spellcraft.domain.material.MaterialId;
import dev.spellcraft.domain.material.MaterialProfile;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Bootstrap data boundary; a datapack reload can replace the immutable correspondence map. */
public final class MaterialProfileResolver {
    private final Map<MaterialId, List<FormParticipation>> correspondences;
    public MaterialProfileResolver(Map<MaterialId, List<FormParticipation>> correspondences) {
        var copy = new java.util.HashMap<MaterialId, List<FormParticipation>>();
        correspondences.forEach((id, forms) -> copy.put(id, new MaterialProfile(id, forms).forms()));
        this.correspondences = Map.copyOf(copy);
    }
    public MaterialProfile resolve(ItemStack stack) {
        if (stack.isEmpty()) return new MaterialProfile(new MaterialId("minecraft:air"), List.of());
        return resolve(stack.getItem());
    }
    public MaterialProfile resolve(net.minecraft.world.item.Item item) {
        var id = new MaterialId(BuiltInRegistries.ITEM.getKey(item).toString());
        return new MaterialProfile(id, correspondences.getOrDefault(id, List.of()));
    }
    public static MaterialProfileResolver bootstrap() {
        return new MaterialProfileResolver(Map.of(
            new MaterialId("minecraft:blaze_powder"), List.of(new FormParticipation(Forms.HEAT, 1)),
            new MaterialId("minecraft:feather"), List.of(new FormParticipation(Forms.MOTION, 1))));
    }
}
