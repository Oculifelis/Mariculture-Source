package mariculture.domain.metal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Crucible fuels (core/RecipesSmelting.addFuels, api FuelInfo). A fuel heats the crucible towards {@code maxTemp}
 * (degrees out of 2000), adding at most {@code maxTempPer} degrees per (purity + 1) before it is spent, and lasts at
 * most {@code ticksPer} ticks. Gunpowder and TNT may explode (CrucibleExplosionTickHandler).
 *
 * <p>Items are ids or "#tag"; fluid fuels of other mods match by the path of their id (legacy keyed fluids by name).
 */
public final class CrucibleFuels {
    public record Fuel(int maxTemp, int maxTempPer, int ticksPer, float explosionRadius, int explosionChance) {
        public boolean explosive() {
            return explosionChance > 0;
        }
    }

    private static final Map<String, Fuel> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Fuel> FLUIDS = new LinkedHashMap<>();

    static {
        item("#c:storage_blocks/coal", 2000, 1680, 10800);
        item("minecraft:coal", 2000, 168, 1200);
        item("minecraft:charcoal", 1600, 128, 900);
        item("#minecraft:logs", 480, 48, 300);
        item("#minecraft:planks", 320, 32, 200);
        item("#c:rods/wooden", 160, 16, 100);
        // plugins/PluginRailcraft.java:47-48, by the common coke tags (Railcraft, Immersive Engineering)
        item("#c:storage_blocks/coal_coke", 2000, 2560, 32400);
        item("#c:coal_coke", 2000, 256, 3600);
        item("mariculture:raw_netherfish", 2000, 256, 2400); // fishery/Fishery.java:427
        ITEMS.put("minecraft:gunpowder", new Fuel(1600, 40, 50, 1f, 32));
        ITEMS.put("minecraft:tnt", new Fuel(2000, 250, 250, 4f, 16));
        fluid("natural_gas", 2000, 35, 1200);
        fluid("fuel", 2000, 35, 1000);
        fluid("pyrotheum", 2000, 100, 100);
        fluid("coal", 2000, 168, 300);
        fluid("biofuel", 1800, 20, 2000);
        fluid("oil", 1750, 20, 400);
        fluid("lava", 1500, 20, 360);
        fluid("biomass", 1500, 10, 1800);
        fluid("bioethanol", 1500, 10, 1800);
        fluid("hootch", 1800, 35, 250);
        fluid("fire_water", 1900, 40, 500);
        fluid("rocket_fuel", 2000, 45, 750);
    }

    private CrucibleFuels() {}

    /** Item fuels by id or "#tag", in table order. */
    public static Map<String, Fuel> items() {
        return java.util.Collections.unmodifiableMap(ITEMS);
    }

    /** Fluid fuels by fluid id path, in table order. */
    public static Map<String, Fuel> fluids() {
        return java.util.Collections.unmodifiableMap(FLUIDS);
    }

    private static void item(String key, int maxTemp, int per, int ticks) {
        ITEMS.put(key, new Fuel(maxTemp, per, ticks, 0, 0));
    }

    private static void fluid(String path, int maxTemp, int per, int ticks) {
        FLUIDS.put(path, new Fuel(maxTemp, per, ticks, 0, 0));
    }

    public static Fuel item(String item, Predicate<String> hasTag) {
        if (item == null) return null;
        return mariculture.domain.plugin.Tweaks.itemFuel(item, hasTag, () -> builtInItem(item, hasTag));
    }

    private static Fuel builtInItem(String item, Predicate<String> hasTag) {
        for (Map.Entry<String, Fuel> e : ITEMS.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("#") ? hasTag.test(k.substring(1)) : k.equals(item)) return e.getValue();
        }
        return null;
    }

    public static Fuel fluid(String fluid) {
        if (fluid == null) return null;
        return mariculture.domain.plugin.Tweaks.fluidFuel(fluid, () -> builtInFluid(fluid));
    }

    private static Fuel builtInFluid(String fluid) {
        // Underscores are ignored so Forestry's bio_ethanol is the legacy bioethanol (PluginForestry.java:62).
        String name = mariculture.domain.plugin.Plugins.fluidName(fluid.substring(fluid.indexOf(':') + 1));
        for (Map.Entry<String, Fuel> e : FLUIDS.entrySet()) {
            if (mariculture.domain.plugin.Plugins.fluidName(e.getKey()).equals(name)) return e.getValue();
        }
        return null;
    }
}
