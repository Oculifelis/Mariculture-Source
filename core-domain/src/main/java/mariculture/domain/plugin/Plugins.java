package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Ingredient;
import mariculture.domain.content.Recipe;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.ShippedSpecies;
import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.metal.MetalRates;

/**
 * Support for other mods (legacy mariculture/plugins). A plugin is active when its mod is installed and its
 * switch in the "plugins" config section is on (core/config/Modules.java:28-35). Plugins only name other mods'
 * items, fluids and biomes by id, so nothing here links against them.
 *
 * <p>Plugins whose mods have no release for Forge 1.20.1 or NeoForge 1.21.1/26.1.2 (Hardcore Ender Expansion,
 * Highlands, AppleCore, MineFactory Reloaded, OpenBlocks, LiquidXP, ModdedTweaks, Carpenter's Blocks) are not
 * ported; see docs/feature_matrix.csv.
 */
public final class Plugins {
    public record Plugin(String name, String modId, Settings.Bool toggle) {}

    public static final Plugin AQUACULTURE = new Plugin("aquaculture", "aquaculture", Settings.PLUGIN_AQUACULTURE);
    public static final Plugin BIOMES_O_PLENTY = new Plugin("biomesoplenty", "biomesoplenty", Settings.PLUGIN_BIOMES_O_PLENTY);
    public static final Plugin RAILCRAFT = new Plugin("railcraft", "railcraft", Settings.PLUGIN_RAILCRAFT);
    public static final Plugin THERMAL = new Plugin("thermal", "thermal", Settings.PLUGIN_THERMAL);
    public static final Plugin TCONSTRUCT = new Plugin("tconstruct", "tconstruct", Settings.PLUGIN_TCONSTRUCT);
    /** Hephaestus, the NeoForge 1.21.1/26.1.2 successor of Tinkers' Construct: the Tinkers plugin on its smeltery. */
    public static final Plugin HEPHAESTUS = new Plugin("hephaestus", "hephaestus", Settings.PLUGIN_HEPHAESTUS);
    public static final Plugin FORESTRY = new Plugin("forestry", "forestry", Settings.PLUGIN_FORESTRY);
    /** Complicated Bees, the Forestry-style bee mod on every edition (our own port on 26.1.2): bees into bee bait. */
    public static final Plugin COMPLICATED_BEES = new Plugin("complicated_bees", "complicated_bees", Settings.PLUGIN_COMPLICATED_BEES);
    public static final Plugin BOTANIA = new Plugin("botania", "botania", Settings.PLUGIN_BOTANIA);
    public static final Plugin BLOOD_MAGIC = new Plugin("bloodmagic", "bloodmagic", Settings.PLUGIN_BLOOD_MAGIC);
    /** Neo Vitae, the NeoForge 1.21.1/26.1.2 successor of Blood Magic: the same plugin on its item ids. */
    public static final Plugin NEO_VITAE = new Plugin("neovitae", "neovitae", Settings.PLUGIN_NEO_VITAE);
    public static final Plugin REDSTONE_ARSENAL = new Plugin("redstone_arsenal", "redstone_arsenal", Settings.PLUGIN_REDSTONE_ARSENAL);
    /** Upgrade Aquatic (1.20.1, 1.21.1): its kelps and corals work as Mariculture's (compat study U1). */
    public static final Plugin UPGRADE_AQUATIC = new Plugin("upgrade_aquatic", "upgrade_aquatic", Settings.PLUGIN_UPGRADE_AQUATIC);
    /** Alex's Caves (official on 1.20.1, community ports on 1.21.1 and 26.x): pearls, Abyssal Chasm water (A1, A4). */
    public static final Plugin ALEXS_CAVES = new Plugin("alexscaves", "alexscaves", Settings.PLUGIN_ALEXS_CAVES);
    /** Starcatcher: Mariculture fish in its catch pools and guide, its rods fish Mariculture fish (SC1, SC2). */
    public static final Plugin STARCATCHER = new Plugin("starcatcher", "starcatcher", Settings.PLUGIN_STARCATCHER);
    /** Stardew Fishing: its minigame plays for Mariculture catches (S1, S2). */
    public static final Plugin STARDEW_FISHING = new Plugin("stardew_fishing", "stardew_fishing", Settings.PLUGIN_STARDEW_FISHING);
    /** Tide (1.20.1, 1.21.1): Mariculture fish in its pools and journal (T1, T2). */
    public static final Plugin TIDE = new Plugin("tide", "tide", Settings.PLUGIN_TIDE);
    public static final Plugin THAUMATURGE = new Plugin("thaumaturge", "thaumaturge", Settings.PLUGIN_THAUMATURGE);
    public static final List<Plugin> ALL = List.of(AQUACULTURE, BIOMES_O_PLENTY, RAILCRAFT, THERMAL, TCONSTRUCT, HEPHAESTUS, FORESTRY, COMPLICATED_BEES, BOTANIA, BLOOD_MAGIC,
            NEO_VITAE, REDSTONE_ARSENAL, UPGRADE_AQUATIC, ALEXS_CAVES, STARCATCHER, STARDEW_FISHING, TIDE, THAUMATURGE);

    private static Predicate<String> modLoaded = id -> false;
    private static Predicate<String> itemExists = id -> true;

    private Plugins() {}

    /** Set once by each target at mod construction (its mod list). */
    public static void setModLoaded(Predicate<String> loaded) {
        modLoaded = loaded;
    }

    /** Set by each target: whether an item id is registered (other mods rename items between versions). */
    public static void setItemExists(Predicate<String> exists) {
        itemExists = exists;
    }

    /** Whether a mod is loaded (data pack tweaks gated by "requires"). */
    public static boolean modLoaded(String id) {
        return modLoaded.test(id);
    }

    public static boolean active(Plugin p) {
        return p.toggle().get() && modLoaded.test(p.modId());
    }

    /**
     * Items that only exist for a plugin: legacy registered them only with that mod present (PluginBotania.java:46,
     * PluginBloodMagic.java:44). Here they stay registered so worlds keep them, but the creative tab and JEI hide them
     * while no plugin that crafts them is active, so a player never sees a rod with no recipe.
     */
    public static boolean hiddenWithoutPlugin(String itemId) {
        String id = itemId.startsWith("mariculture:") ? itemId.substring("mariculture:".length()) : itemId;
        return switch (id) {
            case "livingwood_fishing_rod" -> !active(BOTANIA);
            case BoundRod.ID -> !active(BLOOD_MAGIC) && !active(NEO_VITAE);
            case "molten_thaumium_bucket", "molten_void_bucket", "brine_of_order_bucket" -> !active(THAUMATURGE);
            default -> false;
        };
    }

    public static Plugin byName(String name) {
        for (Plugin p : ALL) if (p.name().equals(name)) return p;
        return null;
    }

    // ---- crucible melting -------------------------------------------------------------------------------------

    /** A crucible recipe only offered while its plugin is active. */
    public record Melt(Plugin plugin, String input, int temp, String fluid, int amount, String bonus, int chance) {}

    /** Fish food from Thaumaturge's meat chunks, brains and tallow (feeder and crucible), only with Thaumaturge. */
    private static final Map<String, Integer> THAUM_FISH_FOOD = Map.of(
            "thaumaturge:chunk_fish", 3, "thaumaturge:chunk_beef", 3, "thaumaturge:chunk_pork", 3, "thaumaturge:chunk_chicken", 2,
            "thaumaturge:chunk_mutton", 2, "thaumaturge:chunk_rabbit", 2, "thaumaturge:brain", 6, "thaumaturge:tallow", 4);

    private static final List<Melt> MELTING = new ArrayList<>();

    static {
        // PluginRailcraft.java:35-38: steel tools give their metal and a stick back, armour its metal.
        String[] tools = {"pickaxe", "shovel", "axe", "sword", "hoe"};
        for (int i = 0; i < tools.length; i++) {
            MELTING.add(new Melt(RAILCRAFT, "railcraft:steel_" + tools[i], 1370, Fluids.molten("steel"), MetalRates.TOOLS[i], "minecraft:stick", 1));
        }
        String[] armour = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < armour.length; i++) {
            MELTING.add(new Melt(RAILCRAFT, "railcraft:steel_" + armour[i], 1370, Fluids.molten("steel"), MetalRates.ARMOR[i], null, 0));
        }
        // PluginThermalFoundation.java:31-41 (Thermal's own fluids; cryotheum no longer exists)
        MELTING.add(new Melt(THERMAL, "minecraft:redstone_torch", 1600, "thermal:redstone", 100, "minecraft:stick", 1));
        MELTING.add(new Melt(THERMAL, "minecraft:redstone_block", 1600, "thermal:redstone", 900, null, 0));
        MELTING.add(new Melt(THERMAL, "minecraft:redstone", 1600, "thermal:redstone", 100, null, 0));
        MELTING.add(new Melt(THERMAL, "minecraft:glowstone_dust", 2000, "thermal:glowstone", 250, null, 0));
        MELTING.add(new Melt(THERMAL, "minecraft:glowstone", 2000, "thermal:glowstone", 1000, null, 0));
        // PluginTConstruct.java:73-82 addMetal (the Tinkers metals Mariculture has no fluid of): modern Tinkers keeps
        // manyullyn, in its own amounts (90 mB an ingot), so the smeltery casts back exactly what melted.
        MELTING.add(new Melt(TCONSTRUCT, "#c:nuggets/manyullyn", 1950, "tconstruct:molten_manyullyn", 10, null, 0));
        MELTING.add(new Melt(TCONSTRUCT, "#c:ingots/manyullyn", 1950, "tconstruct:molten_manyullyn", 90, null, 0));
        MELTING.add(new Melt(TCONSTRUCT, "#c:storage_blocks/manyullyn", 1950, "tconstruct:molten_manyullyn", 810, null, 0));
        // Thaumaturge: thaumium and void metal melt like any metal (Casting knows their nugget, ingot and block), and
        // its meat chunks, brains and tallow melt into fish food like the feeder takes them.
        String[][] magicMetals = {{"thaumium", "1400"}, {"void", "1600"}};
        for (String[] m : magicMetals) {
            int t = Integer.parseInt(m[1]);
            MELTING.add(new Melt(THAUMATURGE, "thaumaturge:nugget_" + m[0], t, Fluids.molten(m[0]), MetalRates.NUGGET, null, 0));
            MELTING.add(new Melt(THAUMATURGE, "thaumaturge:ingot_" + m[0], t, Fluids.molten(m[0]), MetalRates.INGOT, null, 0));
            MELTING.add(new Melt(THAUMATURGE, "thaumaturge:metal_" + m[0], t, Fluids.molten(m[0]), MetalRates.BLOCK, null, 0));
        }
        for (Map.Entry<String, Integer> e : THAUM_FISH_FOOD.entrySet()) {
            MELTING.add(new Melt(THAUMATURGE, e.getKey(), mariculture.domain.metal.Melting.FISH_FOOD_TEMP, Fluids.FISH_FOOD, e.getValue(), null, 0));
        }
    }

    // ---- Thaumaturge machine rows -------------------------------------------------------------------------------

    /** The fish food value of a plugin item, or 0. */
    public static int fishFood(String item) {
        Integer v = THAUM_FISH_FOOD.get(item);
        return v != null && active(THAUMATURGE) ? v : 0;
    }

    /** Plugin fish food items while their plugin is active (feeder slot hints). */
    public static Set<String> fishFoodItems() {
        return active(THAUMATURGE) ? THAUM_FISH_FOOD.keySet() : Set.of();
    }

    /** Sifter rows only offered while their plugin is active: ancient sand gives amber and quicksilver nuggets. */
    public record Sift(Plugin plugin, mariculture.domain.fishery.Bait.SifterRecipe recipe) {}

    private static final List<Sift> SIFTING = List.of(
            new Sift(THAUMATURGE, new mariculture.domain.fishery.Bait.SifterRecipe("mariculture:ancient_sand", "thaumaturge:amber", 1, 1, 8)),
            new Sift(THAUMATURGE, new mariculture.domain.fishery.Bait.SifterRecipe("mariculture:ancient_sand", "thaumaturge:nugget_quicksilver", 1, 2, 12)));

    public static List<mariculture.domain.fishery.Bait.SifterRecipe> sifting() {
        List<mariculture.domain.fishery.Bait.SifterRecipe> out = new ArrayList<>();
        for (Sift s : SIFTING) if (active(s.plugin())) out.add(s.recipe());
        return out;
    }

    /** Vat rows only offered while their plugin is active: salis mundus in water makes Brine of Order. */
    public record Vat(Plugin plugin, mariculture.domain.metal.VatRecipes.Recipe recipe) {}

    private static final List<Vat> VAT = List.of(new Vat(THAUMATURGE, new mariculture.domain.metal.VatRecipes.Recipe("brine_of_order",
            new mariculture.domain.machine.FluidAmount(Fluids.WATER, 1000), null, "thaumaturge:salis_mundus", 1,
            new mariculture.domain.machine.FluidAmount("mariculture:brine_of_order", 1000), null, 10, "thaumaturge plugin")));

    public static List<mariculture.domain.metal.VatRecipes.Recipe> vat() {
        List<mariculture.domain.metal.VatRecipes.Recipe> out = new ArrayList<>();
        for (Vat v : VAT) if (active(v.plugin())) out.add(v.recipe());
        return out;
    }

    public static List<Melt> melting() {
        return Collections.unmodifiableList(MELTING);
    }

    /**
     * PluginAquaculture.java:80: Aquaculture's raw fish melt at 180 into 250 mB fish oil per fillet with a bone at 1 in
     * 10. Modern Aquaculture keeps no fillet count by item, so each fish counts as one fillet.
     */
    public static final int FOREIGN_FISH_OIL = 250;

    /** Any mod's fish in the tag {@link ForeignFish#TAG} (compat study G1, PLUG-027); legacy took Aquaculture's only. */
    public static boolean isForeignFish(String item, Predicate<String> hasTag) {
        return ForeignFish.is(item, hasTag);
    }

    // ---- bait --------------------------------------------------------------------------------------------------

    /** Botania's sixteen flower colours, in legacy metadata order. */
    public static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    /** Bait a plugin adds: its quality (0 for none) and the rods that may use it (FishingHandler.addBait, addBaitForQuality). */
    public record PluginBait(Plugin plugin, String item, int quality, Set<Species.Rod> rods) {}

    public static final List<PluginBait> BAIT = new ArrayList<>();

    static {
        // PluginAquaculture.java:31-32: the worm (Aquaculture fish meta 19 in 1.7) is bait of quality 25 for dire and flux rods.
        BAIT.add(new PluginBait(AQUACULTURE, "aquaculture:worm", 25, Set.of(Species.Rod.DIRE, Species.Rod.FLUX)));
        // PluginBotania.java:63-65: petals (any colour) are bait of quality 70; the living rod takes petals and grasshoppers.
        for (String c : COLORS) BAIT.add(new PluginBait(BOTANIA, "botania:" + c + "_petal", 70, Set.of(Species.Rod.MANA)));
        BAIT.add(new PluginBait(BOTANIA, "mariculture:bait_grasshopper", 0, Set.of(Species.Rod.MANA)));
        // PluginBloodMagic.java:65-70: rotten flesh (35) and the weak blood shard (75) bait the bound rod. The demon
        // blood shard (100) is gone from Blood Magic 3.
        BAIT.add(new PluginBait(BLOOD_MAGIC, "minecraft:rotten_flesh", 35, Set.of(Species.Rod.BLOOD)));
        BAIT.add(new PluginBait(BLOOD_MAGIC, BoundRod.WEAK_SHARD, 75, Set.of(Species.Rod.BLOOD)));
        BAIT.add(new PluginBait(NEO_VITAE, "minecraft:rotten_flesh", 35, Set.of(Species.Rod.BLOOD)));
        BAIT.add(new PluginBait(NEO_VITAE, BoundRod.NEO_VITAE_SHARD, 75, Set.of(Species.Rod.BLOOD)));
        // PluginRedstoneArsenal.java:30-35: flux dust (RA material 0, electrum flux dust) is bait of quality 80; the
        // infused rod takes ant, maggot and bee bait and flux dust. A player's cast with it takes no bait
        // (RodTypeInfused.canUseBaitManually), so these matter in the autofisher.
        BAIT.add(new PluginBait(REDSTONE_ARSENAL, "redstone_arsenal:flux_dust", 80, Set.of(Species.Rod.INFUSED)));
        for (String b : List.of("mariculture:bait_ant", "mariculture:bait_maggot", "mariculture:bait_bee")) {
            BAIT.add(new PluginBait(REDSTONE_ARSENAL, b, 0, Set.of(Species.Rod.INFUSED)));
        }
    }

    public static Integer baitQuality(String item) {
        for (PluginBait b : BAIT) if (b.quality() > 0 && b.item().equals(item) && active(b.plugin())) return b.quality();
        return null;
    }

    public static boolean baitUsable(Species.Rod rod, String item) {
        for (PluginBait b : BAIT) if (b.item().equals(item) && b.rods().contains(rod) && active(b.plugin())) return true;
        return false;
    }

    // ---- fishing loot ------------------------------------------------------------------------------------------

    /** Loot that only its own rod catches, while the plugin is active and the item exists. */
    public record PluginLoot(Plugin plugin, Species.Rod rod, String item, double chancePercent, FishingRules.Rarity rarity) {}

    public static final List<PluginLoot> LOOT = new ArrayList<>();

    private static final List<String> COSMETICS = List.of("black_bowtie", "black_tie", "red_glasses", "puffy_scarf", "engineer_goggles",
            "eyepatch", "wicked_eyepatch", "red_ribbons", "pink_flower_bud", "polka_dotted_bows", "blue_butterfly", "cat_ears", "witch_pin",
            "devil_tail", "kamui_eye", "googly_eyes", "four_leaf_clover", "clock_eye", "unicorn_horn", "devil_horns", "hyper_plus",
            "botanist_emblem", "ancient_mask", "eerie_mask", "alien_antenna", "anaglyph_glasses", "orange_shades", "groucho_glasses",
            "thick_eyebrows", "lusitanic_shield", "tiny_potato_mask", "questgiver_mark");

    static {
        // PluginBotania.java:68-80. Botania 1.20.1 and 1.21.1 name the discs and the water ring differently; both are
        // listed and whichever exists is used. The 32 legacy cosmetics are Botania's first 32.
        for (String disc : List.of("botania:record_gaia_1", "botania:record_gaia_2", "botania:scathed_music_disc_1", "botania:scathed_music_disc_2")) {
            LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, disc, 10, FishingRules.Rarity.RARE));
        }
        LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, "botania:water_ring", 1, FishingRules.Rarity.RARE));
        LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, "botania:ring_of_chordata", 1, FishingRules.Rarity.RARE));
        LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, "botania:manasteel_shears", 2, FishingRules.Rarity.RARE));
        for (String c : COLORS) LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, "botania:" + c + "_mystical_flower", 10, FishingRules.Rarity.GOOD));
        for (String c : COSMETICS) LOOT.add(new PluginLoot(BOTANIA, Species.Rod.MANA, "botania:cosmetic_" + c, 10, FishingRules.Rarity.JUNK));

        // PluginBloodMagic.java:72-97, legacy Blood Magic 1.x items mapped to their Blood Magic 3 successors. Dropped,
        // with no successor: baseItems 0/1/3/4 (spell components), baseAlchemyItems 4/5 (concentrated catalyst,
        // fractured bone), aether, the enhanced filling agent and the key of Diablo. Bound boots became living boots.
        bloodLoot("bloodmagic:blankslate", 10, FishingRules.Rarity.JUNK);
        bloodLoot("minecraft:cobweb", 10, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:simplecatalyst", 7, FishingRules.Rarity.GOOD);
        bloodLoot("bloodmagic:weakfillingagent", 15, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:strengthenedcatalyst", 15, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:reinforcedslate", 20, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:alchemy_flask", 40, FishingRules.Rarity.GOOD);
        bloodLoot("bloodmagic:standardfillingagent", 50, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:infusedslate", 75, FishingRules.Rarity.JUNK);
        bloodLoot("bloodmagic:livingboots", 200, FishingRules.Rarity.GOOD);

        // The same table on Neo Vitae's ids: slates are tabulae (blank = rasa, reinforced = robur, imbued = animata)
        // and the bound boots' successor is the sentient boots.
        vitaeLoot("neovitae:tabula_rasa", 10, FishingRules.Rarity.JUNK);
        vitaeLoot("minecraft:cobweb", 10, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:simple_catalyst", 7, FishingRules.Rarity.GOOD);
        vitaeLoot("neovitae:weak_filling_agent", 15, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:strengthened_catalyst", 15, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:tabula_robur", 20, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:alchemy_flask", 40, FishingRules.Rarity.GOOD);
        vitaeLoot("neovitae:standard_filling_agent", 50, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:tabula_animata", 75, FishingRules.Rarity.JUNK);
        vitaeLoot("neovitae:sentient_boots", 200, FishingRules.Rarity.GOOD);
    }

    private static void bloodLoot(String item, double chance, FishingRules.Rarity rarity) {
        LOOT.add(new PluginLoot(BLOOD_MAGIC, Species.Rod.BLOOD, item, chance, rarity));
    }

    private static void vitaeLoot(String item, double chance, FishingRules.Rarity rarity) {
        LOOT.add(new PluginLoot(NEO_VITAE, Species.Rod.BLOOD, item, chance, rarity));
    }

    /** Extra anvil repair items per Mariculture item: Neo Vitae's blank tabula repairs the bound rod too. */
    public static final Map<String, List<String>> EXTRA_REPAIRS = Map.of(BoundRod.ID, List.of("neovitae:tabula_rasa"));

    public static List<PluginLoot> loot(Species.Rod rod) {
        List<PluginLoot> out = new ArrayList<>();
        for (PluginLoot l : LOOT) if (l.rod() == rod && active(l.plugin()) && itemExists.test(l.item())) out.add(l);
        return out;
    }

    // ---- Botania -----------------------------------------------------------------------------------------------

    // ---- other mods' rods ----------------------------------------------------------------------------------------

    /**
     * The Mariculture tier another mod's rod fishes as, or null. The table is data (rod_tiers/*.json, see
     * {@link mariculture.domain.fishing.RodTiers}); Redstone Arsenal's flux rod as INFUSED is PluginRedstoneArsenal.java:29.
     */
    public static Species.Rod rodTier(String item) {
        return mariculture.domain.fishing.RodTiers.of(item);
    }

    // ---- Blood Magic -------------------------------------------------------------------------------------------

    /** BloodRodType.caughtAlive (BloodRodType.java:20-28): species the bound rod always lands alive. */
    public static final Set<String> BLOOD_ALIVE = Set.of("mariculture:anglerfish", "mariculture:stickleback", "mariculture:man_o_war",
            "mariculture:tuna", "mariculture:salmon", "mariculture:lungfish");

    /** PluginBotania.java:59 (PearlGenHandler.addPearl, weight 1): oysters can grow Botania's mana pearl. */
    public static final String MANA_PEARL = "botania:mana_pearl";
    public static final int MANA_PEARL_WEIGHT = 1;

    // ---- biomes ------------------------------------------------------------------------------------------------

    // PluginBiomesOPlenty.java:63-142 biome salinity and temperature: data/mariculture/mariculture/biome_environment/
    // biomesoplenty.json, gated on this plugin (BiomeEnvironment).

    // ---- anvil crushing ----------------------------------------------------------------------------------------

    /**
     * PluginBiomesOPlenty.java:147-159 addCrushRecipe(dye x2, flower, true): two dye on the anvil only. The 1.7 flower
     * metas map to the modern flower that gives the same dye.
     */
    public static final Map<String, String> BOP_CRUSHING = Map.ofEntries(
            Map.entry("biomesoplenty:glowflower", "minecraft:cyan_dye"), Map.entry("biomesoplenty:violet", "minecraft:purple_dye"),
            Map.entry("biomesoplenty:lavender", "minecraft:purple_dye"), Map.entry("biomesoplenty:endbloom", "minecraft:light_gray_dye"),
            Map.entry("biomesoplenty:pink_daffodil", "minecraft:pink_dye"), Map.entry("biomesoplenty:pink_hibiscus", "minecraft:pink_dye"),
            Map.entry("biomesoplenty:glowshroom", "minecraft:lime_dye"), Map.entry("biomesoplenty:goldenrod", "minecraft:yellow_dye"),
            Map.entry("biomesoplenty:blue_hydrangea", "minecraft:light_blue_dye"), Map.entry("biomesoplenty:icy_iris", "minecraft:light_blue_dye"),
            Map.entry("biomesoplenty:wildflower", "minecraft:magenta_dye"), Map.entry("biomesoplenty:orange_cosmos", "minecraft:orange_dye"),
            Map.entry("biomesoplenty:burning_blossom", "minecraft:orange_dye"));

    // ---- crafting recipes --------------------------------------------------------------------------------------

    /** A crafting recipe written with a condition, so it only loads while the plugin is active. */
    public record Conditional(Plugin plugin, Recipe recipe) {}

    private static final List<Conditional> RECIPES = new ArrayList<>();

    static {
        // PluginAquaculture.java:63-67: a raw Mariculture fish and salt give fish-meal-size Aquaculture fillets.
        for (ShippedSpecies.Entry e : ShippedSpecies.ALL) {
            if (e.fishMeal() <= 0) continue;
            RECIPES.add(new Conditional(AQUACULTURE, new Recipe.Shapeless("aquaculture/fillet_from_" + e.id(),
                    List.of(Ingredient.item(e.rawItem()), Ingredient.tag("c:dusts/salt")), "aquaculture:fish_fillet_raw", e.fishMeal(),
                    "PluginAquaculture.java:65")));
        }
    }

    static {
        // PluginBotania.java:62: livingwood twigs (manaResource 3) and mana string (manaResource 16).
        RECIPES.add(new Conditional(BOTANIA, new Recipe.Shaped("livingwood_fishing_rod", List.of("  T", " TS", "T S"),
                Map.of('T', Ingredient.item("botania:livingwood_twig"), 'S', Ingredient.item("botania:mana_string")),
                "livingwood_fishing_rod", 1, "PluginBotania.java:62")));
    }

    public static List<Conditional> recipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    // ---- ProjectE (EE3's successor) -----------------------------------------------------------------------------

    /**
     * PluginEE3.load (PluginEE3.java:53-92): EMC values, item id to value, written as ProjectE custom conversions.
     * ProjectE reads the file itself, so it has no config switch. EE3 allowed fractions and ProjectE does not: values
     * below 1 become 1 and the rest round to the nearest whole; the useless droplet (0) gets none. Vanilla items legacy
     * re-priced (sponge, copper) keep ProjectE's own values; living and dried coral take the old plant value 8.
     */
    public static final Map<String, Long> EMC = new java.util.LinkedHashMap<>();

    static {
        emc(24576, "oyster");
        emc(1, "ancient_sand", "coral_rock", "limestone", "smooth_limestone");
        emc(315, "bauxite_ore", "aluminum_ingot");
        emc(6144, "rutile_ore", "rutile_ingot");
        emc(32, "magnesium_ingot");
        emc(8192, "titanium_ingot", "droplet_magic");
        for (String c : List.of("white", "green", "yellow", "orange", "blue", "red", "gold", "pink", "purple", "brown", "black", "silver")) {
            emc(24, c + "_pearl");
        }
        emc(1, "bait_ant", "bait_maggot", "bait_grasshopper", "bait_worm", "bait_bee");
        emc(48, "fish_eggs");
        emc(1, "droplet_air", "droplet_earth", "droplet_water", "droplet_aqua", "droplet_frozen", "droplet_plant");
        emc(35, "droplet_aluminum");
        emc(60, "droplet_ardite", "droplet_cobalt");
        emc(481, "droplet_attack");
        emc(15, "droplet_copper", "droplet_electric");
        emc(128, "droplet_ender");
        emc(114, "droplet_gold");
        emc(16, "droplet_health", "droplet_tin", "droplet_zinc");
        emc(25, "droplet_iron");
        emc(24, "droplet_lead");
        emc(4, "droplet_magnesium");
        emc(6, "droplet_nether");
        emc(32, "droplet_nickel");
        emc(250, "droplet_osmium", "droplet_platinum");
        emc(2, "droplet_poison");
        emc(650, "droplet_rutile");
        emc(57, "droplet_silver");
        emc(1024, "droplet_experience");
        for (String c : List.of("brown", "grey", "light_blue", "light_grey", "magenta", "orange", "pink", "purple", "red", "white", "yellow")) {
            emc(8, c + "_coral", "live_" + c + "_coral");
        }
    }

    private static void emc(long value, String... items) {
        for (String i : items) EMC.put("mariculture:" + i, value);
    }

    // ---- Tinkers' Construct ------------------------------------------------------------------------------------

    /**
     * PluginTConstruct.addMelting/addAlloy (PluginTConstruct.java:144-176): Mariculture metals melt and cast in the
     * smeltery as Mariculture fluids at Mariculture's amounts (an ingot is 144 mB here, 90 in Tinkers), so the
     * crucible and the smeltery never trade fluid at different rates. Legacy temperatures were 2000 for titanium and
     * rutile and 300 for magnesium on Tinkers 1.7's scale; modern Tinkers fuels top out at 1500 (blaze), so titanium
     * and rutile need the hottest fuel (1400) and magnesium melts at 400.
     */
    public record TinkersMetal(String metal, int temperature, boolean ore) {}

    public static final List<TinkersMetal> TINKERS_METALS = List.of(
            new TinkersMetal("titanium", 1400, false), new TinkersMetal("rutile", 1400, true), new TinkersMetal("magnesium", 400, false));

    /** Smeltery fuel: Tinkers 1.7 addSmelteryFuel(natural gas, 2000, 100) (PluginTConstruct.java:84), see TINKERS_METALS for the scale. */
    public static final int TINKERS_GAS_TEMPERATURE = 1500, TINKERS_GAS_DURATION = 100, TINKERS_GAS_AMOUNT = 50, TINKERS_GAS_RATE = 10;

    /** PluginTConstruct.java:144 limestone melts in the smeltery into 900 quicklime (legacy 250 on the 1.7 scale). */
    public static final int TINKERS_LIMESTONE_TEMPERATURE = 330;

    /**
     * The experimental import (PluginTConstruct.java:95-100) scaled Tinkers 1.7 melting temperatures by 2.085 into
     * crucible degrees. Modern Tinkers melts iron at 800 where the crucible needs 1538, so imports scale by that.
     */
    public static int tinkersToCrucible(int tinkersTemperature) {
        return (int) Math.round(tinkersTemperature * 1538 / 800.0);
    }

    /** Whether the Tinkers recipe import runs: the plugin, its mod and the import switch. */
    public static boolean tinkersImport() {
        return active(TCONSTRUCT) && Settings.PLUGIN_TCONSTRUCT_RECIPE_IMPORT.get();
    }

    /** The same import from Hephaestus' smeltery; its temperatures sit on Tinkers' scale (iron 800), see tinkersToCrucible. */
    public static boolean hephaestusImport() {
        return active(HEPHAESTUS) && Settings.PLUGIN_HEPHAESTUS_RECIPE_IMPORT.get();
    }

    /**
     * Hephaestus' own titanium fluid and scale (90 mB an ingot, 10 a nugget, 810 a block): Mariculture titanium melts
     * into it so it alloys, casts and makes tool parts with every other titanium there. Rutile and magnesium have no
     * Hephaestus fluid and stay Mariculture's, on Mariculture's scale (MetalRates), so neither machine dupes metal.
     */
    public static final String HEPHAESTUS_TITANIUM = "hephaestus:molten_titanium";
    public static final int HEPHAESTUS_INGOT = 90, HEPHAESTUS_NUGGET = 10, HEPHAESTUS_BLOCK = 810, HEPHAESTUS_TEMPERATURE = 1000;

    /** Hephaestus Tools parts and what each takes in mB (copied from hephaestus_tools' own titanium recipes, 1.0.11). */
    public static final Map<String, Integer> HEPHAESTUS_PARTS = Map.ofEntries(Map.entry("adze_head", 180), Map.entry("broad_axe_head", 720),
            Map.entry("hammer_head", 720), Map.entry("large_blade", 720), Map.entry("large_plate", 360), Map.entry("pick_head", 180),
            Map.entry("small_axe_head", 180), Map.entry("small_blade", 180), Map.entry("tool_binding", 90), Map.entry("tool_handle", 90),
            Map.entry("tough_binding", 270), Map.entry("tough_handle", 270));

    /**
     * The titanium tool material (plugins/tconstruct/TitaniumTools.java:91, addToolMaterial(200, "Titanium", 4, 650,
     * 1500, 2, 1.5F, 2, 0, "", 0xFFFFFF)) on Tinkers 3's stat scale: harvest level 4 (cobalt) is Tinkers 3's diamond
     * tier, 650 durability, mining speed 1500 is 15.0 (Tinkers 1.7 speeds were hundredths), attack 2, the handle's
     * 1.5x durability is +50 %, and reinforced II is the reinforced trait at level 2. Tinkers 3 materials are data,
     * so this needs no Tinkers code; the tint stands in for the legacy part textures.
     */
    public static final int TITANIUM_DURABILITY = 650, TITANIUM_REINFORCED = 2, TITANIUM_TIER = 3;
    public static final double TITANIUM_MINING_SPEED = 15.0, TITANIUM_ATTACK = 2.0, TITANIUM_HANDLE_DURABILITY = 0.5;
    public static final String TITANIUM_COLOUR = "FFC9CED6";

    /** PluginTConstruct.addAlloy: 8 titanium from 8 rutile and 8 magnesium. */
    public static final int TINKERS_ALLOY_AMOUNT = 8;

    // ---- Forestry ----------------------------------------------------------------------------------------------

    /** PluginForestry.init (PluginForestry.java:90-92): a Forestry bee crafts into this many bee bait. */
    public static final Map<String, Integer> FORESTRY_BEE_BAIT = new java.util.LinkedHashMap<>();

    static {
        FORESTRY_BEE_BAIT.put("drone", 1);
        FORESTRY_BEE_BAIT.put("princess", 5);
        FORESTRY_BEE_BAIT.put("queen", 7);
    }

    /**
     * The aquatic backpack (PluginForestry.java:79-90 and AquaBackpack, 124-200): a Forestry backpack in normal and
     * woven size, coloured 0x41A4A1 / 0x1A7D7A, holding what the item tag {@link #AQUA_BACKPACK_TAG} lists (legacy's
     * valid-item list). The normal one is crafted (string, wool, fish, chest); the woven one in Forestry's carpenter
     * from the normal one, woven silk and a diamond with 1000 mB water in 200 ticks.
     */
    public static final String AQUA_BACKPACK = "aqua_backpack", WOVEN_AQUA_BACKPACK = "woven_aqua_backpack";
    public static final int AQUA_PRIMARY = 4301985, AQUA_SECONDARY = 1736058;
    public static final String AQUA_BACKPACK_TAG = "mariculture:aqua_backpack";

    /** Legacy's valid items (PluginForestry.java:83-108) as Mariculture ids: oysters, pearls, fish, fish meal, nets, bait, droplets. */
    public static boolean aquaBackpackItem(String id) {
        return id.equals("oyster") || id.endsWith("_pearl") || id.startsWith("raw_") || id.equals("fish_meal") || id.equals("fishing_net")
                || id.startsWith("bait_") || id.startsWith("droplet_");
    }

    /** The vanilla and tag entries of the same list: fish, ink, lily pads and kelp (legacy coral kelp is vanilla kelp now). */
    public static final List<String> AQUA_BACKPACK_EXTRA = List.of("#minecraft:fishes", "minecraft:ink_sac", "minecraft:lily_pad", "minecraft:kelp",
            "minecraft:seagrass", "minecraft:sea_pickle");

    /** FuelManager.bronzeEngineFuel (PluginForestry.java:97): fish oil in the biogas engine, power 1 a cycle for 7500 ticks. */
    public static final int FISH_OIL_BIOGAS_POWER = 1, FISH_OIL_BIOGAS_BURN = 7500, FISH_OIL_BIOGAS_DISSIPATION = 1;

    /** FuelManager.fermenterFuel (PluginForestry.java:113): kelp ferments 150 a cycle for 1 cycle. */
    public static final String FERMENTER_KELP = "minecraft:kelp";
    public static final int KELP_FERMENT_PER_CYCLE = 150, KELP_BURN_DURATION = 1;

    /** A squeezer recipe: one raw fish gives fish oil and, by chance (0 to 1), the species' liquified product. */
    public record Squeeze(String input, int fishOil, String remnant, float chance, int time) {}

    /**
     * PluginForestry.java:102-106: every species with fish oil and a liquified product squeezes into fish oil. Legacy
     * wrote {@code (int) fish.getFishOilVolume() * 1000}, so the cast drops the fraction before scaling: 2.5 buckets
     * gives 2000 mB and anything under a bucket gives none (such a recipe is skipped). The time is the lifespan in
     * minutes and the chance was Forestry 1.7's percent, both passed as legacy did.
     */
    public static Squeeze squeeze(Species s, String rawItem) {
        if (s.fishOilMb() <= 0 || s.liquifiedProduct() == null) return null;
        int oil = (s.fishOilMb() / 1000) * 1000;
        if (oil <= 0) return null;
        return new Squeeze(rawItem, oil, s.liquifiedProduct().item(), Math.min(1f, s.liquifiedProduct().oneIn() / 100f), s.lifespanMinutes());
    }

    /**
     * PluginForestry.preInit (PluginForestry.java:62): Forestry's bioethanol counts as Mariculture's. Legacy fluids
     * were matched by name; Forestry's modern id is bio_ethanol, so fluid names match with underscores ignored.
     */
    public static String fluidName(String path) {
        return path.replace("_", "");
    }
}
