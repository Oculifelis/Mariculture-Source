package mariculture.domain.metal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Stack;

/**
 * Vat recipes (api RecipeVat, core/handlers/VatHandler; registered in core/Recipes.java:72-85,
 * core/RecipesSmelting.java:155-329, fishery/Fishery.java:283-382). Time is in vat steps: one step per 30 ticks.
 *
 * <p>Changes: the quicklime + water to magnesium alloy was registered twice (5 and 9 steps); only the first is kept
 * (BUG-048). Wood inputs use the vanilla tags. Recipes for other modules' items join with those modules.
 */
public final class VatRecipes {
    /** {@code item} is an id or "#tag"; {@code itemCount} items are used. */
    public record Recipe(String id, FluidAmount fluid1, FluidAmount fluid2, String item, int itemCount, FluidAmount outFluid, Stack outItem,
                         int time, String legacy) {
        public boolean itemMatches(Stack s, Predicate<String> hasTag) {
            if (item == null) return true;
            if (s.isEmpty() || s.count() < itemCount) return false;
            return item.startsWith("#") ? hasTag.test(item.substring(1)) : s.is(item);
        }
    }

    private static final List<Recipe> ALL = new ArrayList<>();

    static {
        // World Plus coral (WorldPlus.java:100-121): coloured coral bleaches to partially bleached, then semi, then
        // bleached (50 mB quicklime, 5 steps); soaking dried coral in 100 mB water brings it to life (1 step, grey 5).
        for (mariculture.domain.worldgen.Coral.Colour c : mariculture.domain.worldgen.Coral.Colour.values()) {
            mariculture.domain.worldgen.Coral.Colour to = switch (c) {
                case GREY -> mariculture.domain.worldgen.Coral.Colour.LIGHT_GREY;
                case LIGHT_GREY -> mariculture.domain.worldgen.Coral.Colour.WHITE;
                case WHITE -> null;
                default -> mariculture.domain.worldgen.Coral.Colour.GREY;
            };
            if (to != null) {
                item("bleach_" + c.dried(), "mariculture:" + c.dried(), 1, "mariculture:quicklime", 50, Stack.of("mariculture:" + to.dried()), 5,
                        "WorldPlus.java:100");
            }
            item("soak_" + c.dried(), "mariculture:" + c.dried(), 1, Fluids.WATER, 100, Stack.of("mariculture:" + c.living()),
                    c == mariculture.domain.worldgen.Coral.Colour.GREY ? 5 : 1, "WorldPlus.java:111");
        }
        String ingot = "mariculture:molten_";
        item("golden_silk", "minecraft:string", 1, ingot + "gold", MetalRates.INGOT * 4, Stack.of("mariculture:golden_silk"), 5, "Recipes.java:72");
        item("titanium_rod", "#c:rods/wooden", 1, ingot + "titanium", MetalRates.INGOT * 3, Stack.of("mariculture:titanium_rod"), 30, "Recipes.java:83");
        item("burnt_brick_from_nether_brick", "minecraft:nether_brick", 1, Fluids.LAVA, 100, Stack.of("mariculture:burnt_brick"), 3, "Recipes.java:84");
        item("burnt_brick", "minecraft:brick", 1, Fluids.LAVA, 250, Stack.of("mariculture:burnt_brick"), 5, "Recipes.java:85");
        alloy("electrum", ingot + "silver", MetalRates.NUGGET, ingot + "gold", MetalRates.NUGGET, ingot + "electrum", MetalRates.NUGGET * 2, 1, "RecipesSmelting.java:155");
        alloy("titanium", ingot + "rutile", MetalRates.INGOT, ingot + "magnesium", MetalRates.INGOT, ingot + "titanium", MetalRates.INGOT, 1, "RecipesSmelting.java:258");
        alloy("magnesium", "mariculture:quicklime", 3000, Fluids.WATER, 1000, ingot + "magnesium", MetalRates.INGOT, 5, "RecipesSmelting.java:263");
        item("plastic", "minecraft:glass", 1, Fluids.NATURAL_GAS, 1000, Stack.of("mariculture:plastic_block"), 5, "RecipesSmelting.java:292");
        item("plastic_from_bioethanol", "minecraft:glass", 1, "bioethanol", 1500, Stack.of("mariculture:plastic_block"), 10, "RecipesSmelting.java:294");
        // MariTech (maritech/extensions/modules/ExtensionCore.java:65-68): plastic from limestone.
        // Flux rod recharge (ExtensionFishery.java:65; 1.2.4 had input and output the wrong way round, 1.3.0 fixed it).
        item("flux_rod_charge", "mariculture:flux_rod", 1, Fluids.FLUX, 1000, Stack.of("mariculture:flux_rod", 1,
                java.util.Map.of(mariculture.domain.maritech.EnergyItem.KEY, 250000)), 15, "maritech/extensions/modules/ExtensionFishery.java:65");
        item("plastic_from_limestone", "mariculture:limestone", 4, Fluids.NATURAL_GAS, 5000, Stack.of("mariculture:plastic", 4), 20, "ExtensionCore.java:65");
        item("plastic_from_limestone_and_bioethanol", "mariculture:limestone", 4, "bioethanol", 10000, Stack.of("mariculture:plastic", 3), 60, "ExtensionCore.java:67");
        // Neoprene (ExtensionCore.java:44-45 crafted ink, pearls and a bottle of natural gas): the port has no bottled
        // gas, so the vat cures ink in natural gas instead; the pearls are left out.
        item("neoprene", "minecraft:ink_sac", 4, Fluids.NATURAL_GAS, 1000, Stack.of("mariculture:neoprene", 2), 10, "ExtensionCore.java:44");
        ALL.add(new Recipe("obsidian", new FluidAmount(Fluids.WATER, 1000), new FluidAmount(Fluids.LAVA, 1000), null, 0, null,
                Stack.of("minecraft:obsidian"), 10, "RecipesSmelting.java:297"));
        ALL.add(new Recipe("heat_glass", new FluidAmount(ingot + "aluminum", MetalRates.NUGGET), new FluidAmount("mariculture:quicklime", 300),
                "minecraft:glass", 1, null, Stack.of("mariculture:heat_glass"), 3, "RecipesSmelting.java:329"));
        item("bucket_hat", "minecraft:leather_helmet", 1, Fluids.FISH_OIL, 16000, Stack.of("mariculture:bucket_hat"), 25, "Fishery.java:283");
        item("polished_log", "#minecraft:logs", 1, Fluids.FISH_OIL, 30000, Stack.of("mariculture:polished_log"), 30, "Fishery.java:289");
        item("polished_planks", "#minecraft:planks", 1, Fluids.FISH_OIL, 10000, Stack.of("mariculture:polished_planks"), 15, "Fishery.java:290");
        item("polished_stick", "#c:rods/wooden", 1, Fluids.FISH_OIL, 5000, Stack.of("mariculture:polished_stick"), 5, "Fishery.java:291");
        item("polished_titanium", "mariculture:titanium_rod", 1, Fluids.FISH_OIL, 6500, Stack.of("mariculture:polished_titanium"), 20, "Fishery.java:294");
        ALL.add(new Recipe("custard", new FluidAmount(Fluids.MILK, 1000), null, "minecraft:sugar", 2, new FluidAmount("mariculture:custard", 1000), null,
                15, "Fishery.java:314"));
        item("poisonous_potato", "minecraft:potato", 1, "mariculture:liquid_poison", 1000, Stack.of("minecraft:poisonous_potato"), 10, "Fishery.java:374");
        item("tnt", "minecraft:sand", 2, "mariculture:molten_gunpowder", 250, Stack.of("minecraft:tnt"), 10, "Fishery.java:375");
        item("packed_ice", "minecraft:ice", 1, "mariculture:liquid_ice", 1000, Stack.of("minecraft:packed_ice"), 10, "Fishery.java:376");
        item("dirt", "#minecraft:leaves", 1, "mariculture:liquid_dirt", 800, Stack.of("minecraft:dirt", 2), 10, "Fishery.java:377");
        item("clay", "#minecraft:sand", 1, Fluids.PRESSURISED_WATER, 800, Stack.of("minecraft:clay"), 10, "Fishery.java:378");
        item("mossy_cobblestone", "minecraft:cobblestone", 1, "mariculture:chlorophyll", 4000, Stack.of("minecraft:mossy_cobblestone"), 10, "Fishery.java:379");
        item("mossy_stone_bricks", "minecraft:stone_bricks", 1, "mariculture:chlorophyll", 4000, Stack.of("minecraft:mossy_stone_bricks"), 10, "Fishery.java:380");
        item("cracked_stone_bricks", "minecraft:stone_bricks", 1, Fluids.LAVA, 800, Stack.of("minecraft:cracked_stone_bricks"), 10, "Fishery.java:381");
        alloy("magnesium_from_flux", "mariculture:flux", 100, Fluids.WATER, 1000, ingot + "magnesium", 72, 10, "Fishery.java:382");
    }

    private VatRecipes() {}

    private static void item(String id, String item, int count, String fluid, int amount, Stack out, int time, String legacy) {
        if (!fluid.contains(":")) fluid = "*:" + fluid;
        ALL.add(new Recipe(id, new FluidAmount(fluid, amount), null, item, count, null, out, time, legacy));
    }

    private static void alloy(String id, String f1, int a1, String f2, int a2, String out, int amount, int time, String legacy) {
        ALL.add(new Recipe(id, new FluidAmount(f1, a1), new FluidAmount(f2, a2), null, 0, new FluidAmount(out, amount), null, time, legacy));
    }

    public static List<Recipe> all() {
        List<Recipe> base = new java.util.ArrayList<>(ALL);
        base.addAll(mariculture.domain.plugin.Plugins.vat());
        return Collections.unmodifiableList(mariculture.domain.plugin.Tweaks.vat(base));
    }

    /** Fluid identity; "*:path" matches that path in any namespace (other mods' fuels). */
    static boolean same(String recipeFluid, String fluid) {
        if (fluid == null) return false;
        if (recipeFluid.startsWith("*:")) {
            return mariculture.domain.plugin.Plugins.fluidName(fluid.substring(fluid.indexOf(':') + 1))
                    .equals(mariculture.domain.plugin.Plugins.fluidName(recipeFluid.substring(2)));
        }
        return recipeFluid.equals(fluid);
    }

    static boolean has(FluidAmount need, String fluid, int amount) {
        return need == null || (same(need.fluid(), fluid) && amount >= need.amount());
    }

    /** VatHandler.getResult with tank 1 as the first fluid. */
    public static Recipe find(String f1, int a1, String f2, int a2, Stack item, Predicate<String> hasTag) {
        if (f1 == null) return null;
        for (Recipe r : all()) {
            if (!same(r.fluid1().fluid(), f1) || a1 < r.fluid1().amount()) continue;
            if (r.fluid2() != null && !has(r.fluid2(), f2, a2)) continue;
            if (!r.itemMatches(item, hasTag)) continue;
            return r;
        }
        return null;
    }
}
