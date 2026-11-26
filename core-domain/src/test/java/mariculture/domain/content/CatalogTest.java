package mariculture.domain.content;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Content invariants: every recipe and tag points at something that exists. */
class CatalogTest {
    private static Set<String> known() {
        Set<String> ids = new HashSet<>() {
            @Override
            public boolean contains(Object o) {
                return super.contains(o) || (o instanceof String id && id.startsWith("minecraft:"));
            }
        };
        Catalog.blocks().forEach(b -> ids.add(Ids.full(b.id())));
        Catalog.items().forEach(i -> ids.add(Ids.full(i.id())));
        return ids;
    }

    private static void assertResolves(Ingredient ing, Set<String> ids, String where) {
        if (ing.tag()) {
            if (ing.value().startsWith("mariculture:")) assertTrue(Catalog.itemTags().containsKey(ing.value()), where + " uses missing tag " + ing.value());
        } else if (ing.value().startsWith("mariculture:")) {
            assertTrue(ids.contains(ing.value()), where + " uses unknown item " + ing.value());
        }
    }

    @Test
    void recipesReferenceRegisteredContent() {
        Set<String> ids = known();
        Set<String> recipeIds = new HashSet<>();
        for (Recipe r : Catalog.recipes()) {
            assertTrue(recipeIds.add(r.id()), "duplicate recipe id " + r.id());
            if (r instanceof Recipe.Shaped s) {
                s.key().values().forEach(i -> assertResolves(i, ids, s.id()));
                assertTrue(ids.contains(Ids.full(s.result())), s.id());
            } else if (r instanceof Recipe.Shapeless s) {
                s.ingredients().forEach(i -> assertResolves(i, ids, s.id()));
                assertTrue(ids.contains(Ids.full(s.result())), s.id());
            } else if (r instanceof Recipe.Smelting s) {
                assertResolves(s.input(), ids, s.id());
                assertTrue(ids.contains(Ids.full(s.result())), s.id());
            } else if (r instanceof Recipe.BookShapeless b) {
                assertEquals(Catalog.BOOK_ID, b.book());
            }
        }
    }

    @Test
    void tagsReferenceRegisteredContentOrTags() {
        Set<String> ids = known();
        Catalog.itemTags().forEach((tag, entries) -> entries.forEach(e -> {
            if (e.startsWith("#")) assertTrue(Catalog.itemTags().containsKey(e.substring(1)), tag + " -> " + e);
            else assertTrue(ids.contains(e), tag + " -> " + e);
        }));
        Catalog.blockTags().forEach((tag, entries) -> entries.forEach(e -> {
            if (e.startsWith("#")) assertTrue(Catalog.blockTags().containsKey(e.substring(1)), tag + " -> " + e);
            // Vanilla blocks (coral ground: sand, cobblestone) are named directly.
            else if (!e.startsWith("minecraft:")) assertTrue(Catalog.blocks().stream().anyMatch(b -> Ids.full(b.id()).equals(e)), tag + " -> " + e);
        }));
    }

    @Test
    void noVanillaDuplicates() {
        // Copper ore/block/ingot are vanilla since 1.17; Mariculture must not register its own (docs/source_delta.md).
        for (String banned : new String[] {"copper_ore", "copper_block", "copper_ingot"}) {
            assertFalse(known().contains("mariculture:" + banned), banned);
        }
    }

    @Test
    void twelvePearlColoursEachWithBlockAndBricks() {
        long pearls = Catalog.items().stream().filter(i -> i.id().endsWith("_pearl")).count();
        assertEquals(12, pearls);
        assertEquals(12, Catalog.blocks().stream().filter(b -> b.id().endsWith("_pearl_block")).count());
        assertEquals(12, Catalog.blocks().stream().filter(b -> b.id().endsWith("_pearl_bricks")).count());
    }

    @Test
    void armourItemsUseKnownMaterials() {
        for (ItemSpec i : Catalog.items()) {
            if (i.kind() instanceof ItemKind.Armor a) assertNotNull(Catalog.armorMaterial(a.material()), i.id());
        }
    }

    @Test
    void zombieLoadoutItemsExist() {
        Set<String> ids = known();
        for (mariculture.domain.fishing.ZombieFishingGear.Rod rod : mariculture.domain.fishing.ZombieFishingGear.Rod.values()) {
            assertTrue(ids.contains(Ids.full(rod.itemId())), rod.itemId());
        }
        for (mariculture.domain.pearl.PearlColor c : mariculture.domain.pearl.PearlColor.values()) {
            assertTrue(ids.contains(Ids.full(new mariculture.domain.fishing.ZombieFishingGear.Loadout(
                    mariculture.domain.fishing.ZombieFishingGear.Rod.REED, c).hatItemId())), c.id());
        }
    }

    @Test
    void foodRemaindersAreRegisteredFirst() {
        // NeoForge resolves FoodProperties.usingConvertsTo while the food item is constructed.
        Set<String> seen = new HashSet<>();
        for (ItemSpec i : Catalog.items()) {
            if (i.kind() instanceof ItemKind.Food f && f.remainder() != null && f.remainder().startsWith("mariculture:")) {
                assertTrue(seen.contains(f.remainder()), i.id() + " leaves " + f.remainder() + ", registered later");
            }
            seen.add(Ids.full(i.id()));
        }
    }
}
