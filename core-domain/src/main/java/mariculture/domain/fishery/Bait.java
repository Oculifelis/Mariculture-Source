package mariculture.domain.fishery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mariculture.domain.fish.Species;
import mariculture.domain.plugin.Plugins;
import mariculture.domain.random.Rng;

/**
 * Bait, fish food and sifter tables (fishery/Fishery.java addBait 309-362 and addFishRecipes 383-393,
 * fishery/SifterHandler, api/fishery/RecipeSifter).
 */
public final class Bait {
    public record Kind(String id, String englishName, int legacyMeta, int quality) {}

    /** Bait items in legacy BaitMeta order; quality is the catch rate given to FishingHandler.addBait. */
    public static final List<Kind> KINDS = List.of(
            new Kind("bait_worm", "Bait Worm", 0, 75),
            new Kind("bait_ant", "Ant Bait", 1, 10),
            new Kind("bait_maggot", "Maggot Bait", 2, 55),
            new Kind("bait_grasshopper", "Grasshopper Bait", 3, 40),
            new Kind("bait_bee", "Bee Bait", 4, 70));

    /** Bait quality of anything usable as bait (bread and raw minnow count too). */
    public static final Map<String, Integer> QUALITY = Map.of(
            "mariculture:bait_ant", 10, "minecraft:bread", 30, "mariculture:bait_grasshopper", 40, "mariculture:bait_maggot", 55,
            "mariculture:bait_bee", 70, "mariculture:bait_worm", 75, "mariculture:raw_minnow", 100);

    /** Which rod tiers may use each bait (FishingHandler.addBaitForQuality). */
    public static final Map<String, Set<Species.Rod>> RODS = Map.of(
            "mariculture:bait_ant", Set.of(Species.Rod.DIRE, Species.Rod.OLD, Species.Rod.FLUX),
            "minecraft:bread", Set.of(Species.Rod.DIRE, Species.Rod.OLD, Species.Rod.FLUX),
            "mariculture:bait_grasshopper", Set.of(Species.Rod.OLD, Species.Rod.GOOD, Species.Rod.FLUX),
            "mariculture:bait_maggot", Set.of(Species.Rod.GOOD, Species.Rod.FLUX),
            "mariculture:bait_worm", Set.of(Species.Rod.GOOD, Species.Rod.SUPER, Species.Rod.FLUX),
            "mariculture:bait_bee", Set.of(Species.Rod.SUPER, Species.Rod.FLUX),
            "mariculture:raw_minnow", Set.of(Species.Rod.SUPER, Species.Rod.FLUX));

    /** Fish food value of an item put into a feeder (FishFoodHandler.addFishFood). */
    public static final Map<String, Integer> FISH_FOOD = Map.of(
            "mariculture:bait_ant", 1, "mariculture:bait_maggot", 2, "mariculture:bait_grasshopper", 2, "minecraft:bread", 8,
            "mariculture:bait_worm", 3, "mariculture:bait_bee", 3, "mariculture:fish_meal", 12);

    /** A sifter output: {@code input} is an item id or "#tag"; chance is percent, count uniform in [min, max]. */
    public record SifterRecipe(String input, String bait, int min, int max, int chance) {}

    public static final List<SifterRecipe> SIFTER = new ArrayList<>();

    static {
        String[] dirts = {"minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol"};
        // Blocks.tallgrass meta 1 was "grass"; it is minecraft:grass on 1.20.1 and minecraft:short_grass from 1.20.3.
        String[] tallGrass = {"minecraft:grass", "minecraft:short_grass"};
        String[] redFlowers = {"minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
                "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy"};
        String[] doublePlants = {"#minecraft:tall_flowers", "minecraft:tall_grass", "minecraft:large_fern"};
        for (String d : dirts) sift(d, "bait_ant", 2, 3, 40);
        sift("minecraft:grass_block", "bait_ant", 1, 2, 50);
        sift("minecraft:grass_block", "bait_grasshopper", 1, 2, 35);
        sift("#minecraft:saplings", "bait_grasshopper", 2, 3, 35);
        sift("#minecraft:leaves", "bait_grasshopper", 1, 2, 15);
        for (String g : tallGrass) sift(g, "bait_grasshopper", 4, 5, 45);
        sift("minecraft:fern", "bait_grasshopper", 2, 3, 40);
        sift("minecraft:dead_bush", "bait_grasshopper", 1, 2, 10);
        sift("minecraft:dandelion", "bait_grasshopper", 2, 5, 20);
        for (String f : redFlowers) sift(f, "bait_grasshopper", 3, 4, 25);
        sift("minecraft:rotten_flesh", "bait_maggot", 1, 2, 60);
        sift("minecraft:beef", "bait_maggot", 14, 22, 20);
        sift("minecraft:chicken", "bait_maggot", 6, 14, 30);
        sift("minecraft:porkchop", "bait_maggot", 10, 18, 25);
        sift("minecraft:zombie_head", "bait_maggot", 8, 15, 80);
        sift("minecraft:dandelion", "bait_bee", 2, 3, 25);
        for (String f : redFlowers) sift(f, "bait_bee", 1, 2, 30);
        for (String p : doublePlants) sift(p, "bait_bee", 3, 5, 30);
        sift("minecraft:apple", "bait_worm", 1, 3, 15);
        for (String d : dirts) sift(d, "bait_worm", 2, 3, 10);
        sift("minecraft:grass_block", "bait_worm", 3, 5, 20);
    }

    private Bait() {}

    private static void sift(String input, String bait, int min, int max, int chance) {
        SIFTER.add(new SifterRecipe(input, "mariculture:" + bait, min, max, chance));
    }

    /** Food points from eating bait: max(1, quality / 100 * 4) (ItemBait.onEaten). */
    public static int hunger(int quality) {
        return Math.max(1, (int) (quality / 100.0 * 4.0));
    }

    public static int quality(String item) {
        Integer q = QUALITY.get(item);
        if (q == null) q = Plugins.baitQuality(item);
        return q == null ? 0 : q;
    }

    public static boolean canUse(Species.Rod rod, String item) {
        Set<Species.Rod> rods = RODS.get(item);
        return rods != null && rods.contains(rod) || Plugins.baitUsable(rod, item);
    }

    /**
     * FishingHandler.getBait: bait is taken from the hotbar slot left of the rod, else the one to its right, if the
     * rod's tier may use it. Returns the slot, or -1.
     */
    public static int findSlot(int selected, java.util.function.IntFunction<String> itemInHotbar, Species.Rod rod) {
        if (selected > 0 && canUse(rod, itemInHotbar.apply(selected - 1))) return selected - 1;
        if (selected < 8 && canUse(rod, itemInHotbar.apply(selected + 1))) return selected + 1;
        return -1;
    }

    /**
     * Bite-time reduction in ticks for a bait quality. Legacy EntityHook drew the wait from 100..900 ticks with bait
     * (1500 without) and took off lure * 125 * quality / 10; vanilla's hook only takes a flat reduction, so bait
     * counts as one Lure level per 25 quality (worm: three levels, 300 ticks).
     */
    public static int lureTicks(int quality) {
        return quality / 25 * 100;
    }

    /** Recipes that take this item (matched by id or by a tag the item has). */
    /** Every sifter row: the table plus active plugins' rows. */
    public static List<SifterRecipe> sifterAll() {
        List<SifterRecipe> out = new ArrayList<>(SIFTER);
        out.addAll(Plugins.sifting());
        return out;
    }

    /** Fish food value of an item in a feeder: the table, then active plugins' items; 0 for none. */
    public static int fishFood(String item) {
        Integer v = FISH_FOOD.get(item);
        return v != null ? v : Plugins.fishFood(item);
    }

    public static List<SifterRecipe> sifterRecipes(String item, java.util.function.Predicate<String> hasTag) {
        List<SifterRecipe> out = new ArrayList<>();
        for (SifterRecipe r : sifterAll()) {
            if (r.input().startsWith("#") ? hasTag.test(r.input().substring(1)) : r.input().equals(item)) out.add(r);
        }
        return out;
    }

    /** One sift of one item (TileSifter.process): each matching recipe rolls its chance. Returns bait id to count. */
    public static Map<String, Integer> sift(List<SifterRecipe> recipes, Rng rng) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (SifterRecipe r : recipes) {
            if (rng.nextInt(100) < r.chance()) {
                int n = r.min() + rng.nextInt(r.max() - r.min() + 1);
                out.merge(r.bait(), n, Integer::sum);
            }
        }
        return out;
    }
}
