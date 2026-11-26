package mariculture.domain.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mariculture.domain.random.ScriptedRng;
import mariculture.domain.worldgen.OceanDecor.Settings;
import org.junit.jupiter.api.Test;

class OceanDecorTest {
    private static final Settings S = Settings.LEGACY;

    @Test
    void regionShareMatchesTheLegacyStartAndEndChances() {
        // Reefs: (1/256) / (1/256 + 1/40) is about 0.135.
        assertEquals(0.135, OceanDecor.share(S.reefStart(), S.reefEnd()), 0.001);
        assertEquals(0.130, OceanDecor.share(S.forestStart(), S.forestEnd()), 0.001);
        assertTrue(OceanDecor.active(0.1, S.reefStart(), S.reefEnd()));
        assertFalse(OceanDecor.active(0.2, S.reefStart(), S.reefEnd()));
    }

    @Test
    void reefRadiusTwoToFourAndForestKelpGeometric() {
        assertEquals(2, OceanDecor.reefRadius(new ScriptedRng().ints(0)));
        assertEquals(4, OceanDecor.reefRadius(new ScriptedRng().ints(2)));
        assertEquals(1, OceanDecor.forestKelpHeight(new ScriptedRng().ints(0)));
        assertEquals(3, OceanDecor.forestKelpHeight(new ScriptedRng().ints(1, 4, 2, 0)));
    }

    @Test
    void chestsOneInSixHundredFortyWithTwoToNineItems() {
        assertTrue(OceanDecor.chest(S, new ScriptedRng().ints(0)));
        assertFalse(OceanDecor.chest(S, new ScriptedRng().ints(1)));
        assertEquals(2, OceanDecor.chestItems(S, new ScriptedRng().ints(0)));
        assertEquals(9, OceanDecor.chestItems(S, new ScriptedRng().ints(7)));
        assertEquals(16, OceanDecor.OCEAN_CHEST.size());
        assertEquals(25, OceanDecor.OCEAN_CHEST.stream().filter(l -> l.item().equals("minecraft:bone")).findFirst().orElseThrow().weight());
    }

    @Test
    void naturalGasPocketsOneInTwentySalineChunks() {
        mariculture.domain.content.OreFeature gas = mariculture.domain.content.Catalog.ores().stream()
                .filter(o -> o.id().equals("natural_gas_pocket")).findFirst().orElseThrow();
        assertEquals(48, gas.size());
        assertEquals(20, gas.oneInChunks());
        assertEquals(16, gas.minY());
        assertEquals(26, gas.maxYExclusive());
        assertEquals("mariculture:is_saline", gas.biomeTag());
    }

    @Test
    void ancientSandRadiusTwoOrThree() {
        assertEquals(2, OceanDecor.ancientSandRadius(S, new ScriptedRng().ints(0)));
        assertEquals(3, OceanDecor.ancientSandRadius(S, new ScriptedRng().ints(1)));
    }
}
