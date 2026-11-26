package mariculture.domain.content;

import java.util.List;
import java.util.Map;

/**
 * Magic items (magic/Magic.java:73-85, 216-222, 1.3.0): ring, bracelet and necklace (made through
 * {@link mariculture.domain.magic.Jewelry}, not catalog recipes), and the three mirrors. Mirror durability from
 * ItemMirror (100) and ItemMagicMirror (1000, 10000).
 *
 * <p>Not ported: the alternative celestial mirror recipe from jewelry pieces (Magic.java:226-233); recipes in the
 * catalog cannot ask for item data, and the fish-free recipe is the default one anyway.
 *
 * <p>Mob magnet, hydrophobic initiator and infiwater (Magic.java:135-144, 226-231). Infiwater's recipe asked for water
 * bottles, which are potions with data; water buckets stand in (their empty buckets are returned).
 */
final class MagicContent {
    private MagicContent() {}

    static void register() {
        for (String[] j : new String[][] {{"ring", "Ring"}, {"bracelet", "Bracelet"}, {"necklace", "Necklace"}}) {
            Catalog.ITEMS.add(new ItemSpec(j[0], j[1], j[0], "jewelry/" + j[0] + "/white", 1, new ItemKind.Special("jewelry", 0)));
            Catalog.itemTag("mariculture:jewelry", j[0]);
        }
        Catalog.RECIPES.add(new Recipe.Special("jewelry", "jewelry", "Magic.java:236 addJewelry, ShapedJewelryRecipe, ShapelessJewelryRecipe"));
        Catalog.ITEMS.add(new ItemSpec("mirror", "Mirror", "mirror", "mirror", 1, new ItemKind.Special("mirror", 100)));
        Catalog.ITEMS.add(new ItemSpec("magic_mirror", "Magic Mirror", "magicMirror", "magic_mirror", 1, new ItemKind.Special("magic_mirror", 1000)));
        Catalog.ITEMS.add(new ItemSpec("celestial_mirror", "Celestial Mirror", "celestialMirror", "celestial_mirror", 1,
                new ItemKind.Special("celestial_mirror", 10000)));
        Catalog.RECIPES.add(new Recipe.Shaped("mirror", List.of(" AA", "APA", "SA "), Map.of('A', Ingredient.item("aluminum_ingot"),
                'P', Ingredient.item("minecraft:glass_pane"), 'S', Ingredient.item("minecraft:iron_ingot")), "mirror", 1, "Magic.java:218"));
        Catalog.RECIPES.add(new Recipe.Shaped("magic_mirror", List.of("PMP", "BEB", "PBP"), Map.of('B', Ingredient.item("storage_bookshelf"),
                'M', Ingredient.item("mirror"), 'E', Ingredient.item("minecraft:enchanting_table"), 'P', Ingredient.tag("mariculture:pearls")),
                "magic_mirror", 1, "Magic.java:219"));
        Catalog.RECIPES.add(new Recipe.Shaped("celestial_mirror", List.of("TST", "BMB", "GBG"), Map.of('B', Ingredient.item("storage_bookshelf"),
                'M', Ingredient.item("magic_mirror"), 'S', Ingredient.item("minecraft:nether_star"), 'T', Ingredient.item("droplet_magic"),
                'G', Ingredient.item("golden_thread")), "celestial_mirror", 1, "Magic.java:220"));
        Catalog.ITEMS.add(new ItemSpec("mob_magnet", "Mob Magnet", "mobMagnet", "mob_magnet", 1, new ItemKind.Special("mob_magnet", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped("mob_magnet", List.of("III", "I I", "M M"), Map.of('I', Ingredient.item("minecraft:iron_ingot"),
                'M', Ingredient.item("minecraft:ender_pearl")), "mob_magnet", 1, "Magic.java:227"));
        Catalog.ITEMS.add(new ItemSpec("hydrophobic_initiator", "Hydrophobic Initiator", "placer", "hydrophobic_initiator", 1,
                new ItemKind.Special("hydrophobic_initiator", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped("hydrophobic_initiator", List.of(" L ", "LPL", " L "), Map.of('L', Ingredient.item("minecraft:lily_pad"),
                'P', Ingredient.tag("mariculture:pearls")), "hydrophobic_initiator", 1, "Magic.java:231"));
        Catalog.ITEMS.add(new ItemSpec("infiwater", "Infinity Water", "infiwater", "infiwater", 1, new ItemKind.Special("infiwater", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped("infiwater", List.of("PWP", "WBW", "PWP"), Map.of('P', Ingredient.tag("mariculture:pearls"),
                'W', Ingredient.item("minecraft:water_bucket"), 'B', Ingredient.item("minecraft:bucket")), "infiwater", 1, "Magic.java:226"));
    }
}
