package zoltan.spellcraft.runtime;

public record ManifestationResult(Status status, int applications) {
    public enum Status { APPLIED, NO_TARGET, NO_SOURCE_FORM, UNSUPPORTED_FORM, UNSUPPORTED_OPERATION, INVALID_CASTER, INSUFFICIENT_SOURCE, INCOMPATIBLE_VESSEL, COOLDOWN }
}
