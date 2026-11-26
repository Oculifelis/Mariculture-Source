package mariculture.domain.magic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Catalog;
import mariculture.domain.machine.Stack;

class MagicToolsTest {
    @Test
    void mobMagnetBindsOnceOnAPlayerKill() {
        Stack magnet = Stack.of("mariculture:mob_magnet", 1);
        assertNull(MobMagnet.bound(magnet));
        assertNull(MobMagnet.bind(magnet, "minecraft:zombie", false, false), "an arrow kill does not bind");
        assertNull(MobMagnet.bind(magnet, "minecraft:player", true, true), "players are never bound");
        Stack bound = MobMagnet.bind(magnet, "minecraft:zombie", false, true);
        assertEquals("minecraft:zombie", MobMagnet.bound(bound));
        assertNull(MobMagnet.bind(bound, "minecraft:cow", false, true), "a bound magnet keeps its mob");
        assertArrayEquals(new int[] {-3, 65, 10}, MobMagnet.target(-2.5, 64.0, 10.9));
        assertEquals(32, MobMagnet.RANGE);
    }

    @Test
    void infiwaterGivesWaterOnlyAndTheInitiatorNeedsABlock() {
        assertEquals(1000, WaterItems.infiwaterGives("minecraft:water", 5000));
        assertEquals(250, WaterItems.infiwaterGives("minecraft:water", 250));
        assertEquals(0, WaterItems.infiwaterGives("minecraft:lava", 1000));
        assertTrue(WaterItems.placesOnWater(true, true));
        assertFalse(WaterItems.placesOnWater(false, true));
        assertFalse(WaterItems.placesOnWater(true, false));
    }

    @Test
    void catalogHasTheThreeToolsAndTheirRecipes() {
        for (String id : new String[] {"mob_magnet", "hydrophobic_initiator", "infiwater"}) {
            assertTrue(Catalog.items().stream().anyMatch(i -> i.id().equals(id)), id);
            assertTrue(Catalog.recipes().stream().anyMatch(r -> r.id().equals(id)), id + " recipe");
        }
    }
}
