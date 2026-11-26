package mariculture.domain.content;

import java.util.List;
import java.util.Map;

/**
 * Factory machines (factory/Factory.java, 1.3.0): the sawmill with its plans, chalk, paintbrush and blocks, the fluid
 * dictionary, and the air pump (core/Recipes.java:47). Rendered machines (air
 * pump, geyser) use a {@link MachineModel} built from their legacy renderers.
 */
final class FactoryContent {
    private static final Map<mariculture.domain.factory.Plans.Type, String> PLAN_NAMES = names("Flooring", "Block", "Stairs", "Slabs", "Fence",
            "Gate", "Wall", "Light", "RF Block", "RF Wall");
    private static final Map<mariculture.domain.factory.Plans.Type, String> CUSTOM_NAMES = names("Custom Flooring", "Custom Block", "Custom Stairs",
            "Custom Slabs", "Custom Fence", "Custom Gate", "Custom Wall", "Custom Light", "Custom Redstone Flux Block", "Custom Redstone Flux Wall");

    private FactoryContent() {}

    /** Legacy names in PlansMeta order (en_US.lang mariculture.plans.*, tile.custom*). */
    private static Map<mariculture.domain.factory.Plans.Type, String> names(String... n) {
        Map<mariculture.domain.factory.Plans.Type, String> m = new java.util.EnumMap<>(mariculture.domain.factory.Plans.Type.class);
        for (mariculture.domain.factory.Plans.Type t : mariculture.domain.factory.Plans.Type.values()) m.put(t, n[t.ordinal()]);
        return m;
    }

    static void register() {
        // BlockRenderedMachine AIR_PUMP: hardness 4, pickaxe level 1, bounds 0.2-0.8 x 0-0.9.
        Catalog.add(new BlockSpec("air_pump", "Air Pump", "renderedMachines:0", Shape.MACHINE_MODEL, Sound.METAL, 4f, 4f, true,
                HarvestLevel.STONE, Map.of("particle", "air_pump", "model", "air_pump_model"), true));
        // ModelAirPump drawn by RenderSingleItem: moved to (0.45, 0.45, 0.5) blocks, turned round Z, 1/20 block per unit.
        double ox = 7.2, oy = 7.2, oz = 8, s = 0.8;
        MachineModel.register("air_pump", new MachineModel(MachineModel.Facing.HORIZONTAL, List.of(
                MachineModel.entityBox(ox, oy, oz, s, -6, -6, -4, 10, 14, 8, 15, 25, 64, "model"),
                MachineModel.entityBox(ox, oy, oz, s, -6, 8, -4, 1, 1, 8, 0, 0, 64, "model"),
                MachineModel.entityBox(ox, oy, oz, s, 3, 8, -4, 1, 1, 8, 0, 0, 64, "model"),
                MachineModel.entityBox(ox, oy, oz, s, -7, -8, -5, 1, 10, 10, 30, 0, 64, "model"),
                MachineModel.entityBox(ox, oy, oz, s, 4, -8, -5, 1, 10, 10, 30, 0, 64, "model"))));
        Catalog.RECIPES.add(new Recipe.Shaped("air_pump", List.of("WGW", "PRP", "PMP"), Map.of('W', Ingredient.item("iron_wheel"),
                'G', Ingredient.item("minecraft:glass"), 'R', Ingredient.item("minecraft:redstone"), 'P', Ingredient.tag("minecraft:planks"),
                'M', Ingredient.item("minecraft:piston")), "air_pump", 1, "core/Recipes.java:47"));

        // BlockRenderedMachine GEYSER: hardness 0.85, pickaxe level 1; RenderGeyser draws hopper, stone slab and water.
        Catalog.add(new BlockSpec("geyser", "Geyser", "renderedMachines:3", Shape.MACHINE_MODEL, Sound.METAL, 0.85f, 0.85f, true,
                HarvestLevel.STONE, Map.of("particle", "geyser", "hopper", "minecraft:block/hopper_outside", "slab", "minecraft:block/smooth_stone",
                        "water", "minecraft:block/water_still"), true));
        MachineModel.register("geyser", new MachineModel(MachineModel.Facing.ALL, List.of(
                MachineModel.box(1.6, 0, 1.6, 14.4, 2.4, 14.4, "hopper"),
                MachineModel.box(4, 2.4, 4, 12, 3.84, 12, "slab"),
                MachineModel.box(5.6, 3.84, 5.6, 10.4, 4, 10.4, "water"))));
        Catalog.RECIPES.add(new Recipe.Shaped("geyser", List.of(" W ", " G ", "RCR"), Map.of('W', Ingredient.item("minecraft:water_bucket"),
                'G', Ingredient.item("minecraft:glass"), 'R', Ingredient.item("minecraft:redstone"), 'C', Ingredient.item("base_iron")),
                "geyser", 16, "factory/Factory.java:120"));

        // BlockMachine UNPACKER: hardness 1.5, axe; unpackerTop on top and bottom.
        Catalog.add(new BlockSpec("unpacker", "Unpacker", "machines:7", Shape.MACHINE, Sound.WOOD, 1.5f, 1.5f, false, HarvestLevel.ANY,
                Map.of("down", "unpacker_top", "up", "unpacker_top", "north", "unpacker", "south", "unpacker", "west", "unpacker", "east", "unpacker"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("unpacker", List.of("LLL", "LCL", "LBL"), Map.of('L', Ingredient.tag("minecraft:logs"),
                'C', Ingredient.item("minecraft:crafting_table"), 'B', Ingredient.item("base_wood")), "unpacker", 1, "factory/Factory.java:116"));
        Catalog.blockTag("minecraft:mineable/axe", "unpacker");

        // BlockMachine DICTIONARY_ITEM: hardness 2, axe; base wood on top and bottom.
        Catalog.add(new BlockSpec("automatic_dictionary_converter", "Automatic Dictionary Converter", "machines:1", Shape.MACHINE, Sound.WOOD, 2f, 2f,
                false, HarvestLevel.ANY, Map.of("down", "base_wood", "up", "base_wood", "north", "dictionary", "south", "dictionary",
                        "west", "dictionary", "east", "dictionary"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("automatic_dictionary_converter", List.of(" B ", "FPF", "IMI"), Map.of(
                'B', Ingredient.item("minecraft:writable_book"), 'F', Ingredient.item("minecraft:feather"), 'P', Ingredient.tag("mariculture:pearls"),
                'M', Ingredient.item("base_wood"), 'I', Ingredient.item("minecraft:copper_ingot")), "automatic_dictionary_converter", 1, "factory/Factory.java:118"));
        Catalog.blockTag("minecraft:mineable/axe", "automatic_dictionary_converter");
        // BlockMachine SAWMILL: hardness 2, axe; base wood on top and bottom.
        Catalog.add(new BlockSpec("sawmill", "Sawmill", "machines:2", Shape.MACHINE, Sound.WOOD, 2f, 2f, false, HarvestLevel.ANY,
                Map.of("down", "base_wood", "up", "base_wood", "north", "sawmill", "south", "sawmill", "west", "sawmill", "east", "sawmill"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("sawmill", List.of(" A ", "DWD", "IMI"), Map.of('A', Ingredient.item("minecraft:iron_axe"),
                'D', Ingredient.tag("minecraft:wooden_slabs"), 'W', Ingredient.tag("minecraft:logs"), 'M', Ingredient.item("base_wood"),
                'I', Ingredient.item("minecraft:copper_ingot")), "sawmill", 1, "factory/Factory.java:117"));
        Catalog.blockTag("minecraft:mineable/axe", "sawmill");

        // Planning chalk (ItemChalk, 64 uses): limestone, limestone and white dye in a line, any direction (Factory.java:121-127).
        Catalog.ITEMS.add(new ItemSpec("chalk", "Planning Chalk", "chalk", "chalk", 1, new ItemKind.Special("chalk", mariculture.domain.factory.Plans.CHALK_USES)));
        String[][] chalk = {{"planning_chalk", "LLN"}, {"planning_chalk_down", "L", "L", "N"}, {"planning_chalk_up", "N", "L", "L"},
                {"planning_chalk_diagonal", "N  ", " L ", "  L"}, {"planning_chalk_diagonal_up", "L  ", " L ", "  N"}};
        for (String[] c : chalk) {
            Catalog.RECIPES.add(new Recipe.Shaped(c[0], List.of(c).subList(1, c.length), Map.of('L', Ingredient.tag("mariculture:limestone"),
                    'N', Ingredient.tag("c:dyes/white")), "chalk", 1, "factory/Factory.java:121"));
        }
        // Plans (ItemPlan): drawn with chalk on a block, never crafted.
        for (mariculture.domain.factory.Plans.Type t : mariculture.domain.factory.Plans.Type.values()) {
            Catalog.ITEMS.add(new ItemSpec(t.itemPath(), "Plan - " + PLAN_NAMES.get(t), "plans:" + t.ordinal(), t.itemPath(), 64,
                    new ItemKind.Special("plan", 0)));
        }
        // Blank plan (crafting meta 16): blue dye, black dye, paper and blue dye, shapeless (Factory.java:129).
        Catalog.RECIPES.add(new Recipe.Shapeless("blank_plan", List.of(Ingredient.tag("c:dyes/blue"), Ingredient.tag("c:dyes/black"),
                Ingredient.item("minecraft:paper"), Ingredient.tag("c:dyes/blue")), "blank_plan", 1, "factory/Factory.java:129", Map.of()));
        // Paintbrush (ItemPaintbrush, 128 uses).
        Catalog.ITEMS.add(new ItemSpec("paintbrush", "Paintbrush", "paintbrush", "paintbrush", 1,
                new ItemKind.Special("paintbrush", mariculture.domain.factory.CustomBlocks.PAINTBRUSH_USES)));
        Catalog.RECIPES.add(new Recipe.Shaped("paintbrush", List.of(" WW", " IW", "S  "), Map.of('W', Ingredient.tag("minecraft:wool"),
                'I', Ingredient.item("aluminum_block"), 'S', Ingredient.item("sawmill")), "paintbrush", 1, "factory/Factory.java:131"));
        // The sawmill's blocks (BlockCustom*): hardness and resistance come from their faces; light 15 for the light block.
        for (mariculture.domain.factory.Plans.Type t : mariculture.domain.factory.Plans.Type.values()) {
            Catalog.add(new BlockSpec(t.block, CUSTOM_NAMES.get(t), "custom:" + t.key, Shape.CUSTOM, Sound.STONE, 1.5f, 1.5f, false,
                    HarvestLevel.ANY, Map.of("all", "custom_template"), true));
            Catalog.blockTag("minecraft:mineable/axe", t.block);
        }
        Catalog.blockTag("minecraft:fences", "custom_fence");
        Catalog.blockTag("minecraft:fence_gates", "custom_gate");
        Catalog.blockTag("minecraft:walls", "custom_wall");
        Catalog.blockTag("minecraft:walls", "custom_rf_wall");

        // BlockTank DICTIONARY (tanks:3): hardness 1.5, axe. Legacy had no recipe; the port makes it from a tank and the item converter.
        Catalog.add(new BlockSpec(mariculture.domain.factory.FluidDictionary.TYPE.id(), "Fluid Auto-Dictionary", "tanks:3", Shape.MACHINE_GLASS, Sound.WOOD,
                1.5f, 1.5f, false, HarvestLevel.ANY, Map.of("all", "fluid_dictionary"), true));
        Catalog.RECIPES.add(new Recipe.Shapeless("fluid_dictionary", List.of(Ingredient.item("copper_tank"), Ingredient.item("automatic_dictionary_converter")),
                "fluid_dictionary", 1, "port: no legacy recipe", Map.of()));
        Catalog.blockTag("minecraft:mineable/axe", "fluid_dictionary");

        Catalog.ITEMS.add(new ItemSpec(mariculture.domain.factory.ItemFilter.ID, "Item Filter", "filter", mariculture.domain.factory.ItemFilter.ID, 1,
                new ItemKind.Special("item_filter", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped(mariculture.domain.factory.ItemFilter.ID, List.of("W W", "WNW", " W "), Map.of(
                'W', Ingredient.item("wicker"), 'N', Ingredient.item("fishing_net")), mariculture.domain.factory.ItemFilter.ID, 1, "factory/Factory.java:130"));
    }
}
