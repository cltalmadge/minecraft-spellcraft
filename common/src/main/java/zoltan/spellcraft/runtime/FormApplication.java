package zoltan.spellcraft.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record FormApplication(LivingEntity target, Vec3 direction, double intensity) {}
