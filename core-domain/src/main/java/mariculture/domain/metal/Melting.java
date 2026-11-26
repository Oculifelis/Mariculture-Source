package mariculture.domain.metal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.Species;
import mariculture.domain.fishery.Bait;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.plugin.Plugins;
import mariculture.domain.random.Rng;

/**
 * The crucible melting table (core/RecipesSmelting.java, core/handlers/CrucibleHandler.getResult). Inputs are item
 * ids or "#tag"; the bonus item is an id or "#tag" resolved to the tag's first item, and is given when
 * nextInt(chance) == 0.
 *
 * <p>Changes: ore dictionary names become common tags; raw ores ({@code c:raw_materials/*}) melt like ores, since
 * modern ores drop raw metal; metals of other mods match through their tags (no tag items, no recipe). Recipes are
 * searched in table order (legacy iterated a HashSet). Damaged tools melt for the full amount: legacy scaled a copy
 * of the fluid by durability and then returned the unscaled recipe.
 */
public final class Melting {
    /** One melting recipe; {@code randomFluids} (metallic dusts) replaces {@code fluid} when present. */
    public record Recipe(String input, int temp, String fluid, int amount, String bonus, int chance,
                         List<String> randomFluids, List<Integer> randomChances) {
        public boolean matches(String item, Predicate<String> hasTag) {
            return input.startsWith("#") ? hasTag.test(input.substring(1)) : input.equals(item);
        }
    }

    private static final List<Recipe> TABLE = new ArrayList<>();
    public static final int FISH_FOOD_TEMP = 150;
    public static final int FISH_OIL_TEMP = 180;

    static {
        String limestone = "mariculture:limestone", stone = "minecraft:stone";
        // Gold back / iron back (RecipesSmelting.java:271-286)
        add("minecraft:light_weighted_pressure_plate", 1064, "gold", MetalRates.INGOT * 2, null, 0);
        add("minecraft:clock", 1064, "gold", MetalRates.INGOT * 4, "minecraft:redstone", 2);
        add("minecraft:golden_horse_armor", 1064, "gold", MetalRates.INGOT * 6, "minecraft:saddle", 4);
        add("minecraft:bucket", 1538, "iron", MetalRates.INGOT * 3, null, 0);
        add("minecraft:iron_door", 1538, "iron", MetalRates.INGOT * 6, null, 0);
        add("minecraft:iron_bars", 1538, "iron", MetalRates.INGOT / 4, null, 0);
        add("minecraft:shears", 1538, "iron", MetalRates.INGOT * 2, null, 0);
        add("minecraft:anvil", 1538, "iron", MetalRates.INGOT * 31, null, 0);
        add("minecraft:chipped_anvil", 1538, "iron", MetalRates.INGOT * 22, null, 0);
        add("minecraft:damaged_anvil", 1538, "iron", MetalRates.INGOT * 13, null, 0);
        add("minecraft:heavy_weighted_pressure_plate", 1538, "iron", MetalRates.INGOT * 2, null, 0);
        add("minecraft:compass", 1538, "iron", MetalRates.INGOT * 4, "minecraft:redstone", 2);
        add("minecraft:hopper", 1538, "iron", MetalRates.INGOT * 5, "minecraft:chest", 2);
        add("minecraft:flint_and_steel", 1538, "iron", MetalRates.INGOT, null, 0);
        add("minecraft:iron_horse_armor", 1538, "iron", MetalRates.INGOT * 6, "minecraft:saddle", 4);
        // Full sets (RecipesSmelting.java:221-226, 245-250)
        fullSet("iron", 1538, "mariculture:ironic_dust", "minecraft:iron_");
        fullSet("gold", 1064, "mariculture:golden_dust", "minecraft:golden_");
        // Mariculture metals (:252-256); rutile uses titanium's point
        metal("aluminum", 660, "mariculture:aluminum_dust", 10);
        metal("rutile", 1662, limestone, 2);
        metal("titanium", 1662, limestone, 2);
        metal("magnesium", 650, stone, 2);
        // Other mods' metals, present only through their tags (:142-153)
        metal("tin", 232, "mariculture:tinnic_dust", 10);
        metal("copper", 1085, "mariculture:copperous_dust", 10);
        metal("silver", 962, "mariculture:silvery_dust", 10);
        metal("lead", 328, "mariculture:leader_dust", 10);
        metal("nickel", 1455, "#c:dusts/platinum", 10);
        metal("bronze", 950, null, 0);
        metal("steel", 1370, null, 0);
        metal("electrum", 1000, null, 0);
        metal("ardite", 2000, "minecraft:netherrack", 2);
        metal("cobalt", 1495, "#c:dusts/sulfur", 3);
        metal("platinum", 1768, "#c:dusts/sulfur", 5);
        metal("osmium", 2000, null, 0);
        metal("zinc", 420, "#c:dusts/sulfur", 5);
        // Metallic dusts (:121-146): one of the linked metals, each at 1 in n, only metals with ingots
        dust("aluminum_dust", 660, null, 0, "tin", 7, "iron", 5, "rutile", 35);
        dust("copperous_dust", 1085, "#c:dusts/sulfur", 10, "iron", 4, "silver", 7, "osmium", 30, "gold", 10, "cobalt", 15, "nickel", 8, "lead", 7, "tin", 6);
        dust("golden_dust", 1064, null, 0, "electrum", 3, "silver", 7, "gold", 25);
        dust("ironic_dust", 1538, "#c:silicon", 6, "aluminum", 3, "tin", 8, "copper", 6);
        dust("leader_dust", 328, null, 0, "silver", 3, "iron", 6, "copper", 8, "tin", 10);
        dust("silvery_dust", 962, "#c:dusts/sulfur", 5, "lead", 2, "electrum", 4);
        dust("tinnic_dust", 232, "#c:dusts/sulfur", 7, "copper", 3, "iron", 6, "lead", 8);
        // Glass, ice, snow, limestone, salt (:288-327)
        plain("minecraft:sand", 1000, "mariculture:molten_glass", 1000);
        plain("minecraft:glass", 900, "mariculture:molten_glass", 1000);
        plain("minecraft:glass_pane", 500, "mariculture:molten_glass", 375);
        plain("minecraft:ice", 1, Fluids.WATER, 1000);
        plain("minecraft:snow_block", 1, Fluids.WATER, 1000);
        plain("mariculture:smooth_limestone", 825, "mariculture:quicklime", 1000);
        plain("#mariculture:limestone", 825, "mariculture:quicklime", 900);
        plain("mariculture:chalk", 825, "mariculture:quicklime", 2500); // factory/Factory.java:128
        plain("mariculture:salt", 801, "mariculture:molten_salt", 20);
        // Fish food (fishery/FishFoodHandler.addFishFood)
        for (Map.Entry<String, Integer> e : Bait.FISH_FOOD.entrySet()) plain(e.getKey(), FISH_FOOD_TEMP, Fluids.FISH_FOOD, e.getValue());
    }

    private Melting() {}

    private static void add(String input, int temp, String metal, int amount, String bonus, int chance) {
        TABLE.add(new Recipe(input, temp, Fluids.molten(metal), amount, bonus, chance, List.of(), List.of()));
    }

    private static void plain(String input, int temp, String fluid, int amount) {
        TABLE.add(new Recipe(input, temp, fluid, amount, null, 0, List.of(), List.of()));
    }

    /** RecipesSmelting.addMetal: ore (and raw ore) with the bonus, then nugget, ingot, block, dust. */
    private static void metal(String metal, int temp, String bonus, int chance) {
        add("#c:ores/" + metal, temp, metal, MetalRates.ORE, bonus, chance);
        add("#c:raw_materials/" + metal, temp, metal, MetalRates.ORE, bonus, chance);
        add("#c:nuggets/" + metal, temp, metal, MetalRates.NUGGET, null, 0);
        add("#c:ingots/" + metal, temp, metal, MetalRates.INGOT, null, 0);
        add("#c:storage_blocks/" + metal, temp, metal, MetalRates.BLOCK, null, 0);
        add("#c:dusts/" + metal, temp, metal, MetalRates.INGOT, null, 0);
    }

    /** RecipesSmelting.addFullSet: the metal plus its vanilla tools (which give back a stick) and armour. */
    private static void fullSet(String metal, int temp, String dust, String prefix) {
        metal(metal, temp, dust, 10);
        String[] tools = {"pickaxe", "shovel", "axe", "sword", "hoe"};
        for (int i = 0; i < tools.length; i++) add(prefix + tools[i], temp, metal, MetalRates.TOOLS[i], "minecraft:stick", 1);
        String[] armour = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < armour.length; i++) add(prefix + armour[i], temp, metal, MetalRates.ARMOR[i], null, 0);
    }

    private static void dust(String dust, int temp, String bonus, int chance, Object... metalsAndChances) {
        List<String> fluids = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (int i = 0; i < metalsAndChances.length; i += 2) {
            fluids.add((String) metalsAndChances[i]);
            chances.add((Integer) metalsAndChances[i + 1]);
        }
        TABLE.add(new Recipe("mariculture:" + dust, temp, null, MetalRates.INGOT, bonus, chance, List.copyOf(fluids), List.copyOf(chances)));
    }

    /** The table, then the recipes of active plugins (mariculture/plugins). */
    public static List<Recipe> table() {
        List<Recipe> out = new ArrayList<>(TABLE);
        for (Plugins.Melt m : Plugins.melting()) {
            if (Plugins.active(m.plugin())) out.add(new Recipe(m.input(), m.temp(), m.fluid(), m.amount(), m.bonus(), m.chance(), List.of(), List.of()));
        }
        return Collections.unmodifiableList(mariculture.domain.plugin.Tweaks.melting(out));
    }

    /** The fish oil recipe for a raw fish item, or null (recipe viewers). */
    public static Recipe fishOilRecipe(String item, FishRegistry registry) {
        return fishOil(item, registry);
    }

    /** Raw fish melt into fish oil with the species' by-product (Fish.java:429, RecipeHelper.addFishMelting). */
    static Recipe fishOil(String item, FishRegistry registry) {
        if (registry == null) return null;
        for (Species sp : registry.allSpecies()) {
            if (sp.fishOilMb() <= 0 || !FishingRules.rawForm(sp.id()).equals(item)) continue;
            Species.Liquified l = sp.liquifiedProduct();
            return new Recipe(item, FISH_OIL_TEMP, Fluids.FISH_OIL, sp.fishOilMb(), l == null ? null : l.item(), l == null ? 0 : l.oneIn(),
                    List.of(), List.of());
        }
        return null;
    }

    /**
     * CrucibleHandler.getResult: the first recipe for the item whose melting point is reached ({@code temp} in
     * degrees, temp / 25000 * 2000 in the crucible).
     */
    public static Recipe find(String item, int temp, Predicate<String> hasTag, FishRegistry fish) {
        if (item == null || mariculture.domain.plugin.Tweaks.meltRemoved(item)) return null;
        for (Recipe r : table()) if (temp >= r.temp() && r.matches(item, hasTag)) return r;
        Recipe f = fishOil(item, fish);
        if (f == null && Plugins.isForeignFish(item, hasTag)) {
            f = new Recipe(item, FISH_OIL_TEMP, Fluids.FISH_OIL, Plugins.FOREIGN_FISH_OIL, "minecraft:bone", 10, List.of(), List.of());
        }
        return f != null && temp >= f.temp() ? f : null;
    }

    /** Any recipe for the item, ignoring temperature (automation slot checks). */
    public static boolean canMelt(String item, Predicate<String> hasTag, FishRegistry fish) {
        return find(item, Integer.MAX_VALUE, hasTag, fish) != null;
    }

    /**
     * The fluid a melt gives. Dust recipes roll each linked metal in order at 1 in n and fall back to the first;
     * metals whose ingot tag is empty are left out (RecipesSmelting.addDust). Null when none is left.
     */
    public static FluidAmount fluid(Recipe r, Rng rng, Predicate<String> tagHasItems) {
        if (r.randomFluids().isEmpty()) return new FluidAmount(r.fluid(), r.amount());
        List<String> fluids = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (int i = 0; i < r.randomFluids().size(); i++) {
            String metal = r.randomFluids().get(i);
            if (!tagHasItems.test("c:ingots/" + metal)) continue;
            fluids.add(metal);
            chances.add(r.randomChances().get(i));
        }
        if (fluids.isEmpty()) return null;
        for (int i = 0; i < fluids.size(); i++) {
            if (rng.nextInt(chances.get(i)) == 0) return new FluidAmount(Fluids.molten(fluids.get(i)), r.amount());
        }
        return new FluidAmount(Fluids.molten(fluids.get(0)), r.amount());
    }
}
