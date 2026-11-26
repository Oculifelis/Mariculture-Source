package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.fish.ShippedSpecies;

/**
 * Foods (core/items/ItemFood, 1.3.0 values) and the fish recipes that make them (fishery/Fishery.java addFishRecipes
 * 400-416, fishery/Fish.addRecipe 425-440, core/helpers/RecipeHelper addFishSushi/Soup/Meal 258-277, core/Recipes.java:41).
 *
 * <p>Changes: smoked salmon is crafted from cooked salmon and charcoal, because furnaces, smokers and campfires
 * already turn raw salmon into vanilla cooked salmon (legacy replaced that smelting recipe). Kelp wraps use vanilla
 * kelp (CORE-061). Custard comes from the vat. Rice-paddy sushi (a HarvestCraft variant) is not ported.
 */
final class FoodContent {
    private FoodContent() {}

    static void register() {
        food("fish_finger", "Fish Finger", 0, 2, 0.5f, null);
        // A remainder must be registered before the food that leaves it (NeoForge resolves it eagerly).
        food("half_eaten_calamari", "Half-Eaten Calamari", 10, 3, 0.85f, "minecraft:bowl");
        food("calamari", "Calamari", 1, 4, 0.85f, "half_eaten_calamari");
        food("smoked_salmon", "Smoked Salmon", 2, 8, 1.0f, null);
        food("custard", "Custard", 3, 3, 0.3f, "minecraft:bowl");
        food("fish_and_custard", "Fish Fingers 'n Custard", 4, 5, 0.9f, "minecraft:bowl");
        food("sushi", "Sushi", 5, 6, 0.7f, null);
        food("caviar", "Caviar", 6, 3, 0.25f, null);
        food("miso_soup", "1 Portion of Miso Soup", 7, 4, 1.9f, "minecraft:bowl");
        food("miso_soup_2", "2 Portions of Miso Soup", 11, 5, 1.1f, "miso_soup");
        food("miso_soup_3", "3 Portions of Miso Soup", 12, 6, 1.2f, "miso_soup_2");
        food("kelp_wrap", "Kelp Wrap", 8, 2, 0.1f, null);
        food("oyster_pie", "Beef and Oyster Pie", 9, 20, 5.0f, null);

        Catalog.RECIPES.add(new Recipe.Shaped("fish_finger", List.of(" B ", "BFB", " B "), Map.of('B', Ingredient.item("minecraft:bread"),
                'F', Ingredient.item("minecraft:cod")), "fish_finger", 16, "Fishery.java:412"));
        Catalog.RECIPES.add(new Recipe.Shapeless("fish_and_custard", List.of(Ingredient.item("custard"), Ingredient.item("fish_finger"),
                Ingredient.item("fish_finger"), Ingredient.item("fish_finger")), "fish_and_custard", 1, "Fishery.java:415"));
        Catalog.RECIPES.add(new Recipe.Shapeless("calamari", List.of(Ingredient.item("raw_squid"), Ingredient.item("minecraft:bowl")),
                "calamari", 1, "Fishery.java:407"));
        Catalog.RECIPES.add(new Recipe.Shapeless("smoked_salmon", List.of(Ingredient.item("minecraft:cooked_salmon"), Ingredient.item("minecraft:charcoal")),
                "smoked_salmon", 1, "modern replacement for Fishery.java:408 (furnace smoking)"));
        // Kelp wraps take any kelp in mariculture:kelp; CompatTags adds other mods' kelps as optional entries (U1).
        Catalog.itemTag("mariculture:kelp", "minecraft:kelp");
        Catalog.RECIPES.add(new Recipe.Shaped("kelp_wrap", List.of("###", "###", "###"), Map.of('#', Ingredient.tag("mariculture:kelp")),
                "kelp_wrap", 1, "world/WorldPlus.java:124"));
        Catalog.RECIPES.add(new Recipe.Shaped("oyster_pie", List.of("SSS", "BOP", "WEW"), Map.of('S', Ingredient.item("salt"),
                'B', Ingredient.item("minecraft:beef"), 'O', Ingredient.item("oyster"), 'P', Ingredient.item("minecraft:porkchop"),
                'W', Ingredient.item("minecraft:wheat"), 'E', Ingredient.item("minecraft:egg")), "oyster_pie", 1, "Recipes.java:41"));
        Catalog.RECIPES.add(new Recipe.Special("caviar_from_eggs", "caviar", "Fishery.java:332 ShapelessFishRecipe"));
        Catalog.RECIPES.add(new Recipe.Special("fish_dna_reset", "fish_reset", "Fishery.java:333 ShapelessResetFishRecipe"));

        for (ShippedSpecies.Entry e : ShippedSpecies.ALL) {
            int meal = e.fishMeal();
            if (meal <= 0) continue;
            String raw = e.rawItem();
            // RecipeHelper.addFishSushi: ceil(meal / 1.5) sushi from a raw fish ringed by kelp wraps.
            Catalog.RECIPES.add(new Recipe.Shaped("sushi_from_" + e.id(), List.of(" K ", "KFK", " K "), Map.of('K', Ingredient.item("kelp_wrap"),
                    'F', Ingredient.item(raw)), "sushi", (int) Math.ceil(meal / 1.5), "RecipeHelper.java:262"));
            // addFishSoup: (int) Math.ceil(meal / 2) is integer division, so the portion tier is meal / 2.
            int number = meal / 2;
            String soup = number == 2 ? "miso_soup_2" : number >= 3 ? "miso_soup_3" : "miso_soup";
            Catalog.RECIPES.add(new Recipe.Shapeless("miso_soup_from_" + e.id(), List.of(Ingredient.item("minecraft:bowl"), Ingredient.item("kelp_wrap"),
                    Ingredient.item(raw), Ingredient.item("minecraft:brown_mushroom"), Ingredient.item("minecraft:red_mushroom")), soup, 1, "RecipeHelper.java:269"));
            Catalog.RECIPES.add(new Recipe.Shapeless("fish_meal_from_" + e.id(), List.of(Ingredient.item(raw)), "fish_meal", meal, "RecipeHelper.java:274"));
        }
    }

    private static void food(String id, String name, int meta, int hunger, float saturation, String remainder) {
        Catalog.ITEMS.add(new ItemSpec(id, name, "food:" + meta, id, 64, new ItemKind.Food(hunger, saturation, false, remainder == null ? null : Ids.full(remainder), null)));
        Catalog.itemTag("c:foods", id);
    }
}
