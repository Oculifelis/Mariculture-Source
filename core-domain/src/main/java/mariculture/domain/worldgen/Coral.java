package mariculture.domain.worldgen;

import java.util.HashMap;
import java.util.Map;

import mariculture.domain.random.Rng;

/**
 * Mariculture coral (world/BlockCoral.java, core/lib/CoralMeta.java, 1.3.0). Eleven colours, each as dried coral
 * (plantStatic: generated and crafted, does nothing) and living coral (plantGrowable: made by soaking dried coral in
 * a vat, spreads on coral rock). Both drop the dried coral and break when the block below cannot hold them.
 *
 * <p>Kelp (metas 0 and 1) is vanilla kelp in the port (WORLD-001), so the kelp growth and kelp moss spreading
 * settings are gone with it.
 */
public final class Coral {
    /** The colours, in legacy meta order (2..12), with the dye each crushes into and the legacy display name. */
    public enum Colour {
        PINK("minecraft:pink_dye", "Pink Coral"),
        RED("minecraft:red_dye", "Red Coral"),
        LIGHT_BLUE("minecraft:light_blue_dye", "Blue Coral"),
        MAGENTA("minecraft:magenta_dye", "Candycane Coral"),
        BROWN("minecraft:brown_dye", "Cucumber Coral"),
        YELLOW("minecraft:yellow_dye", "Brain Coral"),
        ORANGE("minecraft:orange_dye", "Orange Coral"),
        PURPLE("minecraft:purple_dye", "Purple Coral"),
        GREY("minecraft:gray_dye", "Partially Bleached Coral"),
        LIGHT_GREY("minecraft:light_gray_dye", "Semi-Bleached Coral"),
        WHITE("minecraft:white_dye", "Bleached Coral");

        public final String dye, name;

        Colour(String dye, String name) {
            this.dye = dye;
            this.name = name;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The dried coral block's path, e.g. {@code pink_coral}. */
        public String dried() {
            return id() + "_coral";
        }

        /** The living coral block's path, e.g. {@code live_pink_coral}. */
        public String living() {
            return "live_" + id() + "_coral";
        }
    }

    /** Settings (core/config/Gardening.java:22-23): spread on, one in 12 random ticks. */
    public record Settings(boolean spread, int spreadOneIn) {
        public static final Settings LEGACY = new Settings(true, 12);
    }

    /** What the block below a coral is, for canSustainCoral. */
    public enum Ground { SAND, MARICULTURE_SAND, LIMESTONE, COBBLESTONE, MOSSY_COBBLESTONE, CORAL_ROCK, OTHER }

    private Coral() {}

    /** BlockCoral.canSustainCoral (BlockCoral.java:143-145). */
    public static boolean canSustain(Ground below) {
        return below != Ground.OTHER;
    }

    /** Colours a reef places: metas 2..9 (WorldGenReef.java:73, rand.nextInt(COUNT - 5) + 2), so no bleached coral. */
    public static Colour reefColour(Rng rng) {
        return Colour.values()[rng.nextInt(8)];
    }

    /** One in 25 reef plants is a sponge instead (WorldGenReef.java:72-76). */
    public static boolean reefSponge(Rng rng) {
        return rng.nextInt(25) == 0;
    }

    /** A random offset for spreading, per axis: x + 1 + nextInt(4) - 2 - nextInt(2), and y + nextInt(3) - 1. */
    public static int[] spreadOffset(Rng rng) {
        int dx = 1 + rng.nextInt(4) - 2 - rng.nextInt(2);
        int dy = rng.nextInt(3) - 1;
        int dz = 1 + rng.nextInt(4) - 2 - rng.nextInt(2);
        return new int[] {dx, dy, dz};
    }

    private static final Map<String, Colour[]> OUTCOMES = new HashMap<>();

    static {
        // BlockCoral.java:230-244
        out(Colour.RED, Colour.WHITE, Colour.PINK, Colour.YELLOW);
        out(Colour.RED, Colour.LIGHT_BLUE, Colour.PURPLE, Colour.MAGENTA);
        out(Colour.RED, Colour.YELLOW, Colour.ORANGE, Colour.BROWN);
        out(Colour.WHITE, Colour.GREY, Colour.LIGHT_GREY);
        out(Colour.WHITE, Colour.PURPLE, Colour.LIGHT_BLUE, Colour.GREY, Colour.LIGHT_GREY);
        out(Colour.WHITE, Colour.LIGHT_BLUE, Colour.GREY);
        out(Colour.PURPLE, Colour.RED, Colour.MAGENTA);
        out(Colour.ORANGE, Colour.RED, Colour.YELLOW, Colour.BROWN);
        out(Colour.ORANGE, Colour.GREY, Colour.RED);
        out(Colour.ORANGE, Colour.LIGHT_BLUE, Colour.RED);
        out(Colour.YELLOW, Colour.GREY, Colour.RED);
        out(Colour.YELLOW, Colour.LIGHT_GREY, Colour.WHITE);
    }

    private static void out(Colour a, Colour b, Colour... results) {
        OUTCOMES.put(a + "|" + b, results);
    }

    /** BlockCoral.getNewColor: half the time this colour, then half that one, then a mix if the pair has one. */
    public static Colour newColour(Colour self, Colour other, Rng rng) {
        Colour[] values = OUTCOMES.get(self + "|" + other);
        if (values == null) values = OUTCOMES.get(other + "|" + self);
        if (rng.nextInt(2) == 0) return self;
        if (rng.nextInt(2) == 0) return other;
        if (values != null) return values[rng.nextInt(values.length)];
        return rng.nextInt(2) == 0 ? self : other;
    }

    /**
     * One random tick of a living coral (BlockCoral.updateCoral): on coral rock, with a spread roll, it looks at a
     * random nearby block; if that is living coral, a second random spot that is water with water above and coral
     * ground below gets living coral of the mixed colour. The target answers the world questions.
     */
    public interface SpreadWorld {
        boolean onCoralRock();

        /** The living coral colour at the offset, or null. */
        Colour livingAt(int dx, int dy, int dz);

        /** Water here and above, and coral ground below. */
        boolean canGrowAt(int dx, int dy, int dz);

        void grow(int dx, int dy, int dz, Colour colour);
    }

    public static void spreadTick(Settings settings, Colour self, SpreadWorld world, Rng rng) {
        if (!settings.spread() || rng.nextInt(Math.max(1, settings.spreadOneIn())) != 0 || !world.onCoralRock()) return;
        int[] a = spreadOffset(rng);
        Colour other = world.livingAt(a[0], a[1], a[2]);
        if (other == null) return;
        Colour colour = newColour(self, other, rng);
        int[] b = spreadOffset(rng);
        if (world.canGrowAt(b[0], b[1], b[2])) world.grow(b[0], b[1], b[2], colour);
    }
}
