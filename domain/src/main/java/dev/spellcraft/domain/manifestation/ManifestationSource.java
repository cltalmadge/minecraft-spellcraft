package dev.spellcraft.domain.manifestation;


/** Capability supplied per cast, independent of the object preserving the program. */
public interface ManifestationSource {
    boolean supports(SpellBurden burden);
}
