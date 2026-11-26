package mariculture.platform;

import net.minecraft.util.RandomSource;

import mariculture.domain.random.Rng;

/** Wraps a Minecraft random source for domain rules. */
public final class Rngs {
    private Rngs() {}

    public static Rng of(RandomSource random) {
        return new Rng() {
            @Override
            public float nextFloat() {
                return random.nextFloat();
            }

            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }
        };
    }
}
