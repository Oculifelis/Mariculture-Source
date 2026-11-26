package mariculture.domain.worldgen;

import mariculture.domain.random.Rng;

/**
 * The legacy limestone sea bed (1.3.0 {@code WorldEventHandler.genBiomeTerrain}), as a column plan the loaders apply
 * after vanilla surface rules have run.
 *
 * <p>Legacy behaviour kept: under one sand block, oceans, rivers and beaches get a layer of limestone whose thickness
 * is {@code (int) (noise / depth + depth + rand * 0.25)}, only where the floor lies below {@code seaLevel - 2 -
 * thickness}; each limestone block is rutile ore with chance {@code 1 / rutileChance} (half that number in rivers).
 *
 * <p>Fixed: legacy computed a per-biome depth and then passed the ocean depth anyway (BUG-041), and drew rutile from
 * the unseeded world random so the same seed gave different ore (BUG-042). Here the depth is the biome's and every
 * draw comes from the caller's seeded {@link Rng}.
 */
public final class Seabed {
    /** Which legacy depth setting a column's biome uses. */
    public enum Kind { OCEAN, DEEP_OCEAN, RIVER, BEACH }

    /** What the loader found in one block of the column, from the ocean floor downwards. */
    public enum Cell { SURFACE, STONE, OTHER }

    /** What to place in one block of the column. */
    public enum Action { KEEP, SAND, LIMESTONE, RUTILE }

    /** Legacy config defaults from 1.3.0 {@code WorldGeneration}. */
    public record Settings(boolean enabled, boolean varyingDepth, double ocean, double deepOcean, double river,
                           double beach, boolean rutile, int rutileChance) {
        public static final Settings LEGACY = new Settings(true, true, 7.5, 3.5, 2.5, 25, true, 500);

        public Settings {
            for (double d : new double[] {ocean, deepOcean, river, beach}) {
                if (!(d > 0)) throw new IllegalArgumentException("limestone depth must be positive: " + d);
            }
            if (rutileChance < 2) throw new IllegalArgumentException("rutile chance must be at least 2: " + rutileChance);
        }

        public double depth(Kind kind) {
            if (!varyingDepth) return ocean;
            return switch (kind) {
                case OCEAN -> ocean;
                case DEEP_OCEAN -> deepOcean;
                case RIVER -> river;
                case BEACH -> beach;
            };
        }

        /** One in this many limestone blocks is rutile; legacy halved the number in rivers. */
        public int rutileOneIn(Kind kind) {
            return kind == Kind.RIVER ? rutileChance / 2 : rutileChance;
        }
    }

    /** Surface layers (sand, gravel, clay) thicker than this are left alone, as the column is not a sea bed. */
    public static final int MAX_SURFACE = 8;

    private Seabed() {}

    /** Legacy thickness formula; {@code noise} is the surface noise sample for the column. */
    public static int thickness(double noise, double depth, Rng rng) {
        return (int) (noise / depth + depth + rng.nextFloat() * 0.25);
    }

    /**
     * Plans one column. {@code cells[0]} is the topmost solid block at {@code floorY}; the result has one action per
     * cell. Rutile draws happen only for blocks that become limestone, in top-down order.
     */
    public static Action[] plan(Settings settings, Kind kind, Cell[] cells, int floorY, int seaLevel, double noise,
                                Rng rng) {
        Action[] out = new Action[cells.length];
        java.util.Arrays.fill(out, Action.KEEP);
        if (!settings.enabled() || cells.length == 0) return out;
        int thickness = thickness(noise, settings.depth(kind), rng);
        if (thickness <= 0 || floorY >= seaLevel - 2 - thickness) return out;

        int i = 0;
        while (i < cells.length && cells[i] == Cell.SURFACE) i++;
        if (i > MAX_SURFACE) return out;
        if (i == 0) {
            if (cells[0] != Cell.STONE) return out;
            out[0] = Action.SAND;
            i = 1;
        }
        int oneIn = settings.rutileOneIn(kind);
        for (int placed = 0; placed < thickness && i < cells.length && cells[i] == Cell.STONE; placed++, i++) {
            out[i] = settings.rutile() && rng.nextInt(oneIn) == 0 ? Action.RUTILE : Action.LIMESTONE;
        }
        return out;
    }
}
