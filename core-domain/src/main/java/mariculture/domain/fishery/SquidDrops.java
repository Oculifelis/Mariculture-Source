package mariculture.domain.fishery;

import mariculture.domain.random.Rng;

/**
 * Squids drop raw squid (fishery/FisheryEventHandler.java:74-89, config "Squid Drop Calamari", default on): one, plus
 * one more per looting level at 1 in 3. Only the plain squid; glow squids (which did not exist) keep vanilla drops.
 */
public final class SquidDrops {
    public static final String ENTITY = "minecraft:squid";
    public static final String ITEM = "mariculture:raw_squid";

    private SquidDrops() {}

    public static int count(int looting, Rng rng) {
        int n = 1;
        for (int i = 0; i < looting; i++) if (rng.nextInt(3) == 0) n++;
        return n;
    }
}
