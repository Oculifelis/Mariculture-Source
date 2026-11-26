package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.diving.Diving;

/**
 * Diving gear (diving/Diving.java:36-65, 1.3.0). Legacy armour material DIVING: durability 20, armour 1/0/2/1, no
 * enchanting; SNORKEL: durability 10, no armour. Each piece repaired with its own material (ItemArmorDiving
 * .getIsRepairable), so the helmet, suit and boots are separate materials sharing one texture.
 */
final class DivingContent {
    private DivingContent() {}

    static void register() {
        Catalog.armorMaterial(new ArmorMaterialSpec("diving_helmet", 20, 1, 0, 0, 0, 0, "minecraft:copper_ingot", "diving", false));
        Catalog.armorMaterial(new ArmorMaterialSpec("diving_suit", 20, 0, 0, 2, 0, 0, "minecraft:leather", "diving", true));
        Catalog.armorMaterial(new ArmorMaterialSpec("diving_boots", 20, 0, 0, 0, 1, 0, "minecraft:iron_ingot", "diving", false));
        Catalog.armorMaterial(new ArmorMaterialSpec("snorkel", 10, 0, 0, 0, 0, 0, null, "snorkel", false));
        Catalog.armorMaterial(new ArmorMaterialSpec("lifejacket", 10, 0, 0, 0, 0, 0, null, "lifejacket", false));
        armor(Diving.HELMET, "Diving Helmet", "diving.helmet", "diving_helmet", ArmorSlot.HEAD);
        armor(Diving.TOP, "Diving Air Supply", "diving.top", "diving_suit", ArmorSlot.CHEST);
        armor(Diving.PANTS, "Diving Suit", "diving.pants", "diving_suit", ArmorSlot.LEGS);
        armor(Diving.BOOTS, "Diving Boots", "diving.boots", "diving_boots", ArmorSlot.FEET);
        armor(Diving.SNORKEL, "Snorkel", "snorkel", "snorkel", ArmorSlot.HEAD);
        armor(Diving.LIFEJACKET, "Buoyancy Aid", "lifejacket", "lifejacket", ArmorSlot.CHEST);
        Ingredient leather = Ingredient.item("minecraft:leather");
        shaped(Diving.HELMET, List.of("CCC", "CGC"), Map.of('C', Ingredient.item("minecraft:copper_ingot"), 'G', Ingredient.item("minecraft:glass")));
        shaped(Diving.TOP, List.of(" C ", "C C", " C "), Map.of('C', leather));
        shaped(Diving.PANTS, List.of("CCC", " C ", "CCC"), Map.of('C', leather));
        shaped(Diving.BOOTS, List.of("C C", "L L"), Map.of('C', leather, 'L', Ingredient.item("minecraft:iron_ingot")));
        shaped(Diving.SNORKEL, List.of("  R", "LLR"), Map.of('R', Ingredient.item("minecraft:sugar_cane"), 'L', Ingredient.item("glass_lens")));
        shaped(Diving.LIFEJACKET, List.of("WSW", "WWW", "WWW"), Map.of('S', Ingredient.item("minecraft:string"), 'W', Ingredient.tag("minecraft:logs")));
        Catalog.itemTag("mariculture:diving_gear", Diving.HELMET);
        for (String id : List.of(Diving.TOP, Diving.PANTS, Diving.BOOTS, Diving.SNORKEL, Diving.LIFEJACKET)) Catalog.itemTag("mariculture:diving_gear", id);
    }

    private static void armor(String id, String name, String legacy, String material, ArmorSlot slot) {
        Catalog.ITEMS.add(new ItemSpec(id, name, legacy, id, 1, new ItemKind.Armor(material, slot)));
    }

    private static void shaped(String id, List<String> pattern, Map<Character, Ingredient> key) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, 1, "Diving.java:58"));
    }
}
