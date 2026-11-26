package mariculture.domain.fishing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.FishData;
import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.Species;

/** A5: catch.biomes and catch.fluid limit where a species bites (the Alex's Caves fish). */
class CavePlaceTest {
    private static FishingRules.Where at(String biome, String fluidTag, boolean otherFluid) {
        return new FishingRules.Where() {
            public boolean inBiome(String idOrTag) { return idOrTag.equals(biome); }
            public boolean inFluid(String idOrTag) { return idOrTag.equals(fluidTag); }
            public boolean otherFluid() { return otherFluid; }
        };
    }

    private static final FishRegistry REG = FishData.registry(Set.of("mariculture:present/alexscaves"));

    @Test
    void acidFishBiteOnlyInAcidInToxicCaves() {
        Species lamprey = REG.species("mariculture:acid_lamprey");
        assertTrue(FishingRules.placeMatches(lamprey, at("alexscaves:toxic_caves", "#mariculture:fishing/acid", true)));
        assertFalse(FishingRules.placeMatches(lamprey, at("alexscaves:toxic_caves", null, false)));
        assertFalse(FishingRules.placeMatches(lamprey, at("minecraft:plains", "#mariculture:fishing/acid", true)));
    }

    @Test
    void waterCaveFishNeedTheirBiomeAndWater() {
        Species ray = REG.species("mariculture:lodestone_ray");
        assertTrue(FishingRules.placeMatches(ray, at("alexscaves:magnetic_caves", null, false)));
        assertFalse(FishingRules.placeMatches(ray, at("minecraft:river", null, false)));
        assertFalse(FishingRules.placeMatches(ray, at("alexscaves:magnetic_caves", "#mariculture:fishing/acid", true)));
    }

    @Test
    void ordinaryFishStayOutOfOtherFluids() {
        Species cod = REG.species("mariculture:cod");
        assertTrue(FishingRules.placeMatches(cod, FishingRules.Where.WATER));
        assertTrue(FishingRules.placeMatches(cod, at("alexscaves:abyssal_chasm", null, false)));
        assertFalse(FishingRules.placeMatches(cod, at("alexscaves:candy_cavity", "#mariculture:fishing/soda", true)));
    }
}
