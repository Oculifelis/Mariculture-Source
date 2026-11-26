package mariculture.domain.fishery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** BUG-004: one box per feeder, removed with the feeder, overlapping feeders independent. */
class SpawnGuardTest {
    private static final String OW = "minecraft:overworld";

    @AfterEach
    void clear() {
        SpawnGuard.clear();
    }

    @Test
    void boxIsTheTankWidenedBy16SidewaysAnd8Vertically() {
        assertArrayEquals(new int[] {-17, -9, -16, 18, 8, 16}, SpawnGuard.around(List.of(new int[] {-1, -1, 0}, new int[] {2, 0, 0})));
        assertNull(SpawnGuard.around(List.of()));
    }

    @Test
    void indexSizeIsBoundedPerFeederAndBreakingRemovesIt() {
        int[] big = {0, 0, 0, 1000, 100, 1000};
        for (int i = 0; i < 50; i++) SpawnGuard.set(OW, 7L, big);
        assertEquals(1, SpawnGuard.size());
        assertTrue(SpawnGuard.blocks(OW, 500, 50, 500));
        assertFalse(SpawnGuard.blocks("minecraft:the_nether", 500, 50, 500));
        SpawnGuard.set(OW, 7L, null);
        assertEquals(0, SpawnGuard.size());
        assertFalse(SpawnGuard.blocks(OW, 500, 50, 500));
    }

    @Test
    void overlappingFeedersDoNotLiftEachOther() {
        SpawnGuard.set(OW, 1L, new int[] {0, 0, 0, 10, 10, 10});
        SpawnGuard.set(OW, 2L, new int[] {5, 0, 5, 15, 10, 15});
        SpawnGuard.set(OW, 1L, null);
        assertTrue(SpawnGuard.blocks(OW, 7, 5, 7));
        assertFalse(SpawnGuard.blocks(OW, 2, 5, 2));
    }
}
