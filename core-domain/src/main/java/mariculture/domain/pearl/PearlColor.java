package mariculture.domain.pearl;

/** Pearl colours in legacy metadata order (mariculture.core.lib.PearlColor, 1.3.0). */
public enum PearlColor {
    WHITE, GREEN, YELLOW, ORANGE, RED, GOLD, BROWN, PURPLE, BLUE, BLACK, PINK, SILVER;

    private static final PearlColor[] VALUES = values();

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static PearlColor byLegacyMeta(int meta) {
        if (meta < 0 || meta >= VALUES.length) {
            throw new IllegalArgumentException("Unknown pearl colour metadata " + meta);
        }
        return VALUES[meta];
    }

    public static int count() {
        return VALUES.length;
    }
}
