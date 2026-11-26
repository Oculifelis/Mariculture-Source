package mariculture.domain.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SettingsTest {
    @AfterEach
    void reset() {
        Settings.reset();
    }

    @Test
    void defaultsAreTheLegacyValues() {
        assertEquals(0.0125, Settings.ZOMBIE_GEAR_CHANCE.probability().value());
        assertEquals(200, Settings.FEEDER_SPEED.get());
        assertEquals(40000, Settings.CRUCIBLE_SPEED.get());
        assertEquals(300, Settings.NET_SPEED.get());
        assertEquals(100, Settings.HATCHERY_SPEED.get());
        assertTrue(Settings.CRUCIBLE_DUSTS.get());
        assertTrue(Settings.STARGAZER_BLOCKS_SPAWNS.get(), "BUG-004: the Stargazer's intended effect is on by default");
    }

    @Test
    void keysAreUniqueWithinASection() {
        Set<String> seen = new HashSet<>();
        for (Settings.Setting s : Settings.ALL) assertTrue(seen.add(s.section() + "." + s.key()), s.key());
    }

    @Test
    void integersClampAndChancesAreValidated() {
        Settings.FEEDER_SPEED.set(0);
        assertEquals(1, Settings.FEEDER_SPEED.get());
        assertThrows(IllegalArgumentException.class, () -> Settings.ZOMBIE_HAT_DROP.set(1.5));
        Settings.STARGAZER_BLOCKS_SPAWNS.set(false);
        Settings.reset();
        assertTrue(Settings.STARGAZER_BLOCKS_SPAWNS.get());
        assertFalse(Settings.ALL.isEmpty());
    }
}
