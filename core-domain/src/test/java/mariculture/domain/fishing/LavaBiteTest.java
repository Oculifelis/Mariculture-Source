package mariculture.domain.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mariculture.domain.random.Rng;

class LavaBiteTest {
    /** Always the low end: waits 100 - lure, swims 20, bites 20. */
    private static final Rng LOW = new Rng() {
        @Override
        public float nextFloat() {
            return 0.9f;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };

    @Test
    void luresThenSwimsThenBitesThenEscapes() {
        LavaBite bite = new LavaBite(40);
        int ticks = 0;
        LavaBite.Event e;
        do {
            e = bite.tick(LOW, false);
            ticks++;
        } while (e != LavaBite.Event.HOOKED);
        // 1 tick to set the wait, 60 to lure, 20 to swim in.
        assertEquals(1 + 60 + 20, ticks);
        assertTrue(bite.biting());
        for (int i = 0; i < 19; i++) assertEquals(LavaBite.Event.BITE, bite.tick(LOW, false));
        assertEquals(LavaBite.Event.ESCAPED, bite.tick(LOW, false));
        assertFalse(bite.biting());
    }

    @Test
    void lureNeverMakesTheWaitZeroOrNegative() {
        LavaBite bite = new LavaBite(10_000);
        assertEquals(LavaBite.Event.LURING, bite.tick(LOW, false));
        assertEquals(LavaBite.Event.LURING, bite.tick(LOW, false));
        assertEquals(LavaBite.Event.SWIMMING, bite.tick(LOW, false));
    }

    @Test
    void underARoofHalfTheTicksDoNotCount() {
        Rng roof = new Rng() {
            @Override
            public float nextFloat() {
                return 0.1f;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        LavaBite bite = new LavaBite(0);
        bite.tick(roof, true);
        for (int i = 0; i < 500; i++) assertEquals(LavaBite.Event.LURING, bite.tick(roof, true));
    }
}
