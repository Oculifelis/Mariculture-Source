package mariculture.domain.diving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mariculture.domain.content.ArmorSlot;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.ItemKind;
import org.junit.jupiter.api.Test;

class DivingTest {
    @Test
    void snorkelGivesAirEveryOtherTickUnderWater() {
        assertTrue(Diving.snorkelGivesAir(10, true, false));
        assertFalse(Diving.snorkelGivesAir(11, true, false));
        assertFalse(Diving.snorkelGivesAir(10, false, false));
        assertFalse(Diving.snorkelGivesAir(10, true, true));
    }

    @Test
    void hardcoreDivingDrainsAirUnlessOffOrWearingAMaricultureHelmet() {
        assertEquals(3, Diving.hardcoreAirLoss(3, 20, 100, true, false));
        assertEquals(0, Diving.hardcoreAirLoss(0, 20, 100, true, false));
        assertEquals(0, Diving.hardcoreAirLoss(3, 21, 100, true, false));
        assertEquals(0, Diving.hardcoreAirLoss(3, 20, 100, true, true));
        assertEquals(0, Diving.hardcoreAirLoss(3, 20, 0, true, false));
    }

    @Test
    void pantsDoubleBreakingUnderWaterAndOystersSixtyFourTimes() {
        assertEquals(2f, Diving.breakSpeed(1, true, true, true, false));
        assertEquals(64f, Diving.breakSpeed(1, true, true, true, true));
        assertEquals(1f, Diving.breakSpeed(1, true, false, true, true));
        assertEquals(1f, Diving.breakSpeed(1, true, true, false, false));
    }

    @Test
    void bootsAndLifejacketMoveTheDiver() {
        Diving.Motion m = Diving.boots(new Diving.Motion(1, 1, 1), true, false, true, false);
        assertEquals(1.13, m.x(), 1e-9);
        assertEquals(0.98, m.y(), 1e-9);
        assertEquals(-0.55, Diving.boots(new Diving.Motion(0, 0, 0), true, false, false, true).y(), 1e-9);
        assertNull(Diving.boots(new Diving.Motion(0, 0, 0), false, false, true, false));
        assertEquals(0.25, Diving.lifejacket(0, true, true, false), 1e-9);
        assertEquals(0, Diving.lifejacket(0, false, true, false), 1e-9);
        assertTrue(Double.isNaN(Diving.lifejacket(-0.2, true, true, false)));
        assertTrue(Double.isNaN(Diving.lifejacket(0, false, true, true)));
    }

    @Test
    void snorkelRefusesEnchantedBooks() {
        assertFalse(Diving.anvilAllowed("mariculture:snorkel", "minecraft:enchanted_book"));
        assertFalse(Diving.anvilAllowed("minecraft:enchanted_book", "mariculture:snorkel"));
        assertTrue(Diving.anvilAllowed("mariculture:diving_helmet", "minecraft:enchanted_book"));
    }

    @Test
    void gearIsArmourWithTheLegacyMaterials() {
        var helmet = Catalog.items().stream().filter(i -> i.id().equals(Diving.HELMET)).findFirst().orElseThrow();
        assertEquals(new ItemKind.Armor("diving_helmet", ArmorSlot.HEAD), helmet.kind());
        var suit = Catalog.armorMaterials().stream().filter(m -> m.id().equals("diving_suit")).findFirst().orElseThrow();
        assertEquals(2, suit.defense(ArmorSlot.LEGS));
        assertEquals("minecraft:leather", suit.repairItem());
        assertTrue(Catalog.recipes().stream().anyMatch(r -> r.id().equals(Diving.LIFEJACKET)));
    }
}
