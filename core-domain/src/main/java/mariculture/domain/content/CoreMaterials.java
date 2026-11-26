package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.machine.Upgrades;

/**
 * Core materials and crafting parts (core/items/ItemMaterial, ItemCrafting, ItemUpgrade; recipes core/Recipes.java).
 * Copper ingot and iron nugget are vanilla. Recipes made in machines (vat, anvil, crucible) arrive with those machines.
 *
 * <p>Not ported: the empty metas 15-26 (CORE-047), legacy dyes (vanilla has every colour, CORE-046), magnesite
 * dust (disabled in 1.3.0), kelp seeds (vanilla kelp, CORE-061), debug upgrades (CORE-078).
 */
final class CoreMaterials {
    private CoreMaterials() {}

    static void register() {
        metals();
        dusts();
        parts();
        upgrades();
        casings();
    }

    // Machine casings: BlockRock BASE_BRICK (hardness 10, explosion resistance 30, iron tool), BlockMetal BASE_IRON
    // (hardness 3, resistance 10, stone tool), BlockWood BASE_WOOD (Core.java:245 setHardness 2, axe). Recipes.java:44-46.
    private static void casings() {
        Catalog.add(new BlockSpec("base_brick", "Base Brick", "rocks:4", Shape.CUBE, Sound.STONE, 10f, 30f, true, HarvestLevel.IRON,
                Map.of("all", "base_brick"), true));
        Catalog.add(new BlockSpec("base_iron", "Base Iron", "metals:5", Shape.CUBE, Sound.METAL, 3f, 10f, true, HarvestLevel.STONE,
                Map.of("all", "base_iron"), true));
        Catalog.add(new BlockSpec("base_wood", "Base Wood", "woods:0", Shape.CUBE, Sound.WOOD, 2f, 2f, false, HarvestLevel.ANY,
                Map.of("all", "base_wood"), true));
        Catalog.blockTag("minecraft:mineable/axe", "base_wood");
        List<String> ring = List.of("IGI", "G G", "IGI");
        shaped("base_brick", 1, ring, Map.of('I', Ingredient.item("burnt_brick"), 'G', Ingredient.item("minecraft:iron_bars")), "Recipes.java:44");
        shaped("base_iron", 1, ring, Map.of('I', Ingredient.item("minecraft:iron_ingot"), 'G', Ingredient.item("minecraft:glass_pane")), "Recipes.java:45");
        shaped("base_wood", 1, ring, Map.of('I', Ingredient.tag("minecraft:logs"), 'G', Ingredient.tag("minecraft:wooden_fences")), "Recipes.java:46");
    }

    private static void item(String id, String name, String legacy) {
        Catalog.ITEMS.add(new ItemSpec(id, name, legacy, id, 64));
    }

    private static void metals() {
        String[][] ingots = {{"aluminum", "Aluminium", "0"}, {"magnesium", "Magnesium", "1"}, {"titanium", "Titanium", "2"}, {"rutile", "Impure Titanium", "3"}};
        String[][] nuggets = {{"aluminum", "Aluminium", "34"}, {"magnesium", "Magnesium", "35"}, {"titanium", "Titanium", "36"}, {"rutile", "Impure Titanium", "37"},
                {"copper", "Copper", "38"}};
        for (String[] i : ingots) {
            item(i[0] + "_ingot", i[1] + " Ingot", "materials:" + i[2]);
            Catalog.itemTag("c:ingots/" + i[0], i[0] + "_ingot");
            Catalog.itemTag("c:ingots", "#c:ingots/" + i[0]);
            // core/Recipes.java:166-177 (block <-> 9 ingots)
            nine(i[0] + "_block", i[0] + "_ingot", "Recipes.java:166");
            Catalog.RECIPES.add(new Recipe.Shapeless(i[0] + "_ingot_from_block", List.of(Ingredient.item(i[0] + "_block")), i[0] + "_ingot", 9, "Recipes.java:167"));
        }
        for (String[] n : nuggets) {
            item(n[0] + "_nugget", n[1] + " Nugget", "materials:" + n[2]);
            Catalog.itemTag("c:nuggets/" + n[0], n[0] + "_nugget");
            Catalog.itemTag("c:nuggets", "#c:nuggets/" + n[0]);
            String ingot = n[0].equals("copper") ? "minecraft:copper_ingot" : n[0] + "_ingot";
            // core/Recipes.java:153-164 (ingot <-> 9 nuggets)
            nine(ingot, n[0] + "_nugget", "Recipes.java:153");
            Catalog.RECIPES.add(new Recipe.Shapeless(n[0] + "_nugget_from_ingot", List.of(Ingredient.item(ingot)), n[0] + "_nugget", 9, "Recipes.java:154"));
        }
    }

    private static void nine(String result, String input, String legacy) {
        String id = (result.contains(":") ? result.substring(result.indexOf(':') + 1) : result) + "_from_" + input.replace(':', '_');
        Catalog.RECIPES.add(new Recipe.Shaped(id, List.of("###", "###", "###"), Map.of('#', Ingredient.item(input)), result, 1, legacy));
    }

    private static void dusts() {
        String[][] dusts = {{"aluminum_dust", "Unknown Aluminium Dust", "5"}, {"silvery_dust", "Unknown Silvery Dust", "6"},
                {"leader_dust", "Unknown Leader Dust", "7"}, {"tinnic_dust", "Unknown Tinnic Dust", "8"}, {"copperous_dust", "Unknown Copperous Dust", "9"},
                {"golden_dust", "Unknown Golden Dust", "10"}, {"ironic_dust", "Unknown Ironic Dust", "11"}};
        for (String[] d : dusts) {
            item(d[0], d[1], "materials:" + d[2]);
            Catalog.itemTag("mariculture:ore_dusts", d[0]);
        }
        item("salt", "Salt", "materials:12");
        Catalog.itemTag("c:dusts/salt", "salt");
        Catalog.itemTag("c:foods/salt", "salt");
    }

    private static void parts() {
        Object[][] parts = {
                {"golden_silk", "Golden Silk", 0}, {"golden_thread", "Golden Thread", 1}, {"polished_titanium", "Polished Titanium Rod", 2},
                {"polished_stick", "Polished Stick", 3}, {"neoprene", "Neoprene", 4}, {"plastic", "Plastic", 5}, {"lens", "Lens", 6},
                {"aluminum_sheet", "Sheet of Aluminium", 7}, {"heating_component", "Heating Component", 8}, {"cooling_component", "Cooling Component", 9},
                {"silicon_carbide", "Silicon Carbide", 10}, {"iron_wheel", "Iron Wheel", 11}, {"wicker", "Wicker", 12}, {"gold_sheet", "Sheet of Gold", 13},
                {"burnt_brick", "Burnt Brick", 14}, {"blank_plan", "Plan - Blank", 16}, {"titanium_sheet", "Sheet of Titanium", 17},
                {"glass_lens", "Glass Lens", 18}, {"titanium_rod", "Titanium Rod", 19}, {"life_core", "Life Core", 20}};
        for (Object[] p : parts) item((String) p[0], (String) p[1], "crafting:" + p[2]);
        Catalog.itemTag("c:plates/aluminum", "aluminum_sheet");
        Catalog.itemTag("c:plates/titanium", "titanium_sheet");
        Catalog.itemTag("c:plates/gold", "gold_sheet");
        Catalog.itemTag("c:plates", "#c:plates/aluminum");
        Catalog.itemTag("c:plates", "#c:plates/titanium");
        Catalog.itemTag("c:plates", "#c:plates/gold");
        // core/Recipes.java addCraftingItems 81-102
        shaped("heating_component", 1, List.of("CC"), Map.of('C', Ingredient.item("silicon_carbide")), "Recipes.java:85");
        String[][] cooling = {{"  P", "PI ", "  P"}, {" P ", " I ", "P P"}, {"P  ", " IP", "P  "}, {"P P", " I ", " P "}};
        for (int i = 0; i < cooling.length; i++) {
            Catalog.RECIPES.add(new Recipe.Shaped("cooling_component" + (i == 0 ? "" : "_" + (i + 1)), List.of(cooling[i]),
                    Map.of('P', Ingredient.tag("minecraft:planks"), 'I', Ingredient.item("minecraft:iron_ingot")), "cooling_component", 1, "Recipes.java:86"));
        }
        shaped("silicon_carbide", 1, List.of(" S ", "FBF", " S "), Map.of('F', Ingredient.tag("minecraft:coals"), 'S', Ingredient.tag("minecraft:sand"),
                'B', Ingredient.item("minecraft:clay")), "Recipes.java:90");
        shaped("iron_wheel", 3, List.of(" W ", "WSW", " W "), Map.of('W', Ingredient.item("minecraft:iron_ingot"), 'S', Ingredient.tag("minecraft:wooden_slabs")),
                "Recipes.java:91");
        shaped("wicker", 1, List.of("CAC", "ACA", "CAC"), Map.of('C', Ingredient.item("minecraft:stick"), 'A', Ingredient.item("minecraft:sugar_cane")),
                "Recipes.java:92");
        shaped("glass_lens", 1, List.of(" P ", "PGP", " P "), Map.of('P', Ingredient.tag("minecraft:planks"), 'G', Ingredient.item("minecraft:glass")),
                "Recipes.java:84");
        shaped("golden_thread", 1, List.of("ABA", "ABA"), Map.of('B', Ingredient.item("polished_stick"), 'A', Ingredient.item("golden_silk")),
                "Recipes.java:83");
        shaped("life_core", 1, List.of("DSR", "FHB", "PAC"), Map.of('D', Ingredient.item("minecraft:dandelion"), 'S', Ingredient.tag("minecraft:saplings"),
                'R', Ingredient.item("minecraft:poppy"), 'F', Ingredient.tag("minecraft:fishes"), 'H', Ingredient.item("droplet_health"),
                'B', Ingredient.tag("mariculture:bait"), 'P', Ingredient.item("minecraft:potato"), 'A', Ingredient.item("minecraft:lily_pad"),
                'C', Ingredient.item("minecraft:carrot")), "Recipes.java:81");
        // Recipes.java:69-70, polished wood (Fishery.java:288-289 make the polished stick from polished planks; the vat makes them).
        shaped("minecraft:book", 1, List.of("PP", "PW"), Map.of('P', Ingredient.item("minecraft:paper"), 'W', Ingredient.item("wicker")), "Recipes.java:70");
        shaped("minecraft:piston", 1, List.of("TTT", "#X#", "#R#"), Map.of('T', Ingredient.tag("minecraft:planks"), '#', Ingredient.item("minecraft:cobblestone"),
                'X', Ingredient.item("aluminum_ingot"), 'R', Ingredient.item("minecraft:redstone")), "Recipes.java:69");
    }

    private static void shaped(String result, int count, List<String> pattern, Map<Character, Ingredient> key, String legacy) {
        String id = result.startsWith("minecraft:") ? result.substring(10) + "_from_mariculture" : result;
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, result, count, legacy));
    }

    private static void upgrades() {
        for (Upgrades.Spec u : Upgrades.all()) {
            Catalog.ITEMS.add(new ItemSpec(u.id(), u.englishName(), "upgrade:" + u.legacyMeta(), "upgrade/" + u.id(), 1));
            Catalog.itemTag("mariculture:upgrades", u.id());
        }
        // core/Recipes.java:119-161 (upgrade recipes; each tier consumes the previous one).
        up("basic_storage_upgrade", List.of("WPW", "DCD", "WPW"), Map.of('D', Ingredient.item("minecraft:copper_ingot"), 'P', Ingredient.tag("minecraft:planks"),
                'C', Ingredient.item("minecraft:chest"), 'W', Ingredient.item("wicker")));
        up("standard_storage_upgrade", List.of("WCW", "CUC", "STS"), Map.of('C', Ingredient.item("minecraft:copper_ingot"), 'S', Ingredient.tag("minecraft:wooden_slabs"),
                'T', Ingredient.item("minecraft:chest"), 'W', Ingredient.item("wicker"), 'U', Ingredient.item("basic_storage_upgrade")));
        up("advanced_storage_upgrade", List.of("CSC", "AUA", "WTW"), Map.of('C', Ingredient.item("minecraft:copper_ingot"), 'S', Ingredient.tag("minecraft:wooden_slabs"),
                'T', Ingredient.item("minecraft:chest"), 'A', Ingredient.item("aluminum_sheet"), 'W', Ingredient.item("wicker"), 'U', Ingredient.item("standard_storage_upgrade")));
        up("ultimate_storage_upgrade", List.of("GWG", "WUW", "ATA"), Map.of('G', Ingredient.item("minecraft:gold_ingot"), 'T', Ingredient.item("minecraft:chest"),
                'A', Ingredient.item("aluminum_sheet"), 'W', Ingredient.item("wicker"), 'U', Ingredient.item("advanced_storage_upgrade")));
        up("basic_cooling_upgrade", List.of(" S ", "CBC", " S "), Map.of('S', Ingredient.item("minecraft:snowball"), 'B', Ingredient.item("minecraft:snow_block"),
                'C', Ingredient.item("cooling_component")));
        up("standard_cooling_upgrade", List.of("ACA", "SUS", "CAC"), Map.of('S', Ingredient.item("minecraft:snowball"), 'A', Ingredient.item("aluminum_ingot"),
                'C', Ingredient.item("cooling_component"), 'U', Ingredient.item("basic_cooling_upgrade")));
        up("advanced_cooling_upgrade", List.of("CTC", "IUI", "TRT"), Map.of('I', Ingredient.item("minecraft:ice"), 'R', Ingredient.item("minecraft:iron_ingot"),
                'T', Ingredient.item("aluminum_ingot"), 'C', Ingredient.item("cooling_component"), 'U', Ingredient.item("standard_cooling_upgrade")));
        up("ultimate_cooling_upgrade", List.of("TCT", "IUI", "CDC"), Map.of('I', Ingredient.item("minecraft:ice"), 'D', Ingredient.item("aluminum_ingot"),
                'T', Ingredient.item("titanium_ingot"), 'C', Ingredient.item("cooling_component"), 'U', Ingredient.item("advanced_cooling_upgrade")));
        up("basic_heating_upgrade", List.of("HIH"), Map.of('I', Ingredient.item("minecraft:iron_ingot"), 'H', Ingredient.item("heating_component")));
        up("standard_heating_upgrade", List.of("A A", "HUH", " A "), Map.of('A', Ingredient.item("aluminum_ingot"), 'H', Ingredient.item("heating_component"),
                'U', Ingredient.item("basic_heating_upgrade")));
        up("advanced_heating_upgrade", List.of("IHI", "TUT", "IHI"), Map.of('T', Ingredient.item("aluminum_ingot"), 'I', Ingredient.item("minecraft:iron_ingot"),
                'H', Ingredient.item("heating_component"), 'U', Ingredient.item("standard_heating_upgrade")));
        up("ultimate_heating_upgrade", List.of("TDT", "HUH", "GTG"), Map.of('G', Ingredient.item("minecraft:gold_ingot"), 'T', Ingredient.item("titanium_ingot"),
                'D', Ingredient.item("minecraft:blaze_rod"), 'H', Ingredient.item("heating_component"), 'U', Ingredient.item("advanced_heating_upgrade")));
        up("basic_purity_upgrade", List.of(" A ", "PDP", " A "), Map.of('D', Ingredient.item("minecraft:red_dye"), 'A', Ingredient.item("aluminum_ingot"),
                'P', Ingredient.item("white_pearl")));
        up("filter_upgrade", List.of("MPM", "CUC", "AMA"), Map.of('A', Ingredient.item("aluminum_ingot"), 'M', Ingredient.item("magnesium_ingot"),
                'P', Ingredient.item("blue_pearl"), 'C', Ingredient.item("cooling_component"), 'U', Ingredient.item("basic_purity_upgrade")));
        up("advanced_filter_upgrade", List.of("UAU"), Map.of('A', Ingredient.item("aluminum_ingot"), 'U', Ingredient.item("filter_upgrade")));
        up("standard_purity_upgrade", List.of("APA", "DUD", "APA"), Map.of('D', Ingredient.item("droplet_health"), 'A', Ingredient.item("aluminum_ingot"),
                'P', Ingredient.item("white_pearl"), 'U', Ingredient.item("basic_purity_upgrade")));
        up("advanced_purity_upgrade", List.of("RFR", "PUP", "ARA"), Map.of('R', Ingredient.item("droplet_health"), 'F', Ingredient.item("raw_angelfish"),
                'P', Ingredient.item("white_pearl"), 'U', Ingredient.item("standard_purity_upgrade"), 'A', Ingredient.item("droplet_aqua")));
        up("ultimate_purity_upgrade", List.of("RWR", "AUA", "PFP"), Map.of('R', Ingredient.item("droplet_health"), 'A', Ingredient.item("droplet_aqua"),
                'P', Ingredient.item("minecraft:glistering_melon_slice"), 'U', Ingredient.item("advanced_purity_upgrade"), 'F', Ingredient.item("raw_koi"),
                'W', Ingredient.item("white_pearl")));
        up("basic_impurity_upgrade", List.of(" B ", "ENE", " B "), Map.of('N', Ingredient.item("minecraft:nether_wart"), 'E', Ingredient.item("minecraft:fermented_spider_eye"),
                'B', Ingredient.item("minecraft:bone")));
        up("salinator_upgrade", List.of("ASA", "SUS", "MAM"), Map.of('A', Ingredient.item("aluminum_ingot"), 'S', Ingredient.item("salt"),
                'M', Ingredient.item("magnesium_ingot"), 'U', Ingredient.item("basic_impurity_upgrade")));
        up("advanced_salinator_upgrade", List.of("UAU"), Map.of('A', Ingredient.item("aluminum_ingot"), 'U', Ingredient.item("salinator_upgrade")));
        up("standard_impurity_upgrade", List.of(" B ", "PUP", " B "), Map.of('B', Ingredient.item("minecraft:spider_eye"), 'P', Ingredient.item("droplet_poison"),
                'U', Ingredient.item("basic_impurity_upgrade")));
        up("advanced_impurity_upgrade", List.of("PDP", "SUS", "HPH"), Map.of('D', Ingredient.item("raw_undead_fish"), 'P', Ingredient.item("droplet_poison"),
                'H', Ingredient.item("droplet_attack"), 'S', Ingredient.item("raw_blaasop"), 'U', Ingredient.item("standard_impurity_upgrade")));
        up("ultimate_impurity_upgrade", List.of("PDP", "DUD", "HLH"), Map.of('P', Ingredient.item("droplet_poison"), 'L', Ingredient.item("raw_boneless_fish"),
                'D', Ingredient.item("droplet_attack"), 'U', Ingredient.item("advanced_impurity_upgrade"), 'H', Ingredient.item("minecraft:fermented_spider_eye")));
        up("ethereal_upgrade", List.of("PUP", "GEG", "PDP"), Map.of('P', Ingredient.item("minecraft:ender_pearl"), 'G', Ingredient.item("minecraft:gold_ingot"),
                'E', Ingredient.item("minecraft:ender_eye"), 'D', Ingredient.item("minecraft:diamond"), 'U', Ingredient.item("minecraft:redstone_torch")));
        up("basic_speed_upgrade", List.of(" W ", "WSW", " W "), Map.of('W', Ingredient.item("minecraft:sugar"), 'S', Ingredient.item("minecraft:iron_ingot")));
        up("standard_speed_upgrade", List.of(" S ", "AUA", " S "), Map.of('A', Ingredient.item("aluminum_ingot"), 'S', Ingredient.item("minecraft:sugar"),
                'U', Ingredient.item("basic_speed_upgrade")));
        up("advanced_speed_upgrade", List.of("ASA", "TUT", "SNS"), Map.of('A', Ingredient.item("aluminum_ingot"), 'S', Ingredient.item("minecraft:sugar"),
                'T', Ingredient.item("titanium_ingot"), 'N', Ingredient.item("minecraft:ice"), 'U', Ingredient.item("standard_speed_upgrade")));
        up("ultimate_speed_upgrade", List.of("TRT", "SUS", "ARA"), Map.of('A', Ingredient.item("aluminum_sheet"), 'S', Ingredient.item("minecraft:sugar"),
                'T', Ingredient.item("titanium_ingot"), 'R', Ingredient.item("minecraft:powered_rail"), 'U', Ingredient.item("advanced_speed_upgrade")));
        // Fishery.java:409-410 (eternal upgrades), 330 (AquaScum without MariTech).
        up("eternal_male_upgrade", List.of("WEW", "FRF", "DWD"), Map.of('W', Ingredient.item("minecraft:light_blue_wool"), 'E', Ingredient.tag("c:storage_blocks/emerald"),
                'F', Ingredient.item("raw_dragonfish"), 'R', Ingredient.item("minecraft:dragon_egg"), 'D', Ingredient.item("minecraft:diamond")));
        up("eternal_female_upgrade", List.of("WEW", "FRF", "DWD"), Map.of('W', Ingredient.item("minecraft:pink_wool"), 'E', Ingredient.tag("c:storage_blocks/emerald"),
                'F', Ingredient.item("raw_dragonfish"), 'R', Ingredient.item("minecraft:dragon_egg"), 'D', Ingredient.item("minecraft:diamond")));
    }

    private static void up(String id, List<String> pattern, Map<Character, Ingredient> key) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, 1, "Recipes.java:119-161"));
    }
}
