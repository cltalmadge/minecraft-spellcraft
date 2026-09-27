package zoltan.spellcraft.workbench;

import net.minecraft.util.StringRepresentable;

public enum WorkbenchPart implements StringRepresentable {
    FRONT, BACK;
    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
