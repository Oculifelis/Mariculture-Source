package mariculture.domain.content;

/**
 * Legacy Forge harvest levels (0 wood, 1 stone, 2 iron, 3 diamond) expressed as the modern
 * "needs_*_tool" block tag a target should add, if any.
 */
public enum HarvestLevel {
    ANY(null), STONE("needs_stone_tool"), IRON("needs_iron_tool"), DIAMOND("needs_diamond_tool");

    private final String vanillaTag;

    HarvestLevel(String vanillaTag) {
        this.vanillaTag = vanillaTag;
    }

    /** Vanilla block tag path, or null when any pickaxe works. */
    public String vanillaTag() {
        return vanillaTag;
    }

    public static HarvestLevel ofLegacy(int level) {
        return switch (level) {
            case 0 -> ANY;
            case 1 -> STONE;
            case 2 -> IRON;
            case 3 -> DIAMOND;
            default -> throw new IllegalArgumentException("Unknown legacy harvest level " + level);
        };
    }
}
