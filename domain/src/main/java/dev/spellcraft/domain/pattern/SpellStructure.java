package dev.spellcraft.domain.pattern;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Shared semantic data in patterns and programs. No physical working or world coordinates. */
public record SpellStructure(List<SpellLocus> loci, List<LocusRelation> relations) {
    public SpellStructure {
        loci = loci.stream().sorted(Comparator.comparing(SpellLocus::id)).toList();
        relations = relations.stream().sorted(Comparator.comparing(LocusRelation::from)
            .thenComparing(LocusRelation::to)).toList();
        var ids = new HashSet<LocusId>();
        for (var locus : loci) {
            if (!ids.add(locus.id())) throw new IllegalArgumentException("Duplicate locus id");
        }
        if (loci.isEmpty() || loci.stream().allMatch(l -> l.forms().terms().isEmpty()))
            throw new IllegalArgumentException("No active Form");
        if (new HashSet<>(relations).size() != relations.size()) throw new IllegalArgumentException("Duplicate relation");
        for (var relation : relations) {
            if (!ids.contains(relation.from()) || !ids.contains(relation.to()))
                throw new IllegalArgumentException("Undeclared relation endpoint");
        }
    }

    public List<SpellLocus> withRole(LocusRole role) {
        return loci.stream().filter(l -> l.role() == role).toList();
    }

    public SpellLocus single(LocusRole role) {
        var matching = withRole(role);
        if (matching.size() != 1) throw new IllegalArgumentException("Expected one " + role);
        return matching.getFirst();
    }

    /** Validate the small relational grammar separately from physical topology recognition. */
    public void validate(MagicalOperation operation, Geometry geometry) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(geometry);
        Geometry topology = geometry instanceof Geometry.Enclosure enclosure ? enclosure.interior() : geometry;
        switch (operation) {
            case CONCENTRATE -> {
                var focus = single(LocusRole.FOCUS);
                if (focus.forms().terms().isEmpty()) throw new IllegalArgumentException("Inactive focus");
                if (topology instanceof Geometry.Point) {
                    require(loci.size() == 1 && relations.isEmpty(), "Point needs one focus");
                } else if (topology instanceof Geometry.Radial radial) {
                    require(loci.size() == 5 && withRole(LocusRole.ANCHOR).size() == 4, "Radial needs four anchors");
                    validateSpokes(focus, radial.outward());
                } else throw new IllegalArgumentException("Unsupported concentration geometry");
            }
            case TRANSFER -> {
                var source = single(LocusRole.SOURCE);
                var recipient = single(LocusRole.RECIPIENT);
                require(loci.size() == 2 && topology instanceof Geometry.Line, "Transfer needs a directed pair");
                require(relations.equals(List.of(new LocusRelation(source.id(), recipient.id()))), "Transfer direction disagrees with roles");
            }
            case MEDIATE -> {
                var source = single(LocusRole.SOURCE);
                var mediator = single(LocusRole.MEDIATOR);
                var recipient = single(LocusRole.RECIPIENT);
                require(loci.size() == 3 && topology instanceof Geometry.Line, "Mediation needs three loci");
                require(relations.size() == 2 && relations.contains(new LocusRelation(source.id(), mediator.id()))
                    && relations.contains(new LocusRelation(mediator.id(), recipient.id())), "Invalid mediated chain");
            }
            case STABILIZE -> {
                var center = single(LocusRole.ANCHOR);
                require(loci.size() == 5 && withRole(LocusRole.STABILIZER).size() == 4
                    && topology instanceof Geometry.Intersection, "Stabilization needs a center and four stabilizers");
                validateSpokes(center, relations.stream().allMatch(r -> r.from().equals(center.id())));
            }
        }
    }

    public boolean hasEquivalentStabilizers() {
        var stabilizers = withRole(LocusRole.STABILIZER);
        return stabilizers.size() == 4 && !stabilizers.getFirst().forms().terms().isEmpty()
            && stabilizers.stream().allMatch(l -> l.forms().equals(stabilizers.getFirst().forms()));
    }

    private void validateSpokes(SpellLocus center, boolean outward) {
        var expected = loci.stream().filter(l -> !l.id().equals(center.id()))
            .map(l -> outward ? new LocusRelation(center.id(), l.id()) : new LocusRelation(l.id(), center.id()))
            .collect(java.util.stream.Collectors.toSet());
        require(relations.size() == 4 && new HashSet<>(relations).equals(expected), "Invalid spoke direction or endpoints");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
