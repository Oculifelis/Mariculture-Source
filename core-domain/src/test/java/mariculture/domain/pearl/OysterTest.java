package mariculture.domain.pearl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mariculture.domain.pearl.Oyster.Below;
import mariculture.domain.pearl.Oyster.Placement;
import mariculture.domain.pearl.Oyster.Settings;
import mariculture.domain.random.ScriptedRng;
import org.junit.jupiter.api.Test;

class OysterTest {
    private static final Settings S = Settings.LEGACY;

    @Test
    void legacyTableWeights() {
        assertEquals(226, Oyster.table(S).stream().mapToInt(Oyster.Weighted::weight).sum());
        Settings noEnder = new Settings(true, 3, 12, 3, 32, false);
        assertEquals(224, Oyster.table(noEnder).stream().mapToInt(Oyster.Weighted::weight).sum());
    }

    @Test
    void weightedDrawWalksInRegistrationOrder() {
        assertEquals("mariculture:black_pearl", Oyster.randomPearl(S, new ScriptedRng().ints(0)));
        assertEquals("mariculture:black_pearl", Oyster.randomPearl(S, new ScriptedRng().ints(13)));
        assertEquals("mariculture:blue_pearl", Oyster.randomPearl(S, new ScriptedRng().ints(14)));
        assertEquals(Oyster.SAND, Oyster.randomPearl(S, new ScriptedRng().ints(179)));
        assertEquals(Oyster.RED_SAND, Oyster.randomPearl(S, new ScriptedRng().ints(209)));
        assertEquals(Oyster.ENDER_PEARL, Oyster.randomPearl(S, new ScriptedRng().ints(225)));
    }

    @Test
    void onlyOnePlainSandGoesIntoAnEmptyOyster() {
        assertTrue(Oyster.accepts(null, Oyster.SAND));
        assertFalse(Oyster.accepts(Oyster.SAND, Oyster.SAND));
        assertFalse(Oyster.accepts(null, Oyster.RED_SAND));
        assertFalse(Oyster.accepts(null, "mariculture:white_pearl"));
    }

    @Test
    void growthNeedsSandAndWaterAndTheRoll() {
        assertNull(Oyster.randomTick(S, null, true, false, Below.OTHER, null, new ScriptedRng()));
        assertNull(Oyster.randomTick(S, Oyster.SAND, false, false, Below.OTHER, null, new ScriptedRng()));
        assertNull(Oyster.randomTick(S, Oyster.RED_SAND, true, false, Below.OTHER, null, new ScriptedRng()));
        assertNull(Oyster.randomTick(S, Oyster.SAND, true, false, Below.OTHER, null, new ScriptedRng().ints(1)));
        assertEquals("mariculture:brown_pearl",
                Oyster.randomTick(S, Oyster.SAND, true, false, Below.OTHER, null, new ScriptedRng().ints(0, 32)));
    }

    @Test
    void whatIsBelowDecidesTheResult() {
        assertEquals(Oyster.ENDER_PEARL, Oyster.randomTick(S, Oyster.SAND, true, true, Below.SOUL_SAND, null, new ScriptedRng().ints(0)));
        assertEquals(Oyster.SOUL_SAND, Oyster.randomTick(S, Oyster.SAND, true, false, Below.SOUL_SAND, null, new ScriptedRng().ints(0)));
        assertEquals(Oyster.RED_SAND, Oyster.randomTick(S, Oyster.SAND, true, false, Below.RED_SAND, null, new ScriptedRng().ints(0)));
        assertEquals("mariculture:gold_pearl",
                Oyster.randomTick(S, Oyster.SAND, true, false, Below.PEARL_BLOCK, PearlColor.GOLD, new ScriptedRng().ints(0)));
    }

    @Test
    void pearlBlocksAreRecognisedByIdOnly() {
        assertEquals(PearlColor.GOLD, Oyster.pearlBlockColor("mariculture:gold_pearl_block"));
        assertNull(Oyster.pearlBlockColor("mariculture:gold_pearl_bricks"));
        assertNull(Oyster.pearlBlockColor("minecraft:sand"));
    }

    @Test
    void bug051_generationStaysInsideTheChunk() {
        ScriptedRng rng = new ScriptedRng()
                .ints(0, 15, 15, 3, 0, 14)  // placed, far corner, natural pearl roll hits, blue pearl
                .ints(5)                    // second attempt misses
                .ints(0, 0, 7, 1, 2);       // placed, no pearl
        List<Placement> placed = Oyster.generate(S, rng);
        assertEquals(List.of(new Placement(15, 15, 3, "mariculture:blue_pearl"), new Placement(0, 7, 1, null)), placed);
        assertTrue(rng.exhausted());
    }

    @Test
    void generationCanBeTurnedOffAndSettingsAreValidated() {
        assertTrue(Oyster.generate(new Settings(false, 3, 12, 3, 32, true), new ScriptedRng()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new Settings(true, 3, 0, 3, 32, true));
    }
}
