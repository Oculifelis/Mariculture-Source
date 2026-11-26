package mariculture.domain.content;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OreFeatureTest {
    @Test
    void invertedHeightRangeIsRejectedInsteadOfCrashingWorldgen() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new OreFeature("ore_x", "x", 8, 1, 64, 64, "minecraft:stone_ore_replaceables", "minecraft:is_overworld", "test"));
        assertTrue(e.getMessage().contains("maximum Y"));
    }

    @Test
    void bauxiteKeepsLegacyDefaults() {
        OreFeature bauxite = Catalog.ores().stream().filter(o -> o.id().equals("ore_bauxite")).findFirst().orElseThrow();
        assertEquals(8, bauxite.size());
        assertEquals(16, bauxite.veinsPerChunk());
        assertEquals(64, bauxite.minY());
        assertEquals(256, bauxite.maxYExclusive());
        assertTrue(Catalog.blocks().stream().anyMatch(b -> b.id().equals(bauxite.block())));
    }
}
