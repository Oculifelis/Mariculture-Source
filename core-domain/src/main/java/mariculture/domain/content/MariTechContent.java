package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.diving.Scuba;
import mariculture.domain.maritech.EnergyItem;
import mariculture.domain.maritech.SpeedBoat;
import mariculture.domain.maritech.Rotor;

/**
 * MariTech (a separate mod in 1.3.0 that registered into Mariculture and set HIGH_TECH_ENABLED): here it is part of
 * Mariculture and always on. Batteries and RF upgrade recipes (maritech/extensions/modules/ExtensionCore.java), scuba
 * gear and the air compressor (ExtensionDiving.java), the plastic lens.
 */
final class MariTechContent {
    private MariTechContent() {}

    static void register() {
        Ingredient copper = Ingredient.item("minecraft:copper_ingot"), titanium = Ingredient.item("titanium_ingot"),
                redstone = Ingredient.item("minecraft:redstone");

        // Batteries (ExtensionCore.java:31-32, 42-43); a block of redstone charges one fully (56-64).
        battery(EnergyItem.COPPER, "Copper Battery", "battery.copper", copper);
        battery(EnergyItem.TITANIUM, "Titanium Battery", "battery.titanium", titanium);
        Catalog.ITEMS.add(new ItemSpec(EnergyItem.CREATIVE, "Creative Battery", "crafting:22", EnergyItem.CREATIVE, 1,
                new ItemKind.Special("battery", 0)));

        // RF upgrades (ExtensionCore.java:51-54).
        up("basic_rf_upgrade", List.of(" T ", "CRC"), Map.of('T', Ingredient.item("minecraft:redstone_torch"), 'C', copper, 'R', redstone));
        up("standard_rf_upgrade", List.of("CTC", "QUQ", "RCR"), Map.of('C', copper, 'T', Ingredient.item("minecraft:redstone_torch"),
                'Q', Ingredient.item("minecraft:quartz"), 'R', Ingredient.item("minecraft:repeater"), 'U', Ingredient.item("basic_rf_upgrade")));
        up("advanced_rf_upgrade", List.of(" D ", "RUR", "QCQ"), Map.of('D', redstone, 'Q', Ingredient.item("minecraft:quartz"),
                'R', Ingredient.item("minecraft:repeater"), 'C', Ingredient.item(EnergyItem.COPPER), 'U', Ingredient.item("standard_rf_upgrade")));
        up("ultimate_rf_upgrade", List.of(" C ", "RUR", "QBQ"), Map.of('Q', Ingredient.item("minecraft:comparator"), 'R', redstone,
                'B', Ingredient.item("minecraft:redstone_block"), 'C', Ingredient.item(EnergyItem.TITANIUM), 'U', Ingredient.item("advanced_rf_upgrade")));

        // Plastic lens (ExtensionCore.java:46): neoprene round glass.
        Catalog.RECIPES.add(new Recipe.Shaped("lens", List.of(" N ", "NGN", " N "), Map.of('N', Ingredient.item("neoprene"),
                'G', Ingredient.item("minecraft:glass")), "lens", 1, "maritech/extensions/modules/ExtensionCore.java:46"));

        // Scuba gear (ExtensionDiving.java:38-43, 55-59): armour material SCUBA 15, {0, 0, 1, 0}, enchantability 10.
        Catalog.armorMaterial(new ArmorMaterialSpec("scuba", 15, 0, 0, 1, 0, 10, "neoprene", "scuba", true));
        Catalog.armorMaterial(new ArmorMaterialSpec("scuba_tank", 15, 0, 0, 1, 0, 10, null, "scuba", true));
        // F.L.U.D.D. (ExtensionFactory.java:54: material "FLUDD", durability 0, no protection, enchantability 0).
        Catalog.armorMaterial(new ArmorMaterialSpec("fludd", 0, 0, 0, 0, 0, 0, null, "fludd", false));
        Catalog.armorMaterial(new ArmorMaterialSpec("swimfin", 15, 0, 0, 1, 0, 10, "neoprene", "swimfin", false));
        armor(Scuba.MASK, "Scuba Mask", "scuba.mask", "scuba", ArmorSlot.HEAD);
        armor(Scuba.TANK, "Scuba Tank", "scuba.tank", "scuba_tank", ArmorSlot.CHEST);
        armor(Scuba.SUIT, "Scuba Suit", "scuba.suit", "scuba", ArmorSlot.LEGS);
        armor(Scuba.FINS, "Swimfins", "swimfin", "swimfin", ArmorSlot.FEET);
        Ingredient neoprene = Ingredient.item("neoprene");
        shaped(Scuba.MASK, List.of("PD", "LL"), Map.of('P', Ingredient.tag("c:dyes/black"), 'D', Ingredient.tag("c:dyes/yellow"), 'L', Ingredient.item("lens")));
        shaped(Scuba.TANK, List.of("DSD", "S S", "DSD"), Map.of('S', Ingredient.item("aluminum_sheet"), 'D', Ingredient.tag("c:dyes/yellow")));
        shaped(Scuba.SUIT, List.of("NNN", " N ", "NNN"), Map.of('N', neoprene));
        shaped(Scuba.FINS, List.of("N N", "PDP", "PDP"), Map.of('N', neoprene, 'P', Ingredient.item("plastic"), 'D', Ingredient.item("minecraft:ink_sac")));
        for (String id : List.of(Scuba.MASK, Scuba.TANK, Scuba.SUIT, Scuba.FINS)) Catalog.itemTag("mariculture:diving_gear", id);

        // Air compressor: one block for the legacy 2x2x2 of two bases (ITT/III/W: aluminium sheets, titanium, iron
        // wheel) and two tops (F/PB/III, made in pairs: cooling component, piston, titanium battery).
        Catalog.add(new BlockSpec("air_compressor", "Air Compressor", "renderedMachinesMulti:compressor", Shape.MACHINE, Sound.METAL, 10f, 10f, false,
                HarvestLevel.STONE, Map.of("down", "titanium_block", "up", "air_compressor_top", "north", "air_compressor", "south", "air_compressor",
                        "west", "air_compressor", "east", "air_compressor"), true));
        shaped("air_compressor", List.of("SFS", "PBP", "TWT"), Map.of('S', Ingredient.item("aluminum_sheet"), 'F', Ingredient.item("cooling_component"),
                'P', Ingredient.item("minecraft:piston"), 'B', Ingredient.item(EnergyItem.TITANIUM), 'T', titanium, 'W', Ingredient.item("iron_wheel")));
    }

    /**
     * Power (maritech/extensions/modules/ExtensionFactory.java:62-82, tile/TileGenerator, TileRotor*, TileSluice*;
     * gas turbine core/Recipes.java:51): generator, rotor items and their placed blocks, sluices, gas turbine tank.
     */
    static void registerPower() {
        Ingredient baseIron = Ingredient.item("base_iron"), titanium = Ingredient.item("titanium_ingot");
        // Generator: hardness 2.5, pickaxe 1; front "generator", back "generatorBack", base iron elsewhere
        // (ExtensionIconMachine.getWorldIcon:56).
        Catalog.add(new BlockSpec("generator", "Generator", "machines:9", Shape.MACHINE_MODEL, Sound.METAL, 2.5f, 2.5f, true, HarvestLevel.STONE,
                Map.of("particle", "generator", "front", "generator", "back", "generator_back", "side", "base_iron"), true));
        MachineModel.register("generator", new MachineModel(MachineModel.Facing.HORIZONTAL, List.of(cube("front", "back", "side", "south", "north"))));
        power("generator", 1, List.of(" B ", "CMC", "RIR"), Map.of('B', Ingredient.item(EnergyItem.COPPER), 'C', Ingredient.item("minecraft:comparator"),
                'M', Ingredient.item("magnesium_ingot"), 'R', Ingredient.item("minecraft:redstone"), 'I', baseIron), "ExtensionFactory.java:74");

        // Rotors: items (ItemRotor, durability shown from the block's wear) and the placed blocks they make from base
        // iron. ModelRotor: a hub along the axis and blades reaching about a block either side.
        rotor(Rotor.COPPER, "Copper Rotor", "turbine.copper", "rotorCopper", Ingredient.item("minecraft:copper_ingot"), Ingredient.tag("minecraft:wooden_slabs"));
        rotor(Rotor.ALUMINUM, "Aluminium Rotor", "turbine.aluminum", "rotorAluminum", Ingredient.item("aluminum_ingot"), Ingredient.item("minecraft:stone_slab"));
        rotor(Rotor.TITANIUM, "Titanium Rotor", "turbine.titanium", "rotorTitanium", titanium, Ingredient.item("minecraft:quartz_slab"));

        // Sluices: six-way (no "front" slot, which would make them horizontal); face, rear and sides (ExtensionIconMachine.getWorldIcon:42-52); hardness 5 / 7.5.
        Catalog.add(new BlockSpec("sluice", "Sluice", "machines:3", Shape.MACHINE_MODEL, Sound.METAL, 5f, 5f, true, HarvestLevel.STONE,
                Map.of("particle", "sluice", "face", "sluice", "rear", "sluice_back", "side", "base_iron"), true));
        MachineModel.register("sluice", new MachineModel(MachineModel.Facing.ALL, List.of(cube("face", "rear", "side", "up", "down"))));
        power("sluice", 4, List.of(" H ", "WBW", "IMI"), Map.of('H', Ingredient.item("minecraft:hopper"), 'W', Ingredient.item("iron_wheel"),
                'B', Ingredient.item("minecraft:iron_bars"), 'I', Ingredient.item("aluminum_ingot"), 'M', baseIron), "ExtensionFactory.java:76");
        Catalog.add(new BlockSpec("advanced_sluice", "Advanced Sluice", "machines:8", Shape.MACHINE_MODEL, Sound.METAL, 7.5f, 7.5f, true, HarvestLevel.IRON,
                Map.of("particle", "advanced_sluice", "face", "advanced_sluice", "rear", "advanced_sluice_back", "side", "advanced_sluice_side"), true));
        MachineModel.register("advanced_sluice", new MachineModel(MachineModel.Facing.ALL, List.of(cube("face", "rear", "side", "up", "down"))));
        power("advanced_sluice", 1, List.of("TPT", "TST", "TBT"), Map.of('T', titanium, 'P', Ingredient.item("pressurised_water_bucket"),
                'S', Ingredient.item("advanced_storage_upgrade"), 'B', Ingredient.item("sluice")), "ExtensionFactory.java:77");

        // Gas turbine tank (BlockTank TANK_GAS: hardness 3, gasTank texture), keeps its gas when broken.
        Catalog.add(new BlockSpec("gas_turbine_tank", "Gas Turbine Tank", "tanks:7", Shape.MACHINE_GLASS, Sound.METAL, 3f, 3f, true,
                HarvestLevel.ANY, Map.of("all", "gas_turbine_tank"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("gas_turbine_tank", List.of("ARA", "ATA", "ACA"), Map.of('A', Ingredient.item("aluminum_block"),
                'R', Ingredient.item(Rotor.TITANIUM.item()), 'T', Ingredient.item("titanium_tank"), 'C', baseIron), "gas_turbine_tank", 1, "core/Recipes.java:51"));
    }

    /**
     * Powered machines (maritech/extensions/modules/ExtensionFishery.java:57-59, ExtensionFactory.java:75): incubator,
     * autofisher, mechanized sponge.
     */
    static void registerMachines() {
        // Incubator: one block for the base (hardness 10) and two tops; sides from the tops, top and bottom from the
        // structure's ends. Recipe: the top's row over the base's rows (DFD/CHC + DBD/CHC lose one dye row and one
        // heating component): brown dye, fish, light blue terracotta, heating component, white terracotta, copper battery.
        Catalog.add(new BlockSpec("incubator", "Incubator", "machinesMulti:1-2", Shape.MACHINE, Sound.STONE, 10f, 10f, true, HarvestLevel.STONE,
                Map.of("down", "incubator_bottom", "up", "incubator_top", "north", "incubator_side", "south", "incubator_side",
                        "west", "incubator_side", "east", "incubator_side"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("incubator", List.of("DFD", "CHC", "WBW"), Map.of('D', Ingredient.item("minecraft:brown_dye"),
                'F', Ingredient.tag("minecraft:fishes"), 'C', Ingredient.item("minecraft:light_blue_terracotta"), 'H', Ingredient.item("heating_component"),
                'W', Ingredient.item("minecraft:white_terracotta"), 'B', Ingredient.item(EnergyItem.COPPER)), "incubator", 1,
                "maritech/extensions/modules/ExtensionFishery.java:57-58"));
        // Autofisher (machines:5): base wood top and bottom, "autofisher" sides; placed on still water like a lily pad
        // (ItemBlockMachine.onItemRightClick:48).
        Catalog.add(new BlockSpec("autofisher", "Automatic Fisher", "machines:5", Shape.MACHINE, Sound.WOOD, 2f, 2f, false, HarvestLevel.ANY,
                Map.of("down", "base_wood", "up", "base_wood", "north", "autofisher", "south", "autofisher", "west", "autofisher", "east", "autofisher"), true));
        Catalog.RECIPES.add(new Recipe.Shaped("autofisher", List.of(" F ", "RPR", "WBW"), Map.of('F', Ingredient.tag("minecraft:fishes"),
                'R', Ingredient.item("wooden_fishing_rod"), 'P', Ingredient.tag("minecraft:planks"), 'W', Ingredient.tag("minecraft:logs"),
                'B', Ingredient.item("base_wood")), "autofisher", 1, "maritech/extensions/modules/ExtensionFishery.java:59"));
        // Mechanized sponge (machines:4): base iron top and bottom, "sponge" sides (ExtensionIconMachine.getWorldIcon:32).
        Catalog.add(new BlockSpec("mechanized_sponge", "Mechanized Sponge", "machines:4", Shape.MACHINE, Sound.METAL, 2.5f, 2.5f, true, HarvestLevel.STONE,
                Map.of("down", "base_iron", "up", "base_iron", "north", "mechanized_sponge", "south", "mechanized_sponge", "west", "mechanized_sponge",
                        "east", "mechanized_sponge"), true));
        // DNA machines (machines:10-11, hardness 5, ExtensionMachine.getHardness:148): titanium block top and bottom
        // (BlockMachine.getIcon:96, metas above 9), "extractor"/"injector" sides; recipes ExtensionFishery.java:69-70.
        for (String[] m : new String[][] {{"dna_extractor", "DNA Extractor", "machines:10"}, {"dna_injector", "DNA Injector", "machines:11"}}) {
            Catalog.add(new BlockSpec(m[0], m[1], m[2], Shape.MACHINE, Sound.METAL, 5f, 5f, true, HarvestLevel.STONE,
                    Map.of("down", "titanium_block", "up", "titanium_block", "north", m[0], "south", m[0], "west", m[0], "east", m[0]), true));
        }
        Catalog.RECIPES.add(new Recipe.Shaped("dna_extractor", List.of(" H ", "PSP", "TBT"), Map.of('H', Ingredient.item("life_core"),
                'P', Ingredient.item("titanium_rod"), 'S', Ingredient.item("scanner"), 'T', Ingredient.item(Rotor.TITANIUM.item()),
                'B', Ingredient.item("titanium_block")), "dna_extractor", 1, "maritech/extensions/modules/ExtensionFishery.java:69"));
        Catalog.RECIPES.add(new Recipe.Shaped("dna_injector", List.of(" S ", "GTG", "BEB"), Map.of('S', Ingredient.item("scuba_mask"),
                'G', Ingredient.item("gold_sheet"), 'T', Ingredient.item("thermometer"), 'B', Ingredient.item(EnergyItem.TITANIUM),
                'E', Ingredient.item("dna_extractor")), "dna_injector", 1, "maritech/extensions/modules/ExtensionFishery.java:70"));
        // Fish DNA (ItemFishDNA, 32 uses) and the flux rod (ItemFluxRod: RodType.FLUX, no durability, 250000 FE, 100 a
        // cast; ExtensionFishery.java:60-65: crafted empty, charged with a redstone block or 1000 mB of liquid flux).
        Catalog.ITEMS.add(new ItemSpec("fish_dna", "Fish DNA", "dna", "fish_dna", 1, new ItemKind.Special("dna", 0)));
        // The speed boat (ExtensionTransport.java:22-33, ItemSpeedBoat: stacks to 1).
        Catalog.ITEMS.add(new ItemSpec(SpeedBoat.ID, "Speed Boat", "boat.speed", SpeedBoat.ID, 1, new ItemKind.Special("speed_boat", 0)));
        Catalog.RECIPES.add(new Recipe.Shaped(SpeedBoat.ID, List.of("G F", "AAA"), Map.of('G', Ingredient.item("minecraft:glass_pane"),
                'F', Ingredient.item("cooling_component"), 'A', Ingredient.item("aluminum_ingot")), SpeedBoat.ID, 1,
                "maritech/extensions/modules/ExtensionTransport.java:33"));
        Catalog.ITEMS.add(new ItemSpec(EnergyItem.FLUX_ROD, "Flux Rod", "rod.flux", EnergyItem.FLUX_ROD, 1,
                new ItemKind.Rod(mariculture.domain.fish.Species.Rod.FLUX, 0, 0, null)));
        Catalog.itemTag("mariculture:fishing_rods", EnergyItem.FLUX_ROD);
        Catalog.RECIPES.add(new Recipe.Shaped(EnergyItem.FLUX_ROD, List.of("  R", " RS", "B S"), Map.of('R', Ingredient.item("titanium_fishing_rod"),
                'S', Ingredient.item("minecraft:string"), 'B', Ingredient.item(EnergyItem.TITANIUM)), EnergyItem.FLUX_ROD, 1,
                "maritech/extensions/modules/ExtensionFishery.java:60"));
        Catalog.RECIPES.add(new Recipe.Shapeless(EnergyItem.FLUX_ROD + "_charged", List.of(Ingredient.item(EnergyItem.FLUX_ROD),
                Ingredient.item("minecraft:redstone_block")), EnergyItem.FLUX_ROD, 1, "maritech/extensions/modules/ExtensionFishery.java:64",
                Map.of(EnergyItem.KEY, EnergyItem.spec("mariculture:" + EnergyItem.FLUX_ROD).capacity())));

        // Pressure vessel (machines_multi_rendered:2, hardness 15) and the F.L.U.D.D. with its stand (machines_single_rendered:4,
        // hardness 1, placed by using the F.L.U.D.D. on a block and broken back into it); ExtensionFactory.java:76,78.
        Catalog.add(new BlockSpec("pressure_vessel", "Pressure Vessel", "renderedMachinesMulti:2", Shape.MACHINE, Sound.METAL, 15f, 15f, true,
                HarvestLevel.STONE, Map.of("all", "pressure_vessel"), true));
        power("pressure_vessel", 1, List.of("WLW", "PTP", "PSP"), Map.of('W', Ingredient.item("iron_wheel"), 'L', Ingredient.item("minecraft:lapis_block"),
                'P', Ingredient.item("titanium_sheet"), 'T', Ingredient.item("copper_tank"), 'S', Ingredient.item("sluice")), "ExtensionFactory.java:76");
        Catalog.ITEMS.add(new ItemSpec("fludd", "F.L.U.D.D.", "fludd", "fludd", 1, new ItemKind.Armor("fludd", ArmorSlot.CHEST)));
        power("fludd", 1, List.of(" E ", "PGP", "LUL"), Map.of('E', Ingredient.item("lens"), 'P', Ingredient.item("gold_sheet"),
                'G', Ingredient.item("plastic_block"), 'L', Ingredient.item("copper_tank"), 'U', Ingredient.item("life_core")), "ExtensionFactory.java:78");
        Catalog.add(new BlockSpec("fludd_stand", "F.L.U.D.D.", "renderedMachines:4", Shape.MACHINE_MODEL, Sound.METAL, 1f, 1f, false,
                HarvestLevel.ANY, Map.of("particle", "fludd_stand", "fludd", "fludd_stand"), false));
        MachineModel.register("fludd_stand", new MachineModel(MachineModel.Facing.ALL, fluddModel()));

        power("mechanized_sponge", 1, List.of(" D ", "ATA", "SCS"), Map.of('D', Ingredient.tag("minecraft:fishes"), 'A', Ingredient.item("minecraft:water_bucket"),
                'T', Ingredient.item("aluminum_ingot"), 'S', Ingredient.item("minecraft:sponge"), 'C', Ingredient.item("base_iron")), "ExtensionFactory.java:75");
    }

    /** A full block: front on the model's front face, back opposite, side texture on the rest. */
    private static MachineModel.Box cube(String front, String back, String side, String frontFace, String backFace) {
        Map<String, MachineModel.FaceTex> faces = new java.util.LinkedHashMap<>();
        for (String f : List.of("down", "up", "north", "south", "west", "east")) {
            faces.put(f, new MachineModel.FaceTex(f.equals(frontFace) ? front : f.equals(backFace) ? back : side, null));
        }
        return new MachineModel.Box(new double[] {0, 0, 0}, new double[] {16, 16, 16}, faces);
    }

    private static void rotor(Rotor.Tier tier, String name, String legacyItem, String legacyBlock, Ingredient metal, Ingredient slab) {
        Catalog.ITEMS.add(new ItemSpec(tier.item(), name, legacyItem, tier.item(), 1, new ItemKind.Special("rotor", 0)));
        power(tier.item(), 1, List.of(" I ", "ISI", " I "), Map.of('I', metal, 'S', slab), "ExtensionFactory.java:80-82");
        Catalog.add(new BlockSpec(tier.block(), name, "renderedMachines:" + legacyBlock, Shape.MACHINE_MODEL, Sound.METAL, 2.5f, 2.5f, true,
                HarvestLevel.STONE, Map.of("particle", tier.block(), "rotor", tier.block()), false));
        // ModelRotor at 1.2 x 1/20 block a unit: hub 18x12x12 along the axis, blades 12 wide from 6 to 26 out.
        double k = 0.25, hub = 5.76, tip = 24.96;
        List<MachineModel.Box> boxes = new java.util.ArrayList<>();
        boxes.add(textured(8 - hub, 8 - hub, 0, 8 + hub, 8 + hub, 16, new double[] {12 * k, 40 * k, 24 * k, 52 * k}, new double[] {0, 40 * k, 12 * k, 52 * k}));
        double[] bladeFace = {20 * k, 0, 32 * k, 20 * k}, bladeEdge = {0, 20 * k, 20 * k, 21 * k};
        boxes.add(textured(8 - hub, 8 + hub, 7.5, 8 + hub, Math.min(32, 8 + tip), 8.5, bladeFace, bladeEdge));
        boxes.add(textured(8 - hub, Math.max(-16, 8 - tip), 7.5, 8 + hub, 8 - hub, 8.5, bladeFace, bladeEdge));
        boxes.add(textured(Math.max(-16, 8 - tip), 8 - hub, 7.5, 8 - hub, 8 + hub, 8.5, bladeFace, bladeEdge));
        boxes.add(textured(8 + hub, 8 - hub, 7.5, Math.min(32, 8 + tip), 8 + hub, 8.5, bladeFace, bladeEdge));
        MachineModel.register(tier.block(), new MachineModel(MachineModel.Facing.HORIZONTAL, boxes));
    }

    /**
     * The stand's ModelFLUDD pieces (64x64 texture, model units; the pack sits on the block, nozzle up): base, neck,
     * neck bump, neck top, head, nose and the hover mouth. Facing ALL turns the nozzle the way the stand faces.
     */
    private static List<MachineModel.Box> fluddModel() {
        int[][] parts = {
                // u, v, x, y, z, w, h, d (ModelRenderer.addBox, y down)
                {0, 16, -3, 0, -3, 6, 6, 6}, {0, 10, -1, -2, -1, 2, 2, 2}, {0, 31, -2, -5, -2, 4, 3, 4}, {0, 40, -1, -7, -1, 2, 2, 2},
                {0, 47, -2, -10, -2, 4, 3, 4}, {0, 62, -1, -9, -3, 2, 1, 1}, {46, 0, -3, -11, -4, 6, 5, 1}};
        List<MachineModel.Box> boxes = new java.util.ArrayList<>();
        double k = 0.25;
        for (int[] p : parts) {
            int u = p[0], v = p[1], w = p[5], h = p[6], d = p[7];
            double x1 = 8 + p[2], z1 = 8 + p[4], y2 = 6 - p[3], y1 = y2 - h;
            Map<String, MachineModel.FaceTex> faces = new java.util.LinkedHashMap<>();
            faces.put("up", new MachineModel.FaceTex("fludd", new double[] {(u + d) * k, v * k, (u + d + w) * k, (v + d) * k}));
            faces.put("down", new MachineModel.FaceTex("fludd", new double[] {(u + d + w) * k, v * k, (u + d + w + w) * k, (v + d) * k}));
            faces.put("north", new MachineModel.FaceTex("fludd", new double[] {(u + d) * k, (v + d) * k, (u + d + w) * k, (v + d + h) * k}));
            faces.put("south", new MachineModel.FaceTex("fludd", new double[] {(u + d + w + d) * k, (v + d) * k, (u + d + w + d + w) * k, (v + d + h) * k}));
            faces.put("west", new MachineModel.FaceTex("fludd", new double[] {u * k, (v + d) * k, (u + d) * k, (v + d + h) * k}));
            faces.put("east", new MachineModel.FaceTex("fludd", new double[] {(u + d + w) * k, (v + d) * k, (u + d + w + d) * k, (v + d + h) * k}));
            boxes.add(new MachineModel.Box(new double[] {x1, y1, z1}, new double[] {x1 + w, y2, z1 + d}, faces));
        }
        return boxes;
    }

    /** A rotor box: {@code face} uv on north and south, {@code edge} uv elsewhere (64x64 texture, in sixteenths). */
    private static MachineModel.Box textured(double x1, double y1, double z1, double x2, double y2, double z2, double[] face, double[] edge) {
        Map<String, MachineModel.FaceTex> faces = new java.util.LinkedHashMap<>();
        for (String f : List.of("down", "up", "north", "south", "west", "east")) {
            faces.put(f, new MachineModel.FaceTex("rotor", f.equals("north") || f.equals("south") ? face : edge));
        }
        return new MachineModel.Box(new double[] {x1, y1, z1}, new double[] {x2, y2, z2}, faces);
    }

    private static void power(String id, int count, List<String> pattern, Map<Character, Ingredient> key, String legacy) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, count, "maritech/extensions/modules/" + legacy));
    }

    private static void battery(String id, String name, String legacy, Ingredient metal) {
        Catalog.ITEMS.add(new ItemSpec(id, name, legacy, id, 1, new ItemKind.Special("battery", 0)));
        shaped(id, List.of(" I ", "TRT", "TRT"), Map.of('I', Ingredient.item("minecraft:iron_ingot"), 'R', Ingredient.item("minecraft:redstone"), 'T', metal));
        int full = EnergyItem.spec("mariculture:" + id).capacity();
        Catalog.RECIPES.add(new Recipe.Shapeless(id + "_charged", List.of(Ingredient.item(id), Ingredient.item("minecraft:redstone_block")), id, 1,
                "maritech/extensions/modules/ExtensionCore.java:56-64", Map.of(EnergyItem.KEY, full)));
    }

    private static void armor(String id, String name, String legacy, String material, ArmorSlot slot) {
        Catalog.ITEMS.add(new ItemSpec(id, name, legacy, id, 1, new ItemKind.Armor(material, slot)));
    }

    private static void up(String id, List<String> pattern, Map<Character, Ingredient> key) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, 1, "maritech/extensions/modules/ExtensionCore.java:51-54"));
    }

    private static void shaped(String id, List<String> pattern, Map<Character, Ingredient> key) {
        Catalog.RECIPES.add(new Recipe.Shaped(id, pattern, key, id, 1, "maritech/extensions/modules/ExtensionDiving.java:54-59"));
    }
}
