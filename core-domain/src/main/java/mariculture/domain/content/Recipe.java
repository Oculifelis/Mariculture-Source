package mariculture.domain.content;

import java.util.List;
import java.util.Map;

/** Canonical recipe definitions; each target exporter writes them in its own JSON schema. */
public sealed interface Recipe permits Recipe.Shaped, Recipe.Shapeless, Recipe.Smelting, Recipe.BookShapeless, Recipe.Special {
    String id();

    /** Mariculture-only recipe metadata kept for docs: where the legacy recipe lives. */
    String legacy();

    record Shaped(String id, List<String> pattern, Map<Character, Ingredient> key, String result, int count, String legacy) implements Recipe {
        public Shaped {
            Ids.requireValid(id);
            pattern = List.copyOf(pattern);
            key = Map.copyOf(key);
            if (pattern.isEmpty() || pattern.size() > 3) throw new IllegalArgumentException(id + ": bad pattern");
            int width = pattern.get(0).length();
            for (String row : pattern) {
                if (row.length() != width || width > 3) throw new IllegalArgumentException(id + ": ragged pattern");
                for (char c : row.toCharArray()) {
                    if (c != ' ' && !key.containsKey(c)) throw new IllegalArgumentException(id + ": unmapped key " + c);
                }
            }
            if (count < 1) throw new IllegalArgumentException(id + ": bad count");
        }
    }

    /** {@code resultData} is item data put on the result (a charged battery's "Energy"); usually empty. */
    record Shapeless(String id, List<Ingredient> ingredients, String result, int count, String legacy, Map<String, Object> resultData) implements Recipe {
        public Shapeless {
            Ids.requireValid(id);
            ingredients = List.copyOf(ingredients);
            resultData = Map.copyOf(resultData);
            if (ingredients.isEmpty() || ingredients.size() > 9 || count < 1) throw new IllegalArgumentException(id + ": bad shapeless recipe");
        }

        public Shapeless(String id, List<Ingredient> ingredients, String result, int count, String legacy) {
            this(id, ingredients, result, count, legacy, Map.of());
        }
    }

    /** Vanilla furnace recipe. Legacy FurnaceRecipes used 200 ticks. */
    record Smelting(String id, Ingredient input, String result, float experience, int cookingTime, String legacy) implements Recipe {
        public Smelting {
            Ids.requireValid(id);
            if (cookingTime < 1) throw new IllegalArgumentException(id + ": bad cooking time");
        }
    }

    /** A crafting recipe with code behind it ("mariculture:<type>"), registered by each target (caviar, fish DNA reset). */
    record Special(String id, String type, String legacy) implements Recipe {
        public Special {
            Ids.requireValid(id);
            Ids.requireValid(type);
        }
    }

    /** A shapeless Patchouli guide book recipe (Patchouli's serializer on 1.20.1, a book data component later). */
    record BookShapeless(String id, List<Ingredient> ingredients, String book, String legacy) implements Recipe {
        public BookShapeless {
            Ids.requireValid(id);
            ingredients = List.copyOf(ingredients);
        }
    }
}
