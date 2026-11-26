package mariculture.domain.fishery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Species;
import mariculture.domain.random.ScriptedRng;

class BaitAndHatcheryTest {
    @Test
    void baitHunger() {
        assertEquals(1, Bait.hunger(10));
        assertEquals(2, Bait.hunger(55));
        assertEquals(3, Bait.hunger(75));
        assertEquals(4, Bait.hunger(100));
    }

    @Test
    void baitComesFromTheLeftSlotFirst() {
        String[] hotbar = {"mariculture:bait_worm", "minecraft:fishing_rod", "mariculture:bait_ant", "", "", "", "", "", ""};
        assertEquals(0, Bait.findSlot(1, i -> hotbar[i], Species.Rod.GOOD));
        assertEquals(2, Bait.findSlot(1, i -> hotbar[i], Species.Rod.OLD), "worms are not for old rods");
        assertEquals(-1, Bait.findSlot(0, i -> hotbar[i], Species.Rod.SUPER));
    }

    @Test
    void siftingDirtRollsAntsAndWorms() {
        var recipes = Bait.sifterRecipes("minecraft:dirt", t -> false);
        assertEquals(2, recipes.size());
        // ant: roll 39 < 40 passes, count 2 + 1; worm: roll 9 < 10 passes, count 2 + 0.
        Map<String, Integer> out = Bait.sift(recipes, new ScriptedRng().ints(39, 1, 9, 0));
        assertEquals(3, out.get("mariculture:bait_ant"));
        assertEquals(2, out.get("mariculture:bait_worm"));
    }

    @Test
    void tagRecipesNeedTheTag() {
        assertTrue(Bait.sifterRecipes("minecraft:oak_sapling", t -> false).isEmpty());
        assertEquals(1, Bait.sifterRecipes("minecraft:oak_sapling", t -> t.equals("minecraft:saplings")).size());
    }

    @Test
    void hatcherySpeedCountsNeighbouringHatcheries() {
        assertEquals(26, Hatchery.speed(1, 0), "alone in air");
        assertEquals(2, Hatchery.speed(1, 26), "alone in water, the only case legacy could reach");
        assertEquals(18 * 9, Hatchery.speed(9, 18), "nine hatcheries in water, BUG-043 fixed");
    }

    @Test
    void squidDropsOnePlusOneInThreePerLootingLevel() {
        assertEquals(1, SquidDrops.count(0, new ScriptedRng()));
        assertEquals(3, SquidDrops.count(3, new ScriptedRng().ints(0, 2, 0)));
    }
}
