package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnvironmentTest {
    private final FishRegistry registry = FishData.registry();

    @Test
    void freshwaterFishWithNoToleranceRejectsSaltWater() {
        // Legacy maxSalt = max(2, ...) accepted SALINE for every fish.
        Species perch = registry.species("mariculture:perch");
        assertEquals(Salinity.FRESH, perch.salinityBase());
        assertFalse(perch.ignoresSalinity());
        int t = perch.temperatureBase();
        assertTrue(Environment.matches(Salinity.FRESH, t, perch, 0, 0));
        assertFalse(Environment.matches(Salinity.SALINE, t, perch, 0, 0));
        assertTrue(Environment.matches(Salinity.BRACKISH, t, perch, 1, 0));
        assertFalse(Environment.matches(Salinity.SALINE, t, perch, 1, 0));
    }

    @Test
    void temperatureBoundsAreInclusive() {
        Species cod = registry.species("mariculture:cod");
        int base = cod.temperatureBase();
        assertTrue(Environment.matches(Salinity.SALINE, base + 5, cod, 0, 5));
        assertFalse(Environment.matches(Salinity.SALINE, base + 6, cod, 0, 5));
        assertTrue(Environment.matches(Salinity.SALINE, base - 5, cod, 0, 5));
    }

    @Test
    void salinityIgnoringSpeciesLiveAnywhere() {
        Species s = registry.allSpecies().stream().filter(Species::ignoresSalinity).findFirst().orElseThrow();
        for (Salinity sal : Salinity.values()) assertTrue(Environment.matches(sal, s.temperatureBase(), s, 0, 0));
    }
}
