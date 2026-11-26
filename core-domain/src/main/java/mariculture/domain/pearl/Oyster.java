package mariculture.domain.pearl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import mariculture.domain.random.Rng;

/**
 * Pearl oyster rules from 1.3.0: {@code core/blocks/BlockWater.java} (oyster meta), {@code core/tile/TileOyster.java},
 * {@code core/handlers/PearlGenHandler.java}, {@code WorldGenHandler.generateOyster} and the defaults in
 * {@code core/config/WorldGeneration.java:48-51} and {@code core/config/Gardening.java:28-29}.
 *
 * <p>An oyster holds one item. Plain sand put inside turns, with chance {@code 1 / growthChance} per random tick
 * while water is above, into a weighted random pearl (or sand, red sand, or an ender pearl); in the End it always
 * becomes an ender pearl, on soul sand it becomes soul sand, on red sand it becomes red sand, and on a pearl block
 * it becomes a pearl of that block's colour. Items are named by registry id so the rules stay platform-neutral.
 */
public final class Oyster {
    public static final String SAND = "minecraft:sand";
    public static final String RED_SAND = "minecraft:red_sand";
    public static final String SOUL_SAND = "minecraft:soul_sand";
    public static final String ENDER_PEARL = "minecraft:ender_pearl";

    public record Settings(boolean generate, int attemptsPerChunk, int oneInPerAttempt, int naturalPearlOneIn,
                           int growthOneIn, boolean enderPearls) {
        /** Legacy defaults: 3 attempts, 1 in 12 each, 1 in 3 natural pearls, 1 in 32 growth, ender pearls on. */
        public static final Settings LEGACY = new Settings(true, 3, 12, 3, 32, true);

        public Settings {
            if (attemptsPerChunk < 0) throw new IllegalArgumentException("attempts must not be negative");
            if (oneInPerAttempt < 1 || naturalPearlOneIn < 1 || growthOneIn < 1) {
                throw new IllegalArgumentException("chances are '1 in n' with n >= 1");
            }
        }
    }

    /** What is under the oyster, as far as growth cares. */
    public enum Below { SOUL_SAND, RED_SAND, PEARL_BLOCK, OTHER }

    /** One weighted outcome of PearlGenHandler.getRandomPearl. */
    public record Weighted(String item, int weight) {}

    /** Legacy registration order and weights (Core.java:320-335); order matters for the weighted draw. */
    private static final List<Weighted> LEGACY_TABLE = List.of(
            new Weighted(pearl(PearlColor.BLACK), 14), new Weighted(pearl(PearlColor.BLUE), 18),
            new Weighted(pearl(PearlColor.BROWN), 25), new Weighted(pearl(PearlColor.GOLD), 5),
            new Weighted(pearl(PearlColor.GREEN), 18), new Weighted(pearl(PearlColor.ORANGE), 22),
            new Weighted(pearl(PearlColor.PINK), 20), new Weighted(pearl(PearlColor.PURPLE), 18),
            new Weighted(pearl(PearlColor.RED), 18), new Weighted(pearl(PearlColor.SILVER), 6),
            new Weighted(pearl(PearlColor.WHITE), 8), new Weighted(pearl(PearlColor.YELLOW), 7),
            new Weighted(SAND, 30), new Weighted(RED_SAND, 15));
    private static final Weighted ENDER = new Weighted(ENDER_PEARL, 2);

    /** A placement chosen by world generation, relative to the chunk's minimum corner. */
    public record Placement(int dx, int dz, int facing, String contents) {}

    private Oyster() {}

    public static String pearl(PearlColor color) {
        return "mariculture:" + color.id() + "_pearl";
    }

    /** The colour of a Mariculture pearl block id such as {@code mariculture:gold_pearl_block}, or null. */
    public static PearlColor pearlBlockColor(String blockId) {
        for (PearlColor c : PearlColor.values()) {
            if (("mariculture:" + c.id() + "_pearl_block").equals(blockId)) return c;
        }
        return null;
    }

    public static List<Weighted> table(Settings settings) {
        boolean mana = mariculture.domain.plugin.Plugins.active(mariculture.domain.plugin.Plugins.BOTANIA);
        if (!settings.enderPearls() && !mana) return LEGACY_TABLE;
        List<Weighted> all = new ArrayList<>(LEGACY_TABLE);
        if (settings.enderPearls()) all.add(ENDER);
        if (mana) all.add(new Weighted(mariculture.domain.plugin.Plugins.MANA_PEARL, mariculture.domain.plugin.Plugins.MANA_PEARL_WEIGHT));
        return List.copyOf(all);
    }

    /** {@code WeightedRandom.getRandomItem}: one draw of {@code nextInt(total)}, walked in registration order. */
    public static String randomPearl(Settings settings, Rng rng) {
        List<Weighted> table = table(settings);
        int total = table.stream().mapToInt(Weighted::weight).sum();
        int r = rng.nextInt(total);
        for (Weighted w : table) {
            r -= w.weight();
            if (r < 0) return w.item();
        }
        throw new IllegalStateException("weighted draw fell off the table");
    }

    /** Only one item of plain sand can be put in, and only into an empty oyster (TileOyster.canInsertItem). */
    public static boolean accepts(String contents, String offered) {
        return contents == null && SAND.equals(offered);
    }

    /**
     * One random tick (BlockWater.updateTick). Returns the new contents, or {@code null} when nothing changes.
     *
     * @param pearlBlockColor colour of the pearl block below, used only when {@code below == PEARL_BLOCK}
     */
    public static String randomTick(Settings settings, String contents, boolean waterAbove, boolean inTheEnd,
                                    Below below, PearlColor pearlBlockColor, Rng rng) {
        if (!SAND.equals(contents) || !waterAbove) return null;
        if (rng.nextInt(settings.growthOneIn()) != 0) return null;
        if (inTheEnd) return ENDER_PEARL;
        return switch (below) {
            case SOUL_SAND -> SOUL_SAND;
            case RED_SAND -> RED_SAND;
            case PEARL_BLOCK -> pearl(Objects.requireNonNull(pearlBlockColor, "pearl block colour"));
            case OTHER -> randomPearl(settings, rng);
        };
    }

    /**
     * World generation for one chunk. Legacy aimed at {@code chunkOrigin - 8 + nextInt(4)}, which is outside the chunk
     * being decorated (cascading generation) and packs every oyster of a chunk into one 4x4 patch (BUG-051); here
     * positions cover the chunk's own 16x16 columns. The caller still checks salinity, water and the floor.
     */
    public static List<Placement> generate(Settings settings, Rng rng) {
        List<Placement> out = new ArrayList<>();
        if (!settings.generate()) return out;
        for (int i = 0; i < settings.attemptsPerChunk(); i++) {
            if (rng.nextInt(settings.oneInPerAttempt()) != 0) continue;
            int dx = rng.nextInt(16);
            int dz = rng.nextInt(16);
            int facing = rng.nextInt(4);
            String contents = rng.nextInt(settings.naturalPearlOneIn()) == 0 ? randomPearl(settings, rng) : null;
            out.add(new Placement(dx, dz, facing, contents));
        }
        return out;
    }
}
