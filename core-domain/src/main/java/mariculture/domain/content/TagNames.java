package mariculture.domain.content;

/**
 * Common (cross-mod) tag names. Content declares them in the modern "c:" convention; Forge 1.20.1
 * still uses the "forge:" namespace for the same paths.
 */
public final class TagNames {
    private TagNames() {}

    public static String forLoader(String tag, boolean legacyForgeNamespace) {
        if (legacyForgeNamespace && tag.startsWith("c:")) return "forge:" + tag.substring(2);
        return tag;
    }
}
