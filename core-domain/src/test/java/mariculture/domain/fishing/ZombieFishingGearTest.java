package mariculture.domain.fishing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import mariculture.domain.config.Probability;
import mariculture.domain.fishing.ZombieFishingGear.Loadout;
import mariculture.domain.fishing.ZombieFishingGear.Rod;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.Rng;
import mariculture.domain.random.ScriptedRng;

/** BUG-001 regressions: the configured chance is honoured exactly. */
class ZombieFishingGearTest {
    private static final Probability DEFAULT = Probability.of("chance", ZombieFishingGear.DEFAULT_SPAWN_CHANCE);

    @Test
    void legacyOffsetNoLongerApplies() {
        // Legacy: 0.1 - 0.1 < 0.0125 passed. A draw of 0.1 is far above the configured 1.25%.
        ScriptedRng rng = new ScriptedRng().floats(0.1f);
        assertEquals(Optional.empty(), ZombieFishingGear.roll(DEFAULT, true, true, rng));
        assertTrue(rng.exhausted());
    }

    @Test
    void drawJustBelowThresholdEquips() {
        ScriptedRng rng = new ScriptedRng().floats(Math.nextDown(0.0125f)).ints(5, 5, 3);
        Optional<Loadout> gear = ZombieFishingGear.roll(DEFAULT, true, true, rng);
        assertEquals(Optional.of(new Loadout(Rod.REED, PearlColor.ORANGE)), gear);
        assertTrue(rng.exhausted());
    }

    @Test
    void drawAtThresholdDoesNotEquip() {
        ScriptedRng rng = new ScriptedRng().floats(0.0125f);
        assertTrue(ZombieFishingGear.roll(DEFAULT, true, true, rng).isEmpty());
    }

    @Test
    void zeroChanceDrawsNothing() {
        ScriptedRng rng = new ScriptedRng();
        assertTrue(ZombieFishingGear.roll(Probability.NEVER, true, true, rng).isEmpty());
        assertTrue(rng.exhausted());
    }

    @Test
    void chanceOneAlwaysEquipsEmptyZombie() {
        ScriptedRng rng = new ScriptedRng().floats(Math.nextDown(1f)).ints(0, 11);
        assertEquals(Optional.of(new Loadout(Rod.TITANIUM, PearlColor.SILVER)),
                ZombieFishingGear.roll(Probability.ALWAYS, true, true, rng));
    }

    @Test
    void rodTiersFollowLegacyOrder() {
        assertEquals(Rod.WOOD, ZombieFishingGear.roll(Probability.ALWAYS, true, true,
                new ScriptedRng().floats(0f).ints(1, 0, 0)).orElseThrow().rod());
        assertEquals(Rod.REED, ZombieFishingGear.roll(Probability.ALWAYS, true, true,
                new ScriptedRng().floats(0f).ints(1, 1, 0)).orElseThrow().rod());
    }

    @Test
    void alreadyEquippedZombieIsUntouched() {
        assertTrue(ZombieFishingGear.roll(Probability.ALWAYS, false, true, new ScriptedRng().floats(0f)).isEmpty());
        assertTrue(ZombieFishingGear.roll(Probability.ALWAYS, true, false, new ScriptedRng().floats(0f)).isEmpty());
    }

    @Test
    void observedRateMatchesConfiguredChance() {
        // 200k seeded trials; binomial sd at p=0.0125 is ~0.00025, tolerance is 5 sd.
        Rng rng = Rng.of(new Random(1234L));
        int trials = 200_000, hits = 0;
        for (int i = 0; i < trials; i++) {
            if (ZombieFishingGear.roll(DEFAULT, true, true, rng).isPresent()) hits++;
        }
        assertEquals(0.0125, hits / (double) trials, 0.00125);
    }
}
