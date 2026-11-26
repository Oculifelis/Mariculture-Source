package mariculture.domain.machine;

import java.util.function.BiPredicate;

/**
 * Item tag lookups for rules that run without a world at hand (automation slot checks). Each target installs the
 * loader's lookup at start-up; until then nothing has a tag.
 */
public final class ItemTags {
    private static volatile BiPredicate<String, String> lookup = (item, tag) -> false;

    private ItemTags() {}

    public static void install(BiPredicate<String, String> itemHasTag) {
        lookup = itemHasTag;
    }

    public static boolean has(String item, String tag) {
        return lookup.test(item, tag);
    }
}
