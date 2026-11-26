package mariculture.domain.worldgen;

import mariculture.domain.random.Rng;

/**
 * Coral reefs, kelp forests and ancient sand (world/GenerationHandler.java, decorate/WorldGenReef.java,
 * WorldGenKelp.java, WorldGenAncientSand.java, config core/config/WorldGeneration.java:52-66, 1.3.0).
 *
 * <p>Changes: legacy switched reefs and kelp forests on and off with one static flag shared by every world and
 * chunk, so where they appeared depended on the order chunks generated in. Here a region of 4x4 chunks is a reef
 * (or forest) or not, from the world seed, with the long-run share the legacy start/end chances gave:
 * {@code (1/start) / (1/start + 1/end)}, about one saline chunk in seven for both. Kelp is vanilla kelp, and only the
 * forests are kept: vanilla oceans already grow scattered kelp.
 */
public final class OceanDecor {
    /** Legacy defaults: reef start 256 / end 40, kelp forest start 640 / end 96, chest 1 in 640 with 2-10 items, ancient sand 1 in 2 size 4. */
    public record Settings(boolean reefs, int reefStart, int reefEnd, boolean forests, int forestStart, int forestEnd,
                           int chestOneIn, int chestMin, int chestMax, boolean ancientSand, int ancientSandOneIn, int ancientSandSize) {
        public static final Settings LEGACY = new Settings(true, 256, 40, true, 640, 96, 640, 2, 10, true, 2, 4);
    }

    /** Chunks per region side for reef and forest areas. */
    public static final int REGION = 4;

    private OceanDecor() {}

    /** The share of regions that are active for a start/end chance pair. */
    public static double share(int start, int end) {
        double on = 1.0 / Math.max(1, start), off = 1.0 / Math.max(1, end);
        return on / (on + off);
    }

    /** Whether a region is active, from a seeded number in [0, 1) for that region. */
    public static boolean active(double regionRoll, int start, int end) {
        return regionRoll < share(start, end);
    }

    /** Reef attempts per active chunk (GenerationHandler.java:48). */
    public static final int REEF_ATTEMPTS = 5;

    /** WorldGenReef: the radius of a reef patch, nextInt(5 - 2) + 2. */
    public static int reefRadius(Rng rng) {
        return rng.nextInt(3) + 2;
    }

    /** Kelp attempts per forest chunk (WorldGenKelp.java:22). */
    public static final int FOREST_ATTEMPTS = 128;

    /** WorldGenKelp: forest kelp keeps growing while nextInt(5) is not zero (the loop re-rolled its bound each step). */
    public static int forestKelpHeight(Rng rng) {
        int h = 0;
        while (h < 24 && rng.nextInt(5) != 0) h++;
        return Math.max(1, h);
    }

    /** One attempt in {@code chestOneIn} is an ocean chest instead of kelp, on sand only. */
    public static boolean chest(Settings s, Rng rng) {
        return rng.nextInt(Math.max(1, s.chestOneIn())) == 0;
    }

    /** Items in an ocean chest: nextInt(max - min) + min. */
    public static int chestItems(Settings s, Rng rng) {
        return rng.nextInt(Math.max(1, s.chestMax() - s.chestMin())) + s.chestMin();
    }

    /** WorldGenAncientSand: radius nextInt(size - 2) + 2, two blocks up and down from the floor. */
    public static int ancientSandRadius(Settings s, Rng rng) {
        return rng.nextInt(Math.max(1, s.ancientSandSize() - 2)) + 2;
    }

    public static final int ANCIENT_SAND_HALF_HEIGHT = 2;

    /** One ocean chest entry: item, stack size range and weight (WeightedRandomChestContent). */
    public record ChestLoot(String item, int min, int max, int weight) {}

    /**
     * WorldPlus.addOceanChestLoot (WorldPlus.java:131-156), with the Fishery rods since that module is always on here.
     * The cocoa beans were dye meta 3; the skulls a skeleton and a wither skeleton skull.
     */
    public static final java.util.List<ChestLoot> OCEAN_CHEST = java.util.List.of(
            new ChestLoot("minecraft:cocoa_beans", 3, 7, 17),
            new ChestLoot("minecraft:bone", 5, 10, 25),
            new ChestLoot("minecraft:skeleton_skull", 5, 5, 5),
            new ChestLoot("minecraft:wither_skeleton_skull", 2, 3, 3),
            new ChestLoot("minecraft:tnt", 2, 4, 4),
            new ChestLoot("minecraft:paper", 2, 3, 20),
            new ChestLoot("minecraft:book", 2, 3, 15),
            new ChestLoot("minecraft:experience_bottle", 5, 10, 20),
            new ChestLoot("minecraft:writable_book", 1, 4, 4),
            new ChestLoot("minecraft:map", 1, 1, 5),
            new ChestLoot("minecraft:gold_ingot", 10, 20, 10),
            new ChestLoot("minecraft:diamond", 1, 2, 3),
            new ChestLoot("mariculture:titanium_ingot", 1, 3, 4),
            new ChestLoot("mariculture:reed_fishing_rod", 1, 2, 6),
            new ChestLoot("mariculture:wooden_fishing_rod", 1, 2, 4),
            new ChestLoot("mariculture:titanium_fishing_rod", 1, 1, 2));
}
