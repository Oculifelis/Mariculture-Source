package mariculture.domain.content;

import java.util.List;
import java.util.Map;

/**
 * Metallurgy blocks (core): crucible, vat, casters, storage tanks, and the vat's products heat glass, plastic and
 * polished wood. Hardness from core/blocks/BlockMachineMulti (crucible: the switch falls through to 10, explosion
 * resistance 50), BlockRenderedMachine (casters 1.5), BlockRenderedMachineMulti (vat 2.5), BlockTank (0.5 / 0.75 /
 * 1.5), BlockGlass (0.5, resistance 5), BlockTransparent (Core.java:248, 1), BlockWood (2). Recipes core/Recipes.java:47-59.
 *
 * <p>Simplified models: casters and the vat are plain cubes (legacy drew them with custom renderers); heat glass and
 * plastic have no connected textures.
 */
final class MetalContent {
    private MetalContent() {}

    static void register() {
        Map<String, String> crucibleFaces = Map.of("down", "base_brick", "up", "base_brick", "north", "crucible", "south", "crucible",
                "west", "crucible", "east", "crucible");
        machine("crucible", "Crucible Furnace", "machinesMulti:0", Shape.MACHINE, Sound.STONE, 10f, 50f, crucibleFaces);
        machine("vat", "Vat", "renderedMultiMachines:3", Shape.MACHINE, Sound.METAL, 2.5f, 2.5f, Map.of("all", "vat"));
        machine("ingot_caster", "Ingot Caster", "renderedMachines:9", Shape.MACHINE, Sound.STONE, 1.5f, 1.5f, Map.of("all", "caster"));
        machine("nugget_caster", "Nugget Caster", "renderedMachines:11", Shape.MACHINE, Sound.STONE, 1.5f, 1.5f, Map.of("all", "caster"));
        machine("block_caster", "Block Caster", "renderedMachines:10", Shape.MACHINE, Sound.STONE, 1.5f, 1.5f, Map.of("all", "caster"));
        machine("copper_tank", "Copper Tank", "tanks:0", Shape.MACHINE_GLASS, Sound.GLASS, 0.5f, 0.5f, Map.of("all", "copper_tank"));
        machine("aluminum_tank", "Aluminium Tank", "tanks:5", Shape.MACHINE_GLASS, Sound.GLASS, 0.75f, 0.75f, Map.of("all", "aluminum_tank"));
        machine("titanium_tank", "Titanium Tank", "tanks:6", Shape.MACHINE_GLASS, Sound.GLASS, 1.5f, 1.5f, Map.of("all", "titanium_tank"));

        // Anvil hardness 25, autohammer 1.5 (BlockRenderedMachine); the autohammer had a custom model only.
        machine("anvil", "Anvil", "renderedMachines:8", Shape.MACHINE, Sound.STONE, 25f, 25f, Map.of("all", "anvil"));
        machine("autohammer", "Autohammer", "renderedMachines:1", Shape.MACHINE, Sound.STONE, 1.5f, 1.5f, Map.of("all", "base_brick"));
        Catalog.ITEMS.add(new ItemSpec("hammer", "Hammer", "hammer", "hammer", 1, new ItemKind.Special("hammer", mariculture.domain.metal.Hammer.USES)));
        Catalog.ITEMS.add(new ItemSpec("worked_item", "Unworked Item", "worked", "worked_item", 1, new ItemKind.Special("worked", 0)));
        Catalog.itemTag("minecraft:pickaxes", "hammer");
        Catalog.itemTag("mariculture:hammer_repair", "burnt_brick");

        // Fluid containers (Core.java:183-184, ItemBottle) and the placed void bottle (BlockTank BOTTLE, hardness 0.1).
        Catalog.ITEMS.add(new ItemSpec("ladle", "Ladle", "ladle", "ladle", 1, new ItemKind.FluidContainer(mariculture.domain.metal.MetalRates.ORE, false)));
        Catalog.ITEMS.add(new ItemSpec("titanium_bucket", "Titanium Bucket", "bucket.titanium", "titanium_bucket", 1, new ItemKind.FluidContainer(16000, false)));
        Catalog.ITEMS.add(new ItemSpec("heat_bottle", "Heat Resistant Bottle", "bottles:20", "heat_bottle", 16,
                new ItemKind.FluidContainer(mariculture.domain.fluid.HeatBottle.CAPACITY, true)));
        machine("void_bottle", "Void Bottle", "bottles:0", Shape.MACHINE_GLASS, Sound.GLASS, 0.1f, 0.1f, Map.of("all", "void_bottle"));
        // core/blocks/BlockGround: bubbles 0.5, ancient 0.7, shovel; ancient sand smelts into heat glass (WorldPlus.java:126).
        Catalog.add(new BlockSpec("ancient_sand", "Ancient Sand", "sands:1", Shape.ANCIENT_SAND, Sound.SAND, 0.7f, 0.7f, false, HarvestLevel.ANY,
                Map.of("all", "ancient_sand"), true));
        Catalog.add(new BlockSpec("bubbling_sand", "Bubbling Sand", "sands:0", Shape.BUBBLE_SAND, Sound.SAND, 0.5f, 0.5f, false, HarvestLevel.ANY,
                Map.of("all", "minecraft:block/sand"), true));
        for (String id : List.of("ancient_sand", "bubbling_sand")) Catalog.blockTag("minecraft:mineable/shovel", id);
        Catalog.RECIPES.add(new Recipe.Smelting("heat_glass_from_ancient_sand", Ingredient.item("ancient_sand"), "heat_glass", 0.025f, 200, "WorldPlus.java:126"));
        // Storage bookshelf (BlockMachine BOOKSHELF, hardness 1.5, axe; Recipes.java:52); thermometer and dragon spawn egg
        // (core/items/ItemCrafting.onItemUse; thermometer recipe Fishery.java:295; the egg is made by MariTech).
        Catalog.add(new BlockSpec("storage_bookshelf", "Storage Bookshelf", "machines:3", Shape.MACHINE, Sound.WOOD, 1.5f, 1.5f, false, HarvestLevel.ANY,
                Map.of("down", "minecraft:block/oak_planks", "up", "minecraft:block/oak_planks", "north", "storage_bookshelf", "south", "storage_bookshelf",
                        "west", "storage_bookshelf", "east", "storage_bookshelf"), true));
        Catalog.blockTag("minecraft:mineable/axe", "storage_bookshelf");
        Catalog.ITEMS.add(new ItemSpec("thermometer", "Thermometer", "crafting:29", "thermometer", 1, new ItemKind.Special("thermometer", 0)));
        Catalog.ITEMS.add(new ItemSpec("dragon_egg_item", "Dragon Spawn Egg", "crafting:21", "dragon_egg_item", 64, new ItemKind.Special("dragon_spawn", 0)));
        Catalog.add(new BlockSpec("heat_glass", "Heat Resistant Glass", "glass:0", Shape.GLASS, Sound.GLASS, 0.5f, BlockSpec.fromLegacySetResistance(5f),
                false, HarvestLevel.ANY, Map.of("all", "heat_glass"), true));
        Catalog.add(new BlockSpec("plastic_block", "Plastic", "transparent:0", Shape.GLASS, Sound.STONE, 1f, 1f, false, HarvestLevel.ANY,
                Map.of("all", "plastic_block"), true));
        Catalog.add(new BlockSpec("polished_log", "Polished Log", "woods:2", Shape.PILLAR, Sound.WOOD, 2f, 2f, false, HarvestLevel.ANY,
                Map.of("end", "polished_log_top", "side", "polished_log"), true));
        Catalog.add(new BlockSpec("polished_planks", "Polished Planks", "woods:1", Shape.CUBE, Sound.WOOD, 2f, 2f, false, HarvestLevel.ANY,
                Map.of("all", "polished_planks"), true));
        for (String id : List.of("polished_log", "polished_planks")) Catalog.blockTag("minecraft:mineable/axe", id);
        Catalog.itemTag("c:glass_blocks", "heat_glass");

        Ingredient burnt = Ingredient.item("burnt_brick"), copper = Ingredient.item("minecraft:copper_ingot");
        shaped("crucible", 2, List.of(" L ", "BGB", "HCH"), Map.of('L', Ingredient.item("minecraft:lava_bucket"), 'B', burnt,
                'G', Ingredient.item("copper_tank"), 'H', Ingredient.item("heating_component"), 'C', Ingredient.item("base_brick")), "Recipes.java:53");
        shaped("vat", 1, List.of("C C", "C C", "CCC"), Map.of('C', copper), "Recipes.java:56");
        shaped("block_caster", 1, List.of("BBB", "B B", "BBB"), Map.of('B', burnt), "Recipes.java:57");
        shaped("ingot_caster", 1, List.of(" B ", "BBB", " B "), Map.of('B', burnt), "Recipes.java:58");
        shaped("nugget_caster", 1, List.of(" B ", "B B", " B "), Map.of('B', burnt), "Recipes.java:59");
        shaped("anvil", 1, List.of("CCC", " N ", "BBB"), Map.of('C', Ingredient.item("base_brick"), 'N', Ingredient.item("minecraft:nether_bricks"), 'B', burnt), "Recipes.java:54");
        shaped("autohammer", 1, List.of(" N ", "NCN", " N "), Map.of('C', Ingredient.item("anvil"), 'N', Ingredient.item("hammer")), "Recipes.java:55");
        shaped("hammer", 1, List.of("PP ", " SP", "S  "), Map.of('P', burnt, 'S', Ingredient.item("minecraft:nether_bricks")), "Recipes.java:35");
        shaped("ladle", 1, List.of(" C ", " C ", "C  "), Map.of('C', copper), "Recipes.java:36");
        shaped("titanium_bucket", 1, List.of("T T", " T "), Map.of('T', Ingredient.item("titanium_ingot")), "Recipes.java:37");
        shaped("heat_bottle", 3, List.of("G G", " G "), Map.of('G', Ingredient.item("heat_glass")), "Recipes.java:39");
        Catalog.RECIPES.add(new Recipe.Shapeless("void_bottle", List.of(Ingredient.item("heat_bottle"), Ingredient.item("minecraft:redstone"),
                Ingredient.item("minecraft:ink_sac")), "void_bottle", 8, "Recipes.java:40"));
        shaped("storage_bookshelf", 1, List.of("SPS", "PCP", "SSS"), Map.of('P', Ingredient.tag("minecraft:planks"), 'S', Ingredient.item("minecraft:bookshelf"),
                'C', Ingredient.item("minecraft:chest")), "Recipes.java:52");
        Catalog.RECIPES.add(new Recipe.Shapeless("thermometer", List.of(Ingredient.tag("minecraft:fishes"), Ingredient.item("minecraft:compass")),
                "thermometer", 1, "Fishery.java:295"));
        List<String> tank = List.of("CWC", "WGW", "CWC");
        shaped("copper_tank", 1, tank, Map.of('C', copper, 'W', Ingredient.tag("minecraft:planks"), 'G', Ingredient.item("minecraft:glass")), "Recipes.java:48");
        shaped("aluminum_tank", 1, tank, Map.of('C', Ingredient.item("aluminum_ingot"), 'W', Ingredient.item("minecraft:stone"),
                'G', Ingredient.item("heat_glass")), "Recipes.java:49");
        shaped("titanium_tank", 1, tank, Map.of('C', Ingredient.item("titanium_ingot"), 'W', Ingredient.item("minecraft:quartz_block"),
                'G', Ingredient.item("heat_glass")), "Recipes.java:50");
    }

    private static void machine(String id, String name, String legacy, Shape shape, Sound sound, float hardness, float resistance, Map<String, String> textures) {
        Catalog.add(new BlockSpec(id, name, legacy, shape, sound, hardness, resistance, false, HarvestLevel.ANY, textures, true));
        Catalog.blockTag("minecraft:mineable/pickaxe", id);
    }

    private static void shaped(String id, int count, List<String> pattern, Map<Character, Ingredient> key, String legacy) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, count, legacy));
    }
}
