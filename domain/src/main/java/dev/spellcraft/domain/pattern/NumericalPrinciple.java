package dev.spellcraft.domain.pattern;


public enum NumericalPrinciple {
    MONAD, DYAD, TRIAD, TETRAD;
    public MagicalOperation operation() {
        return switch (this) {
            case MONAD -> MagicalOperation.CONCENTRATE;
            case DYAD -> MagicalOperation.TRANSFER;
            case TRIAD -> MagicalOperation.MEDIATE;
            case TETRAD -> MagicalOperation.STABILIZE;
        };
    }
}
