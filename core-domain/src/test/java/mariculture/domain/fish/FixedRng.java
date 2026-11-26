package mariculture.domain.fish;

import mariculture.domain.random.Rng;

/** Always returns the same draw (clamped into the bound). */
record FixedRng(int value) implements Rng {
    @Override public float nextFloat() { return 0f; }
    @Override public int nextInt(int bound) { return Math.min(value, bound - 1); }
}
