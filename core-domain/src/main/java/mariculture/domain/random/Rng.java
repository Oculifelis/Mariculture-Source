package mariculture.domain.random;

/**
 * The randomness a rule needs. Target adapters wrap the level's random source;
 * tests inject a scripted sequence so boundaries can be asserted exactly.
 */
public interface Rng {
    /** Uniform in [0, 1). */
    float nextFloat();

    /** Uniform in [0, bound); bound must be positive. */
    int nextInt(int bound);

    static Rng of(java.util.Random random) {
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
