package mariculture.domain.machine;

/** core/gui/feature/FeatureRedstone.RedstoneMode: DISABLED ignores redstone, LOW works unpowered, HIGH only powered. */
public enum RedstoneMode {
    DISABLED, LOW, HIGH;

    public boolean canWork(boolean powered) {
        return switch (this) {
            case DISABLED -> true;
            case LOW -> !powered;
            case HIGH -> powered;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : LOW;
    }
}
