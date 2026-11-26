package mariculture.domain.env;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Salinity;

class BiomeEnvironmentTest {
    @Test
    void legacyTable() {
        BiomeData.loadShipped();
        assertEquals(new BiomeEnvironment.Entry(Salinity.SALINE, 4), BiomeEnvironment.of("minecraft:ocean", t -> false));
        assertEquals(new BiomeEnvironment.Entry(Salinity.BRACKISH, 25), BiomeEnvironment.of("minecraft:beach", t -> false));
        assertEquals(new BiomeEnvironment.Entry(Salinity.FRESH, 80), BiomeEnvironment.of("minecraft:nether_wastes", t -> false));
    }

    @Test
    void unknownBiomesFallBackThroughTags() {
        BiomeData.loadShipped();
        assertEquals(100, BiomeEnvironment.of("modded:hell", t -> t.equals("c:is_nether")).temperature());
        assertEquals(10, BiomeEnvironment.of("modded:nowhere", t -> false).temperature());
        assertEquals(Salinity.SALINE, BiomeEnvironment.of("modded:sea", t -> t.equals("minecraft:is_ocean")).salinity(), "BUG-076");
        assertEquals(Salinity.BRACKISH, BiomeEnvironment.of("modded:shore", t -> t.equals("minecraft:is_beach")).salinity());
    }

    @Test
    void dataPacksAddBiomesAndTagsByPriority() {
        BiomeData.loadShipped();
        assertEquals(new BiomeEnvironment.Entry(Salinity.SALINE, 1), BiomeEnvironment.of("alexscaves:abyssal_chasm", t -> false), "A1");
        java.util.List<BiomeEnvironment.Layer> l = new java.util.ArrayList<>();
        l.add(BiomeEnvironment.parse("{\"priority\":-100,\"biomes\":{\"#minecraft:is_ocean\":{\"salinity\":\"saline\",\"temperature\":3}}}", "a"));
        l.add(BiomeEnvironment.parse("{\"biomes\":{\"#modded:salt_lake\":{\"salinity\":\"brackish\",\"temperature\":30},"
                + "\"minecraft:river\":{\"salinity\":\"brackish\",\"temperature\":11}}}", "b"));
        BiomeEnvironment.setData(l);
        assertEquals(Salinity.BRACKISH, BiomeEnvironment.of("modded:lake", t -> true).salinity(), "higher priority tag first");
        assertEquals(11, BiomeEnvironment.of("minecraft:river", t -> true).temperature(), "ids before tags");
        assertEquals(BiomeEnvironment.DEFAULT, BiomeEnvironment.of("modded:x", t -> false));
        l.add(0, BiomeEnvironment.parse("{\"priority\":5,\"biomes\":{\"minecraft:river\":{\"salinity\":\"saline\",\"temperature\":2}}}", "0"));
        BiomeEnvironment.setData(l);
        assertEquals(2, BiomeEnvironment.of("minecraft:river", t -> true).temperature(), "higher priority id wins over a later file");
        BiomeData.loadShipped();
    }

    @Test
    void temperatureFormula() {
        // ocean 4, noon sun 1.0, y 62, dry: 4 - 3 = 1; + 9 - 2 gives 8; above y 60: (60 - 62) * 0.11255 truncates to 0.
        assertEquals(8, BiomeEnvironment.temperature(4, 1.0f, 62, 0f));
        assertEquals(1.0f, BiomeEnvironment.sunBrightness(0f, 0f, 0f), 1e-6, "noon");
        assertEquals(0.2f, BiomeEnvironment.sunBrightness(0.5f, 0f, 0f), 1e-6, "midnight");
    }
}
