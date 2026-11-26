package mariculture.domain.fishing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Fish;
import mariculture.domain.fish.Salinity;
import mariculture.domain.pearl.PearlColor;

/** Legacy ItemArmorFishingHat colour effects. */
class FishingHatTest {
    @Test
    void plainHatHasTheBiggerAliveBonusAndNothingElse() {
        FishingHat.Effects e = FishingHat.effects(null);
        assertEquals(15, e.aliveModifier());
        assertNull(e.gender());
        assertNull(e.salinity());
        assertFalse(e.forceLoot() || e.forceFish() || e.nether() || e.alwaysDead() || e.enchantLoot());
        assertEquals("bucket_hat", FishingHat.itemId(null));
    }

    @Test
    void colourEffectsMatchLegacy() {
        assertEquals(Fish.MALE, FishingHat.effects(PearlColor.BLUE).gender());
        assertEquals(Fish.FEMALE, FishingHat.effects(PearlColor.PINK).gender());
        assertEquals(Salinity.SALINE, FishingHat.effects(PearlColor.SILVER).salinity());
        assertEquals(Salinity.BRACKISH, FishingHat.effects(PearlColor.YELLOW).salinity());
        assertEquals(Salinity.FRESH, FishingHat.effects(PearlColor.WHITE).salinity());
        assertEquals(24, FishingHat.effects(PearlColor.GREEN).temperature());
        assertEquals(90, FishingHat.effects(PearlColor.RED).temperature());
        assertTrue(FishingHat.effects(PearlColor.RED).nether());
        assertTrue(FishingHat.effects(PearlColor.GOLD).forceLoot());
        assertTrue(FishingHat.effects(PearlColor.ORANGE).forceFish());
        assertEquals(10, FishingHat.effects(PearlColor.BROWN).yHeight());
        assertTrue(FishingHat.effects(PearlColor.BLACK).alwaysDead());
        assertTrue(FishingHat.effects(PearlColor.PURPLE).enchantLoot());
        assertEquals(5, FishingHat.effects(PearlColor.PURPLE).aliveModifier());
        assertEquals("purple_bucket_hat", FishingHat.itemId(PearlColor.PURPLE));
    }
}
