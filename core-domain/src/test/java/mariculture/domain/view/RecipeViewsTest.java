package mariculture.domain.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.machine.Stack;
import mariculture.domain.magic.Binding;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.VatRecipes;

/** Recipe viewer pages come from the machine tables and fit their category. */
class RecipeViewsTest {
    @Test
    void everyCategoryHasPagesWithUniqueIdsInsideItsBounds() {
        for (RecipeViews.Category c : RecipeViews.CATEGORIES) {
            Set<String> ids = new HashSet<>();
            var views = RecipeViews.views(c, mariculture.domain.fish.FishData.registry());
            assertFalse(views.isEmpty(), c.id());
            for (RecipeViews.View v : views) {
                assertTrue(ids.add(v.id()), v.id());
                for (RecipeViews.Slot s : v.slots()) {
                    assertTrue(s.x() >= 0 && s.x() + 16 <= c.width() && s.y() >= 0 && s.y() + 16 <= c.height(), c.id() + " " + v.id());
                }
            }
        }
    }

    @Test
    void vatAndAnvilShowEveryRecipe() {
        assertEquals(VatRecipes.all().size(), RecipeViews.views(RecipeViews.VAT, null).size());
        assertEquals(Anvil.recipes().size(), RecipeViews.views(RecipeViews.ANVIL, null).size());
    }

    @Test
    void castingShowsIronIngotFromMoltenIron() {
        assertTrue(RecipeViews.views(RecipeViews.CASTING, null).stream().anyMatch(v -> v.slots().stream()
                .anyMatch(s -> s.id().equals("minecraft:iron_ingot")) && v.slots().stream().anyMatch(s -> s.id().equals("mariculture:molten_iron"))));
    }

    @Test
    void jewelryPagesShowWhatTheGridCrafts() {
        var views = RecipeViews.views(RecipeViews.JEWELRY, null);
        assertEquals(JewelryType.values().length * Binding.values().length * JewelryMaterial.values().length, views.size());
        for (RecipeViews.View v : views) {
            String[] grid = new String[9];
            java.util.Arrays.fill(grid, "");
            RecipeViews.Slot out = null;
            for (RecipeViews.Slot s : v.slots()) {
                if (s.role() == RecipeViews.Role.INPUT) grid[s.y() / 18 * 3 + s.x() / 18] = s.id();
                if (s.role() == RecipeViews.Role.OUTPUT) out = s;
            }
            Jewelry.Match m = Jewelry.match(java.util.List.of(grid), i -> null);
            assertEquals(Jewelry.create(m.type(), m.binding(), m.material()), Stack.of(out.id(), 1, out.data()), v.id());
        }
    }

    @Test
    void fishingPagesCoverLootBaitAndEverySpecies() {
        var fish = mariculture.domain.fish.FishData.registry();
        var loot = RecipeViews.views(RecipeViews.FISHING_LOOT, null);
        // The nether star is rare loot from the titanium rod up, in the Nether only.
        RecipeViews.View star = loot.stream().filter(v -> v.slots().get(1).id().equals("minecraft:nether_star")).findFirst().orElseThrow();
        assertEquals("mariculture:titanium_fishing_rod", star.slots().get(0).id());
        assertEquals("jei.mariculture.fishing.dimension.nether", star.lines().get(2).key());
        assertTrue(RecipeViews.views(RecipeViews.FISHING_BAIT, null).stream()
                .anyMatch(v -> v.slots().get(0).id().equals("mariculture:raw_minnow") && v.slots().get(1).id().equals("mariculture:titanium_fishing_rod")));
        assertEquals(fish.allSpecies().size(), RecipeViews.views(RecipeViews.FISH_CATCH, fish).size());
        assertEquals(fish.mutations().size(), RecipeViews.views(RecipeViews.FISH_BREEDING, fish).size());
        int products = fish.allSpecies().stream().mapToInt(sp -> sp.products().size()).sum();
        assertEquals(products, RecipeViews.views(RecipeViews.FISH_PRODUCTS, fish).size());
    }
}
