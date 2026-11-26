package mariculture.domain.fishery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Face;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;

class FishSorterTest {
    private static Stack fish(String upper, String lower) {
        return Stack.of("mariculture:" + upper, 1, Map.of("species", "mariculture:" + upper, "lower_species", "mariculture:" + lower));
    }

    private static Stack egg(String species) {
        return Stack.of("mariculture:fish_eggs", 1, Map.of("species_0", "mariculture:" + species));
    }

    @Test
    void filtersMatchSpeciesPairsEggsAndItems() {
        assertTrue(FishSorter.matches(fish("cod", "bass"), fish("bass", "cod")), "either order");
        assertFalse(FishSorter.matches(fish("cod", "bass"), fish("cod", "cod")));
        assertTrue(FishSorter.matches(egg("cod"), egg("koi")), "any eggs");
        assertTrue(FishSorter.matches(Stack.of("minecraft:dirt"), Stack.of("minecraft:dirt", 5)));
        assertFalse(FishSorter.matches(Stack.of("minecraft:dirt"), fish("cod", "cod")));
    }

    @Test
    void matchingItemsLeaveThroughTheirFilterFaceOthersThroughTheDefault() {
        FishSorter sorter = new FishSorter();
        ListInventory inv = new ListInventory(sorter);
        inv.set(3, fish("koi", "koi"));
        for (int i = 0; i < 2; i++) sorter.button(FishSorter.FILTER_BUTTON + 3); // filter 3 -> north
        sorter.button(FishSorter.BUTTON_DEFAULT_SIDE); // default -> up
        FakeWorld world = new FakeWorld(1);
        inv.set(FishSorter.INPUT, fish("koi", "koi").withCount(5));
        world.tick(sorter);
        inv.set(FishSorter.INPUT, Stack.of("minecraft:dirt", 2));
        world.tick(sorter);
        assertEquals(5, world.ejected.get(Face.NORTH).get(0).count());
        assertEquals("minecraft:dirt", world.ejected.get(Face.UP).get(0).item());
        assertTrue(inv.get(FishSorter.INPUT).isEmpty());
        assertEquals(1, sorter.slotLimit(0));
        assertTrue(sorter.isGhost(20) && !sorter.isGhost(FishSorter.INPUT));
    }

    @Test
    void sidesSurviveSaveAndLoad() {
        FishSorter a = new FishSorter();
        new ListInventory(a);
        a.button(FishSorter.FILTER_BUTTON + 20);
        a.button(FishSorter.BUTTON_EJECT);
        Map<String, Object> saved = new java.util.HashMap<>();
        a.save(saved);
        FishSorter b = new FishSorter();
        new ListInventory(b);
        b.load(saved);
        assertArrayEquals(a.gauges(), b.gauges());
    }
}
