package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.content.Ingredient;
import mariculture.domain.content.Recipe;

/**
 * Item tag entries naming other mods (compat study 2026-11-15, PLUG-027 to PLUG-030). Every entry here is written
 * optional ({@code "required": false}), so a tag loads whether or not the other mod is installed; packs add or remove
 * entries in their own tag files. Tags in another mod's namespace are only read by that mod.
 */
public final class CompatTags {
    /** One tag: entries Mariculture always has, then optional entries from other mods. */
    public record Entry(String tag, List<String> required, List<String> optional) {}

    /** Kelp that crafts kelp wraps (U1). Legacy coral kelp is vanilla kelp now; Upgrade Aquatic adds four more. */
    public static final String KELP = "mariculture:kelp";

    /** A5: an item tag that is empty without Alex's Caves, so its cave fish switch off with it. */
    public static final String ALEXS_CAVES_PRESENT = "mariculture:present/alexscaves";

    /** An item tag that is empty without Thaumaturge, so the Aquamancy fish switch off with it. */
    public static final String THAUMATURGE_PRESENT = "mariculture:present/thaumaturge";

    /**
     * A5: fluids a Mariculture hook floats on and fish bite in, besides water and Nether lava, and the fluid tags
     * cave fish name in {@code catch.fluid}. Both of Alex's Caves' spellings of the flowing fluids are listed.
     */
    public static final String HOOK_FLOATS_ON = "mariculture:hook_floats_on";
    public static final String ACID = "mariculture:fishing/acid";
    public static final String SODA = "mariculture:fishing/soda";
    private static final List<String> AC_ACID = List.of("alexscaves:acid", "alexscaves:acid_flowing", "alexscaves:flowing_acid");
    private static final List<String> AC_SODA = List.of("alexscaves:purple_soda", "alexscaves:purple_soda_flowing", "alexscaves:flowing_purple_soda");

    public static final List<Entry> FLUID_TAGS = List.of(
            new Entry(ACID, List.of(), AC_ACID),
            new Entry(SODA, List.of(), AC_SODA),
            new Entry(HOOK_FLOATS_ON, List.of("#" + ACID, "#" + SODA), List.of()));

    public static final List<Entry> ITEM_TAGS = List.of(
            new Entry(ForeignFish.TAG, List.of(), concat(ForeignFish.ITEMS, ForeignFish.TAGS)),
            new Entry(KELP, List.of(), List.of("upgrade_aquatic:tongue_kelp", "upgrade_aquatic:thorny_kelp",
                    "upgrade_aquatic:ochre_kelp", "upgrade_aquatic:polar_kelp")),
            // A4: Alex's Caves pearls work wherever a Mariculture pearl does in crafting.
            new Entry("c:gems/pearl", List.of(), List.of("alexscaves:pearl")),
            new Entry("mariculture:pearls", List.of(), List.of("alexscaves:pearl")),
            // A5: has items only while Alex's Caves is installed; its cave fish name it in requires_item_tag.
            new Entry(ALEXS_CAVES_PRESENT, List.of(), List.of("alexscaves:pearl")),
            new Entry(THAUMATURGE_PRESENT, List.of(), List.of("thaumaturge:salis_mundus")),
            // S1: Stardew Fishing plays its minigame when the catch is in starts_minigame, and lets bobbers go on
            // rods in modifiable_rods. The fish_behaviors.json difficulty table is one file per pack that replaces
            // Stardew's own, so Mariculture fish use its default behaviour instead of shipping it.
            new Entry("stardew_fishing:starts_minigame", List.of("#mariculture:live_fish", "#mariculture:raw_fish"), List.of()),
            new Entry("stardew_fishing:modifiable_rods", List.of("#mariculture:fishing_rods"), List.of()));

    /**
     * G1: one recipe set for every fish in {@link ForeignFish#TAG}, written with a condition that the tag is not empty.
     * Fish meal is shapeless; sushi rings the fish with kelp wraps like RecipeHelper.addFishSushi (RecipeHelper.java:262).
     */
    public static List<Recipe> foreignFishRecipes() {
        Ingredient fish = Ingredient.tag(ForeignFish.TAG);
        return List.of(
                new Recipe.Shapeless("compat/fish_meal_from_foreign_fish", List.of(fish), "fish_meal", ForeignFish.FISH_MEAL,
                        "RecipeHelper.java:274 on the foreign fish tag"),
                new Recipe.Shaped("compat/sushi_from_foreign_fish", List.of(" K ", "KFK", " K "),
                        java.util.Map.of('K', Ingredient.item("kelp_wrap"), 'F', fish), "sushi", ForeignFish.SUSHI,
                        "RecipeHelper.java:262 on the foreign fish tag"));
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    private CompatTags() {}
}
