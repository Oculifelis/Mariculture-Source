package mariculture.domain.content;

import static mariculture.domain.content.HarvestLevel.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingHat;
import mariculture.domain.pearl.PearlColor;

/**
 * The canonical list of simple Mariculture content shared by all three targets. Every number
 * cites its legacy source (1.7.10-1.3.0 unless stated). Machines, fish and other stateful
 * content get their own definitions as their slices land.
 */
public final class Catalog {
    public static final String BOOK_ID = "mariculture:compendium";

    static final List<BlockSpec> BLOCKS = new ArrayList<>();
    static final List<ItemSpec> ITEMS = new ArrayList<>();
    static final List<Recipe> RECIPES = new ArrayList<>();
    /** Tag id -> entries (item/block ids or #tags). Block and item tags are kept separately. */
    private static final Map<String, List<String>> BLOCK_TAGS = new LinkedHashMap<>();
    private static final Map<String, List<String>> ITEM_TAGS = new LinkedHashMap<>();
    private static final List<OreFeature> ORES = new ArrayList<>();
    private static final List<ArmorMaterialSpec> ARMOR_MATERIALS = new ArrayList<>();
    /** Legacy content intentionally replaced by vanilla: legacy ref -> vanilla id. */
    private static final Map<String, String> VANILLA_ALIASES = new LinkedHashMap<>();

    static {
        limestone();
        rocks();
        metalBlocks();
        pearls();
        oyster();
        fishingGear();
        fluidBuckets();
        CoreMaterials.register();
        FisheryContent.register();
        FoodContent.register();
        MetalContent.register();
        MagicContent.register();
        WorldContent.register();
        DivingContent.register();
        FactoryContent.register();
        MariTechContent.register();
        MariTechContent.registerPower();
        MariTechContent.registerMachines();
        book();
    }

    private Catalog() {}

    // core/blocks/BlockLimestone.java: pickaxe level 0, hardness per meta, setResistance(1F) in Core.java:182.
    private static void limestone() {
        float res = BlockSpec.fromLegacySetResistance(1f);
        cube("limestone", "Limestone", "limestone:0", 0.75f, res, ANY, "limestone_raw");
        cube("smooth_limestone", "Smooth Limestone", "limestone:1", 1f, res, ANY, "limestone_smooth");
        cube("limestone_bricks", "Limestone Brick", "limestone:2", 1.5f, res, ANY, "limestone_brick");
        cube("small_limestone_bricks", "Limestone Small Brick", "limestone:3", 1.75f, res, ANY, "limestone_brick_small");
        cube("thin_limestone_bricks", "Limestone Thin Brick", "limestone:4", 2f, res, ANY, "limestone_brick_thin");
        cube("bordered_limestone", "Bordered Limestone", "limestone:5", 1.5f, res, ANY, "limestone_bordered");
        // Chiseled shows the bordered texture on top and bottom (BlockLimestone#getIcon).
        add(new BlockSpec("chiseled_limestone", "Chiseled Limestone", "limestone:6", Shape.CUBE, Sound.STONE, 1.5f, res, true, ANY,
                Map.of("end", "limestone_bordered", "side", "limestone_chiseled"), true));
        // Metas 7-9 are one pillar in three orientations: end = pillar2, side = pillar3 when vertical.
        add(new BlockSpec("limestone_pillar", "Limestone Pillar", "limestone:7-9", Shape.PILLAR, Sound.STONE, 1.5f, res, true, ANY,
                Map.of("end", "limestone_pillar_2", "side", "limestone_pillar_3"), true));
        // Metas 10-15 are one pedestal in six orientations: bordered face points at "facing".
        add(new BlockSpec("limestone_pedestal", "Limestone Pedestal", "limestone:10-15", Shape.PEDESTAL, Sound.STONE, 1.5f, res, true, ANY,
                Map.of("top", "limestone_bordered", "bottom", "limestone_pillar_2", "side", "limestone_pedestal_2"), true));

        for (String id : List.of("limestone", "smooth_limestone", "limestone_bricks", "small_limestone_bricks", "thin_limestone_bricks",
                "bordered_limestone", "chiseled_limestone", "limestone_pillar", "limestone_pedestal")) {
            itemTag("mariculture:limestone", id);
            blockTag("mariculture:limestone", id);
        }

        // aesthetics/Aesthetics.java:43-50
        twoByTwo("limestone_bricks", "limestone", 4, "Aesthetics.java:43");
        twoByTwo("bordered_limestone", "smooth_limestone", 4, "Aesthetics.java:44");
        twoByTwo("small_limestone_bricks", "limestone_bricks", 4, "Aesthetics.java:45");
        twoByTwo("chiseled_limestone", "bordered_limestone", 4, "Aesthetics.java:46");
        RECIPES.add(new Recipe.Smelting("smooth_limestone_from_smelting", Ingredient.item("limestone"), "smooth_limestone", 0.1f, 200, "Aesthetics.java:47"));
        RECIPES.add(new Recipe.Shaped("thin_limestone_bricks", List.of("XY", "YX"),
                Map.of('X', Ingredient.item("limestone_bricks"), 'Y', Ingredient.item("small_limestone_bricks")), "thin_limestone_bricks", 4, "Aesthetics.java:48"));
        RECIPES.add(new Recipe.Shaped("limestone_pillar", List.of("X", "X"), Map.of('X', Ingredient.item("smooth_limestone")), "limestone_pillar", 2, "Aesthetics.java:49"));
        RECIPES.add(new Recipe.Shaped("limestone_pedestal", List.of("X", "Y"),
                Map.of('X', Ingredient.item("limestone_pillar"), 'Y', Ingredient.item("bordered_limestone")), "limestone_pedestal", 2, "Aesthetics.java:50"));
    }

    // core/blocks/BlockRock.java: explosion resistance overrides are used unchanged.
    // Copper ore (rocks:1) is not registered: vanilla copper ore fills that role (docs/source_delta.md, "Vanilla collisions").
    private static void rocks() {
        VANILLA_ALIASES.put("rocks:1", "minecraft:copper_ore");
        cube("coral_rock", "Coral Rock", "rocks:0", 5f, 15f, STONE, "coral_rock");
        cube("bauxite_ore", "Bauxite Ore", "rocks:2", 3f, 3f, STONE, "bauxite_ore");
        cube("rutile_ore", "Rutile Ore", "rocks:3", 8f, 1f, IRON, "rutile_ore");
        blockTag("c:ores/aluminum", "bauxite_ore");
        itemTag("c:ores/aluminum", "bauxite_ore");
        blockTag("c:ores/bauxite", "bauxite_ore");
        itemTag("c:ores/bauxite", "bauxite_ore");
        blockTag("c:ores/rutile", "rutile_ore");
        itemTag("c:ores/rutile", "rutile_ore");
        // core/handlers/WorldGenHandler.java:32-43 with defaults from core/config/WorldGeneration.java:16-20.
        // Legacy ran in every dimension except -1 and 1; modern equivalent is the overworld biome tag.
        // Copper veins (WorldGenHandler.java:45-56) are dropped: vanilla generates copper ore.
        ORES.add(new OreFeature("ore_bauxite", "bauxite_ore", 8, 16, 64, 256,
                "minecraft:stone_ore_replaceables", "minecraft:is_overworld", "WorldGenHandler.java:32-43"));
        // Natural gas pockets (maritech/handlers/MTWorldGen.java:35-46, config core/config/WorldGeneration.java:28-32):
        // one pocket per 20 chunks in saline chunks, vein 48, Y 16 + nextInt(26 - 16), replacing stone with the gas.
        ORES.add(new OreFeature("natural_gas_pocket", "natural_gas", 48, 1, 16, 26,
                "minecraft:stone_ore_replaceables", "mariculture:is_saline", "MTWorldGen.java:35-46", 20));
        blockTag("c:ores", "#c:ores/aluminum");
        blockTag("c:ores", "#c:ores/rutile");
        itemTag("c:ores", "#c:ores/aluminum");
        itemTag("c:ores", "#c:ores/rutile");
    }

    // core/blocks/BlockMetal.java. Copper block (metals:0) is vanilla's copper block.
    private static void metalBlocks() {
        VANILLA_ALIASES.put("metals:0", "minecraft:copper_block");
        metal("aluminum_block", "Block of Aluminium", "metals:1", 4f, 12f, STONE, "aluminum_block", "aluminum");
        metal("rutile_block", "Block of Impure Titanium", "metals:2", 10f, 15f, IRON, "rutile_block", "rutile");
        metal("magnesium_block", "Block of Magnesium", "metals:3", 1f, 2f, ANY, "magnesium_block", "magnesium");
        metal("titanium_block", "Block of Titanium", "metals:4", 15f, 45f, DIAMOND, "titanium_block", "titanium");
    }

    private static void metal(String id, String name, String legacy, float hardness, float resistance, HarvestLevel level, String texture, String material) {
        add(new BlockSpec(id, name, legacy, Shape.CUBE, Sound.METAL, hardness, resistance, true, level, Map.of("all", texture), true));
        blockTag("c:storage_blocks/" + material, id);
        itemTag("c:storage_blocks/" + material, id);
        blockTag("c:storage_blocks", "#c:storage_blocks/" + material);
        itemTag("c:storage_blocks", "#c:storage_blocks/" + material);
    }

    // core/items/ItemPearl.java, core/blocks/BlockPearlBlock.java (setHardness 2, setResistance 1.5),
    // aesthetics pearl bricks (setResistance 2). Recipes: Recipes.java:62-63, Aesthetics.java:40.
    private static void pearls() {
        for (PearlColor color : PearlColor.values()) {
            String c = color.id();
            String title = Character.toUpperCase(c.charAt(0)) + c.substring(1);
            ITEMS.add(new ItemSpec(c + "_pearl", title + " Pearl", "pearls:" + color.ordinal(), "pearl_" + c, 64));
            itemTag("c:gems/pearl", c + "_pearl");
            itemTag("mariculture:pearls", c + "_pearl");
            add(new BlockSpec(c + "_pearl_block", title + " Pearl Block", "pearlBlock:" + color.ordinal(), Shape.CUBE, Sound.STONE,
                    2f, BlockSpec.fromLegacySetResistance(1.5f), true, ANY, Map.of("all", "pearl_block_" + c), true));
            add(new BlockSpec(c + "_pearl_bricks", title + " Pearl Brick", "pearlBrick:" + color.ordinal(), Shape.CUBE, Sound.STONE,
                    2f, BlockSpec.fromLegacySetResistance(2f), true, ANY, Map.of("all", "pearl_brick_" + c), true));
            twoByTwo(c + "_pearl_block", c + "_pearl", 1, "Recipes.java:62");
            RECIPES.add(new Recipe.Shapeless(c + "_pearl_from_block", List.of(Ingredient.item(c + "_pearl_block")), c + "_pearl", 4, "Recipes.java:63"));
            twoByTwo(c + "_pearl_bricks", c + "_pearl_block", 4, "Aesthetics.java:40");
        }
    }

    // Legacy guides were Enchiridion volumes handed out on first action (plugins/enchiridion/EventHandler.java).
    // Modern adaptation: one compendium, craftable from a book and any fish (docs/book_migration.csv).
    private static void book() {
        RECIPES.add(new Recipe.BookShapeless("compendium", List.of(Ingredient.item("minecraft:book"), Ingredient.tag("minecraft:fishes")),
                BOOK_ID, "modern adaptation of plugins/enchiridion/EventHandler.java"));
    }

    // core/blocks/BlockWater.java (meta 0, "Pearl Oyster"): Core.java:250 setHardness(10F) and snow step sound, but
    // getBlockHardness is overridden to 25; explosion resistance stays at the setHardness-derived 10. Stone pickaxe
    // (getToolLevel 1). Rules: mariculture.domain.pearl.Oyster.
    private static void oyster() {
        add(new BlockSpec("oyster", "Pearl Oyster", "water:0", Shape.OYSTER, Sound.SNOW, 25f, 10f, true, STONE,
                Map.of("shell", "oyster", "lid", "oyster_lid"), true));
    }

    // Rods: fishery/Fishery.java:191-193 (durability, enchantability), RodType registrations 263-267, repair
    // materials 277-279, recipes 280-282 (RecipeHelper.addFishingRodRecipe). The wooden and titanium rod recipes
    // need polished sticks / polished titanium and arrive with those materials.
    // Bucket hat: Fishery.java:169 (armour material FISHING, durability factor 8, no protection, enchantability 0)
    // and 200; dyeing with eight pearls 284-287. The plain hat comes from the vat (Fishery.java:283).
    private static void fishingGear() {
        ITEMS.add(new ItemSpec("reed_fishing_rod", "Reed Fishing Rod", "rod.reed", "reed_fishing_rod", 1,
                new ItemKind.Rod(Species.Rod.OLD, 96, 24, "minecraft:sugar_cane")));
        ITEMS.add(new ItemSpec("wooden_fishing_rod", "Wooden Fishing Rod", "rod.wood", "wooden_fishing_rod", 1,
                new ItemKind.Rod(Species.Rod.GOOD, 320, 10, "polished_stick")));
        ITEMS.add(new ItemSpec("titanium_fishing_rod", "Titanium Fishing Rod", "rod.titanium", "titanium_fishing_rod", 1,
                new ItemKind.Rod(Species.Rod.SUPER, 640, 16, "polished_titanium")));
        // Botania's living rod (PluginBotania.java:46, ItemLivingRod: ItemRod's 127 uses and enchantability 1, repaired
        // with a manasteel ingot); it only has a recipe while Botania is installed (Plugins.recipes()).
        ITEMS.add(new ItemSpec("livingwood_fishing_rod", "Livingwood Fishing Rod", "rodLiving", "livingwood_fishing_rod", 1,
                new ItemKind.Rod(Species.Rod.MANA, 127, 1, "botania:manasteel_ingot")));
        // Blood Magic's bound rod (PluginBloodMagic.java:44, ItemBoundRod: ItemRod's 127 uses; legacy drained soul network
        // LP instead of wearing, which needs Blood Magic's API, so here it wears and is repaired with a blank slate). It is
        // bound from a titanium rod in Blood Magic's binding array (Forge 1.20.1 data, ResourceExporter.bloodMagic).
        ITEMS.add(new ItemSpec("bound_fishing_rod", "Bound Fishing Rod", "rodBlood", "bound_fishing_rod", 1,
                new ItemKind.Rod(Species.Rod.BLOOD, 127, 1, "bloodmagic:blankslate")));
        for (String rod : List.of("reed_fishing_rod", "wooden_fishing_rod", "titanium_fishing_rod", "livingwood_fishing_rod", "bound_fishing_rod")) {
            itemTag("mariculture:fishing_rods", rod);
        }
        // Since 1.21 the enchanting table and anvil read item tags: without these the rods took no Lure, Luck of the
        // Sea, Unbreaking or Mending there. c:tools/fishing_rod is the common tag other mods look rods up by.
        itemTag("minecraft:enchantable/fishing", "#mariculture:fishing_rods");
        itemTag("minecraft:enchantable/durability", "#mariculture:fishing_rods");
        itemTag("c:tools/fishing_rod", "#mariculture:fishing_rods");
        itemTag("c:tools", "#c:tools/fishing_rod");
        RECIPES.add(new Recipe.Shaped("reed_fishing_rod", List.of("  S", " SW", "S W"),
                Map.of('S', Ingredient.item("minecraft:sugar_cane"), 'W', Ingredient.item("minecraft:string")), "reed_fishing_rod", 1, "Fishery.java:280"));
        RECIPES.add(new Recipe.Shaped("wooden_fishing_rod", List.of("  S", " SW", "S W"),
                Map.of('S', Ingredient.item("polished_stick"), 'W', Ingredient.item("minecraft:string")), "wooden_fishing_rod", 1, "Fishery.java:281"));
        RECIPES.add(new Recipe.Shaped("titanium_fishing_rod", List.of("  S", " SW", "S W"),
                Map.of('S', Ingredient.item("polished_titanium"), 'W', Ingredient.item("minecraft:string")), "titanium_fishing_rod", 1, "Fishery.java:282"));

        List<PearlColor> hats = new ArrayList<>();
        hats.add(null);
        hats.addAll(List.of(PearlColor.values()));
        for (PearlColor color : hats) {
            String id = FishingHat.itemId(color);
            ARMOR_MATERIALS.add(new ArmorMaterialSpec(id, 8, 0, 0, 0, 0, 0, null, id, false));
            String name = color == null ? "Bucket Hat" : title(color.id()) + " Pearl Bucket Hat";
            ITEMS.add(new ItemSpec(id, name, color == null ? "fishinghat" : "fishinghat{Color:" + color.ordinal() + "}", id, 1,
                    new ItemKind.Armor(id, ArmorSlot.HEAD)));
            itemTag("mariculture:bucket_hats", id);
            if (color == null) continue;
            itemTag("mariculture:dyed_bucket_hats", id);
            RECIPES.add(new Recipe.Shaped("bucket_hat_" + color.id(), List.of("DDD", "DHD", "DDD"),
                    Map.of('D', Ingredient.item(color.id() + "_pearl"), 'H', Ingredient.tag("mariculture:bucket_hats")), id, 1, "Fishery.java:286"));
        }
        RECIPES.add(new Recipe.Shapeless("bucket_hat_undye", List.of(Ingredient.tag("mariculture:dyed_bucket_hats")),
                FishingHat.PLAIN_ID, 1, "Fishery.java:284"));
    }

    // core/helpers/FluidHelper.addFluid registered a bucket (Core.buckets meta) for each liquid; see mariculture.domain.fluid.Fluids.
    private static void fluidBuckets() {
        for (mariculture.domain.fluid.FluidSpec f : mariculture.domain.fluid.Fluids.all()) {
            if (!f.hasBucket()) continue;
            ITEMS.add(new ItemSpec(f.bucketId(), f.englishName() + " Bucket", "buckets:" + f.legacyName(), "bucket/" + f.id(), 1,
                    new ItemKind.FluidBucket("mariculture:" + f.id())));
            itemTag("mariculture:fluid_buckets", f.bucketId());
        }
        itemTag("c:buckets", "#mariculture:fluid_buckets");
    }

    static String title(String id) {
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    static void cube(String id, String name, String legacy, float hardness, float resistance, HarvestLevel level, String texture) {
        add(new BlockSpec(id, name, legacy, Shape.CUBE, Sound.STONE, hardness, resistance, true, level, Map.of("all", texture), true));
    }

    static void twoByTwo(String result, String input, int count, String legacy) {
        RECIPES.add(new Recipe.Shaped(result, List.of("##", "##"), Map.of('#', Ingredient.item(input)), result, count, legacy));
    }

    static void add(BlockSpec spec) {
        for (BlockSpec existing : BLOCKS) {
            if (existing.id().equals(spec.id())) throw new IllegalStateException("Duplicate block " + spec.id());
        }
        BLOCKS.add(spec);
    }

    static void blockTag(String tag, String entry) {
        BLOCK_TAGS.computeIfAbsent(tag, t -> new ArrayList<>()).add(entry.startsWith("#") ? entry : Ids.full(entry));
    }

    static void itemTag(String tag, String entry) {
        ITEM_TAGS.computeIfAbsent(tag, t -> new ArrayList<>()).add(entry.startsWith("#") ? entry : Ids.full(entry));
    }

    public static List<OreFeature> ores() {
        return Collections.unmodifiableList(ORES);
    }

    static void armorMaterial(ArmorMaterialSpec m) {
        ARMOR_MATERIALS.add(m);
    }

    public static List<ArmorMaterialSpec> armorMaterials() {
        return Collections.unmodifiableList(ARMOR_MATERIALS);
    }

    public static ArmorMaterialSpec armorMaterial(String id) {
        for (ArmorMaterialSpec m : ARMOR_MATERIALS) if (m.id().equals(id)) return m;
        throw new IllegalArgumentException("Unknown armour material " + id);
    }

    public static Map<String, String> vanillaAliases() {
        return Collections.unmodifiableMap(VANILLA_ALIASES);
    }

    public static List<BlockSpec> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    public static List<ItemSpec> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static List<Recipe> recipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Map<String, List<String>> blockTags() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        BLOCK_TAGS.forEach((k, v) -> out.put(k, List.copyOf(new java.util.LinkedHashSet<>(v))));
        return Collections.unmodifiableMap(out);
    }

    public static Map<String, List<String>> itemTags() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        ITEM_TAGS.forEach((k, v) -> out.put(k, List.copyOf(new java.util.LinkedHashSet<>(v))));
        return Collections.unmodifiableMap(out);
    }
}
