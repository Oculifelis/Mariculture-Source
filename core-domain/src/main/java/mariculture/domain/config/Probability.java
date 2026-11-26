package mariculture.domain.config;

/**
 * A validated chance in [0, 1]. The legacy config accepted any float and some rules
 * did arithmetic on it before comparing (see BUG-001), so every probability option
 * is parsed through here and compared with {@link #test} only.
 */
public final class Probability {
    public static final Probability NEVER = new Probability(0.0);
    public static final Probability ALWAYS = new Probability(1.0);

    private final double value;

    private Probability(double value) {
        this.value = value;
    }

    /** Rejects NaN, infinities and values outside [0, 1] with a message naming the option. */
    public static Probability of(String option, double value) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(option + " must be a number between 0 and 1, got " + value);
        }
        return value == 0.0 ? NEVER : new Probability(value);
    }

    public double value() {
        return value;
    }

    public boolean isNever() {
        return value == 0.0;
    }

    /**
     * True with exactly the configured probability for a uniform [0, 1) draw.
     * 0 never passes and 1 always passes.
     */
    public boolean test(float uniformDraw) {
        return uniformDraw < value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
