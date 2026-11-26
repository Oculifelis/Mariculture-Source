package mariculture.domain.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProbabilityTest {
    @Test
    void rejectsInvalidValues() {
        for (double bad : new double[] {Double.NaN, -0.0001, 1.0001, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Probability.of("opt", bad));
            assertTrue(e.getMessage().startsWith("opt must be"), e.getMessage());
        }
    }

    @Test
    void zeroNeverPassesAndOneAlwaysPasses() {
        Probability zero = Probability.of("opt", 0);
        Probability one = Probability.of("opt", 1);
        assertTrue(zero.isNever());
        assertFalse(zero.test(0f));
        assertTrue(one.test(0f));
        assertTrue(one.test(Math.nextDown(1f)));
    }

    @Test
    void thresholdIsExclusive() {
        Probability p = Probability.of("opt", 0.25);
        assertTrue(p.test(Math.nextDown(0.25f)));
        assertFalse(p.test(0.25f));
    }
}
