package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.fish.ShippedSpecies;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.fishery.Bait;
import mariculture.domain.fishery.Droplets;

/**
 * Fishery items: live and raw fish, fish eggs, bait, droplets. Machines and tools register with their
 * own slices. Legacy: fishery/Fishery.java registerItems 186-197, ItemFishy, ItemVanillaFish, ItemEgg,
 * ItemBait, ItemDroplet.
 */
final class FisheryContent {
    private FisheryContent() {}

    static void register() {
        fish();
        bait();
        droplets();
        machines();
        lamps();
        tools();
    }

    // fishery/items/ItemScanner and ItemTemperatureControl (Fishery.java:196-198); recipes Fishery.java:302, 304, 311.
    private static void tools() {
        Catalog.ITEMS.add(new ItemSpec("scanner", "Fish DNA Scanner", "scanner", "scanner", 1, new ItemKind.Special("scanner", 0)));
        Catalog.ITEMS.add(new ItemSpec("temperature_control", "Temperature Control", "temperature.control", "temperature_control", 1,
                new ItemKind.Special("temperature_control", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped("scanner", List.of("WPW", "WFW", "WBW"), Map.of('W', Ingredient.tag("mariculture:droplets"),
                'P', Ingredient.tag("mariculture:pearls"), 'F', Ingredient.tag("minecraft:fishes"), 'B', Ingredient.item("minecraft:redstone")),
                "scanner", 1, "Fishery.java:304"));
        Catalog.RECIPES.add(new Recipe.Shaped("temperature_control", List.of(" H ", "CTC", " H "), Map.of('H', Ingredient.item("heating_component"),
                'C', Ingredient.item("cooling_component"), 'T', Ingredient.item("titanium_sheet")), "temperature_control", 1, "Fishery.java:302"));
        Catalog.RECIPES.add(new Recipe.Shaped("aquascum_upgrade", List.of("DFD", "CTH", "DSD"), Map.of('D', Ingredient.item("golden_thread"),
                'F', Ingredient.item("advanced_filter_upgrade"), 'C', Ingredient.item("ultimate_cooling_upgrade"), 'T', Ingredient.item("temperature_control"),
                'H', Ingredient.item("ultimate_heating_upgrade"), 'S', Ingredient.item("advanced_salinator_upgrade")), "aquascum_upgrade", 1, "Fishery.java:311"));
    }

    // fishery/Fishery.java:240-241 (blocks, glass, hardness 1), :419-423 (recipes: 8 from a raw neon tetra, 4 from a flux droplet).
    private static void lamps() {
        for (PearlColor color : PearlColor.values()) {
            String c = color.id();
            String id = c + "_neon_lamp";
            String title = Character.toUpperCase(c.charAt(0)) + c.substring(1);
            Catalog.add(new BlockSpec(id, title + " Neon Lamp", "lamps_on:" + color.ordinal(), Shape.LAMP, Sound.GLASS, 1f, 1f, false,
                    HarvestLevel.ANY, Map.of("on", id + "_on", "off", id + "_off"), true));
            Catalog.blockTag("mariculture:neon_lamps", id);
            for (String[] core : new String[][] {{"tetra", "raw_neon_tetra", "8", "Fishery.java:421"}, {"flux", "droplet_electric", "4", "Fishery.java:422"}}) {
                Catalog.RECIPES.add(new Recipe.Shaped(id + "_from_" + core[0], List.of("PGP", "GFG", "PGP"), Map.of('P', Ingredient.item(c + "_pearl"),
                        'G', Ingredient.item("minecraft:glass"), 'F', Ingredient.item(core[1])), id, Integer.parseInt(core[2]), core[3]));
            }
        }
    }

    // fishery/Fishery.java:296-300 (recipes); blocks: BlockRenderedMachine feeder (hardness 0.5, explosion
    // resistance 100), BlockTank hatchery and fish tank, BlockRenderedMachineMulti sifter, BlockTicking net.
    private static void machines() {
        Catalog.add(new BlockSpec("fish_feeder", "Fish Feeder", "renderedMachines:2", Shape.MACHINE, Sound.WOOD, 0.5f, 100f, false,
                HarvestLevel.ANY, Map.of("all", "fish_feeder"), true));
        Catalog.add(new BlockSpec("hatchery", "Hatchery", "tanks:hatchery", Shape.MACHINE_GLASS, Sound.GLASS, 1f, 3f, false,
                HarvestLevel.ANY, Map.of("all", "hatchery"), true));
        Catalog.add(new BlockSpec("fish_tank", "Fish Tank", "tanks:fish", Shape.MACHINE_GLASS, Sound.GLASS, 1f, 3f, false,
                HarvestLevel.ANY, Map.of("all", "fish_tank"), true));
        Catalog.add(new BlockSpec("sifter", "Sifter", "renderedMultiMachines:4", Shape.MACHINE_GLASS, Sound.WOOD, 1f, 3f, false,
                HarvestLevel.ANY, Map.of("all", "sifter"), true));
        Catalog.add(new BlockSpec("fishing_net", "Fishing Net", "net", Shape.NET, Sound.WOOL, 0.05f, 0.05f, false,
                HarvestLevel.ANY, Map.of("all", "fishing_net"), true));
        // core/blocks/BlockMachine FISH_SORTER: hardness 1.5, axe; one texture per absolute face. Factory.java:119.
        Catalog.add(new BlockSpec("fish_sorter", "Fish Sorter", "machines:6", Shape.MACHINE, Sound.WOOD, 1.5f, 1.5f, false,
                HarvestLevel.ANY, Map.of("down", "fish_sorter_down", "up", "fish_sorter_up", "north", "fish_sorter_north",
                        "south", "fish_sorter_south", "west", "fish_sorter_west", "east", "fish_sorter_east"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("fish_sorter", List.of("BPY", "GFA", "RCW"), Map.of('B', Ingredient.item("minecraft:black_dye"),
                'P', Ingredient.tag("mariculture:pearls"), 'Y', Ingredient.item("minecraft:yellow_dye"), 'G', Ingredient.item("minecraft:green_dye"),
                'F', Ingredient.tag("minecraft:fishes"), 'A', Ingredient.item("minecraft:cyan_dye"), 'R', Ingredient.item("minecraft:red_dye"), 'C', Ingredient.item("base_wood"),
                'W', Ingredient.item("minecraft:white_dye")), "fish_sorter", 1, "factory/Factory.java:119"));
        for (String axe : List.of("fish_feeder", "sifter", "fish_sorter")) Catalog.blockTag("minecraft:mineable/axe", axe);
        Ingredient wicker = Ingredient.item("wicker");
        Ingredient fish = Ingredient.tag("minecraft:fishes");
        Catalog.RECIPES.add(new Recipe.Shaped("fishing_net", List.of("SWS", "WWW", "SWS"), Map.of('S', Ingredient.item("minecraft:stick"),
                'W', Ingredient.item("minecraft:string")), "fishing_net", 4, "Fishery.java:296"));
        Catalog.RECIPES.add(new Recipe.Shaped("sifter", List.of("PNP", "S S"), Map.of('S', Ingredient.item("minecraft:stick"),
                'P', Ingredient.tag("minecraft:planks"), 'N', Ingredient.item("fishing_net")), "sifter", 2, "Fishery.java:297"));
        Catalog.RECIPES.add(new Recipe.Shaped("hatchery", List.of("WWW", "WEW", "WWW"), Map.of('W', wicker, 'E', Ingredient.item("fish_eggs")),
                "hatchery", 1, "Fishery.java:298"));
        Catalog.RECIPES.add(new Recipe.Shaped("fish_feeder", List.of("WFW", "WCW", "WFW"), Map.of('W', wicker, 'F', fish,
                'C', Ingredient.item("minecraft:chest")), "fish_feeder", 1, "Fishery.java:299"));
        Catalog.RECIPES.add(new Recipe.Shaped("fish_tank", List.of("AGA", "GFG", "AGA"), Map.of('A', Ingredient.item("aluminum_ingot"),
                'G', Ingredient.item("minecraft:glass"), 'F', fish), "fish_tank", 1, "Fishery.java:300"));
    }

    private static void fish() {
        for (ShippedSpecies.Entry e : ShippedSpecies.ALL) {
            // Live fish: ItemFishy with the species' genome; display name follows the genome.
            Catalog.ITEMS.add(new ItemSpec(e.id(), e.englishName(), "fishy{SpeciesID:" + e.id() + "}", "fish/" + e.id(), 64,
                    new ItemKind.LiveFish("mariculture:" + e.id())));
            Catalog.itemTag("mariculture:live_fish", e.id());
            if (e.hasOwnRawItem()) {
                String raw = "raw_" + e.id();
                ItemKind kind = e.edible() ? new ItemKind.RawFish("mariculture:" + e.id(), e.hunger(), e.saturation()) : ItemKind.PLAIN;
                Catalog.ITEMS.add(new ItemSpec(raw, "Raw " + e.englishName(), "fish:" + e.id(), "fish/" + e.id(), 64, kind));
                Catalog.itemTag("mariculture:raw_fish", raw);
                if (e.edible()) {
                    Catalog.itemTag("minecraft:fishes", raw);
                    Catalog.itemTag("c:foods/raw_fish", raw);
                    // Plugins.java:71 (1.2.4 HarvestCraft plugin, PLUG-023): Pam's HarvestCraft 2 cooks anything in its
                    // c:rawfish and c:fishes tags (forge: on 1.20.x), so the fish join those instead of extra recipes.
                    Catalog.itemTag("c:rawfish", raw);
                    Catalog.itemTag("c:fishes", raw);
                }
            }
        }
        // Species added by other data packs have no item of their own; they live in this one.
        Catalog.ITEMS.add(new ItemSpec("live_fish", "Fish", "fishy", "fish/live_fish", 64, new ItemKind.LiveFish(null)));
        Catalog.itemTag("mariculture:live_fish", "live_fish");
        Catalog.ITEMS.add(new ItemSpec("fish_eggs", "Fish Eggs", "eggs.fish", "fish/fish_eggs", 64, new ItemKind.FishEggs()));
        Catalog.ITEMS.add(new ItemSpec("fish_meal", "Fish Meal", "materials:FISH_MEAL", "fish_meal", 64));
        // fishery/Fishery.java:403 (two mushroom fish and a bowl make mushroom stew).
        Catalog.RECIPES.add(new Recipe.Shapeless("mushroom_stew_from_shroom_fish", List.of(Ingredient.item("raw_brown_shroom_fish"),
                Ingredient.item("raw_red_shroom_fish"), Ingredient.item("minecraft:bowl")), "minecraft:mushroom_stew", 1, "Fishery.java:403"));
    }

    private static void bait() {
        for (Bait.Kind b : Bait.KINDS) {
            Catalog.ITEMS.add(new ItemSpec(b.id(), b.englishName(), "bait:" + b.legacyMeta(), b.id(), 64,
                    new ItemKind.Food(Bait.hunger(b.quality()), -1f, false, null, null)));
            Catalog.itemTag("mariculture:bait", b.id());
        }
    }

    private static void droplets() {
        for (Droplets.Droplet d : Droplets.ALL) {
            Catalog.ITEMS.add(new ItemSpec(d.id(), d.englishName(), "droplet:" + d.legacyMeta(), d.id(), 64));
            Catalog.itemTag("mariculture:droplets", d.id());
        }
        Catalog.ITEMS.add(new ItemSpec("metal_droplet", "Metal Droplet", "none", "metal_droplet", 64, new ItemKind.Special("metal_droplet", 0)));
        Catalog.itemTag("mariculture:droplets", "metal_droplet");
    }
}
