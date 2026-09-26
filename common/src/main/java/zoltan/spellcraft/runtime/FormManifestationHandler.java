package zoltan.spellcraft.runtime;

import dev.spellcraft.domain.form.FormId;

public interface FormManifestationHandler {
    FormId form();
    void manifest(MinecraftSpellContext context, FormApplication application);
}
