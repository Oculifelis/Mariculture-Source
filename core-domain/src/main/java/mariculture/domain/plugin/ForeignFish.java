package mariculture.domain.plugin;

import java.util.List;
import java.util.function.Predicate;

/**
 * Other mods' raw fish (compat study G1, PLUG-027). Every item in the item tag {@link #TAG} melts into fish oil in the
 * crucible and makes fish meal and sushi, like a Mariculture raw fish. The tag ships with optional entries for the
 * fishing mods we know (Aquaculture 2, Upgrade Aquatic, Alex's Caves, Starcatcher, Tide), so it loads without any of
 * them; data packs add more fish to the tag. Vanilla fish are not in it: cod, salmon, tropical fish and pufferfish are
 * Mariculture species already (ShippedSpecies.rawItem).
 *
 * <p>Legacy only melted Aquaculture's fish (PluginAquaculture.java:80); the tag replaces that one-mod rule.
 */
public final class ForeignFish {
    public static final String TAG = "mariculture:foreign_fish";

    /** Explicit fish ids, each an optional tag entry. Ids that do not exist in a mod's version are skipped. */
    public static final List<String> ITEMS = List.of(
            // Aquaculture 2 (2.5.x-2.9.x), its raw fish in c:foods/raw_fish
            "aquaculture:arapaima", "aquaculture:atlantic_cod", "aquaculture:atlantic_halibut", "aquaculture:atlantic_herring",
            "aquaculture:bayad", "aquaculture:blackfish", "aquaculture:bluegill", "aquaculture:boulti", "aquaculture:brown_shrooma",
            "aquaculture:brown_trout", "aquaculture:capitaine", "aquaculture:carp", "aquaculture:catfish", "aquaculture:gar",
            "aquaculture:largemouth_bass", "aquaculture:minnow", "aquaculture:muskellunge", "aquaculture:pacific_halibut",
            "aquaculture:pink_salmon", "aquaculture:piranha", "aquaculture:pollock", "aquaculture:rainbow_trout",
            "aquaculture:tambaqui", "aquaculture:tuna", "aquaculture:red_shrooma", "aquaculture:smallmouth_bass",
            "aquaculture:synodontis", "aquaculture:red_grouper", "aquaculture:perch",
            // Upgrade Aquatic
            "upgrade_aquatic:lionfish", "upgrade_aquatic:perch", "upgrade_aquatic:pike",
            // Alex's Caves (official 1.20.1 and the 1.21.1/26.x ports keep the ids)
            "alexscaves:lanternfish", "alexscaves:radgill", "alexscaves:tripodfish");

    /** Other mods' own fish tags, each an optional tag entry. Both hold only that mod's fish. */
    public static final List<String> TAGS = List.of("#starcatcher:starcaught_fishable_fish", "#tide:cookable_fish");

    /** Fish meal from one foreign fish (a small Mariculture fish gives 1-2). */
    public static final int FISH_MEAL = 1;
    /** Sushi from one foreign fish ringed by kelp wraps. */
    public static final int SUSHI = 1;

    private ForeignFish() {}

    /** Whether an item is another mod's fish: in the tag and not a Mariculture item (our own fish have their own values). */
    public static boolean is(String item, Predicate<String> hasTag) {
        return !item.startsWith("mariculture:") && hasTag.test(TAG);
    }
}
