package mariculture.domain.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Every Mariculture fluid. Legacy ids (FluidHelper.addFluid field names) map to modern ids: molten
 * metals gain a "molten_" prefix, hp_water becomes pressurised_water. Milk is NeoForge/Forge's own
 * {@code minecraft:milk}; water and lava are vanilla.
 */
public final class Fluids {
    public static final String WATER = "minecraft:water";
    public static final String LAVA = "minecraft:lava";
    /** Enabled by each target with the loader's milk fluid switch (legacy registered its own "milk"). */
    public static final String MILK = "minecraft:milk";

    public static final String FISH_FOOD = "mariculture:fish_food";
    public static final String FISH_OIL = "mariculture:fish_oil";
    public static final String PRESSURISED_WATER = "mariculture:pressurised_water";
    public static final String NATURAL_GAS = "mariculture:natural_gas";
    public static final String XP = "mariculture:xp";
    public static final String BLOOD = "mariculture:blood";
    public static final String FLUX = "mariculture:flux";

    /**
     * Legacy melting points in degrees Celsius (core/RecipesSmelting.java:47-65). Rutile and the plain metal
     * fluids without a listed point use the closest legacy value: rutile melts with titanium's table entry.
     */
    public static final String[][] METALS = {
            {"aluminum", "660"}, {"magnesium", "650"}, {"titanium", "1662"}, {"copper", "1085"}, {"rutile", "1662"},
            {"iron", "1538"}, {"gold", "1064"}, {"tin", "232"}, {"lead", "328"}, {"silver", "962"}, {"nickel", "1455"},
            {"bronze", "950"}, {"steel", "1370"}, {"electrum", "1000"}, {"cobalt", "1495"}, {"ardite", "2000"},
            {"osmium", "2000"}, {"platinum", "1768"}, {"zinc", "420"},
    };

    private static final List<FluidSpec> ALL = new ArrayList<>();

    static {
        // core/Core.java:196-224
        add(new FluidSpec("natural_gas", "Natural Gas", "natural_gas", "naturalgas", 300, 0, -1000, 500, true, FluidEffect.GAS));
        add(new FluidSpec("pressurised_water", "Pressurised Water", "hp_water", "minecraft:block/water", 300, 0, 1000, 1000, false, FluidEffect.PRESSURISED));
        add(new FluidSpec("quicklime", "Liquid Quicklime", "quicklime", "quicklime", 1100, 0, 1500, 3000, false, FluidEffect.HOT));
        add(new FluidSpec("molten_salt", "Molten Salt", "salt", "moltensalt", 1074, 5, 1500, 3000, false, FluidEffect.HOT));
        add(new FluidSpec("molten_glass", "Molten Glass", "glass", "moltenglass", 1673, 10, 2500, 6000, false, FluidEffect.HOT));
        for (String[] metal : METALS) {
            int celsius = Integer.parseInt(metal[1]);
            String name = Character.toUpperCase(metal[0].charAt(0)) + metal[0].substring(1);
            add(new FluidSpec("molten_" + metal[0], "Molten " + name, metal[0], "molten" + metal[0], celsius + 273, 12, 3000, 6000, false, FluidEffect.HOT));
        }
        add(new FluidSpec("xp", "Liquid Experience", "xp", "xp", 300, 10, 800, 800, false, FluidEffect.NONE));
        // fishery/Fishery.java:205-218 (milk is the loader fluid, see MILK)
        add(new FluidSpec("fish_food", "Fish Food", "fish_food", "fishfood", 300, 0, 1200, 1500, false, FluidEffect.NONE));
        add(new FluidSpec("fish_oil", "Fish Oil", "fish_oil", "fishoil", 300, 0, 900, 1200, false, FluidEffect.FISH_OIL));
        add(new FluidSpec("custard", "Custard", "custard", "custard", 300, 0, 1500, 4000, false, FluidEffect.CUSTARD));
        add(new FluidSpec("liquid_dirt", "Liquid Dirt", "dirt", "minecraft:block/dirt!", 300, 0, 2000, 4000, false, FluidEffect.NONE));
        add(new FluidSpec("molten_gunpowder", "Molten Gunpowder", "gunpowder", "gunpowder", 400, 0, 1500, 2000, false, FluidEffect.GUNPOWDER));
        add(new FluidSpec("flux", "Liquid Flux", "flux", "flux", 300, 8, 1000, 1000, false, FluidEffect.FLUX));
        add(new FluidSpec("liquid_ender", "Liquid Ender", "ender", "ender", 300, 4, 1000, 1000, false, FluidEffect.ENDER));
        add(new FluidSpec("liquid_ice", "Liquid Ice", "ice", "ice", 250, 0, 1000, 2000, false, FluidEffect.ICE));
        add(new FluidSpec("blood", "Blood", "blood", "blood", 310, 0, 1100, 2000, false, FluidEffect.BLOOD));
        add(new FluidSpec("liquid_mana", "Liquid Mana", "mana", "mana", 300, 6, 1000, 1000, false, FluidEffect.MANA));
        add(new FluidSpec("liquid_poison", "Liquid Poison", "poison", "poison", 300, 0, 1000, 1000, false, FluidEffect.POISON));
        add(new FluidSpec("chlorophyll", "Chlorophyll", "chlorophyll", "chlorophyll", 300, 0, 1000, 1000, false, FluidEffect.CHLOROPHYLL));
        add(new FluidSpec("liquid_wind", "Liquid Wind", "wind", "wind", 300, 0, 100, 200, false, FluidEffect.WIND));
        // Thaumaturge plugin: registered on every edition so worlds keep them; buckets hide without Thaumaturge.
        add(new FluidSpec("molten_thaumium", "Molten Thaumium", "thaumium", "moltenthaumium", 1400 + 273, 12, 3000, 6000, false, FluidEffect.HOT));
        add(new FluidSpec("molten_void", "Molten Void Metal", "void", "moltenvoid", 1600 + 273, 6, 3000, 6000, false, FluidEffect.HOT));
        add(new FluidSpec("brine_of_order", "Brine of Order", "brine_of_order", "brineoforder", 300, 4, 1100, 1100, false, FluidEffect.NONE));
    }

    private Fluids() {}

    private static void add(FluidSpec spec) {
        for (FluidSpec f : ALL) if (f.id().equals(spec.id())) throw new IllegalStateException("Duplicate fluid " + spec.id());
        ALL.add(spec);
    }

    public static List<FluidSpec> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static FluidSpec byId(String id) {
        String path = id.startsWith("mariculture:") ? id.substring("mariculture:".length()) : id;
        for (FluidSpec f : ALL) if (f.id().equals(path)) return f;
        return null;
    }

    /** Full id of a legacy fluid name (FluidHelper field name), or null if it is not a Mariculture or vanilla fluid. */
    public static String fromLegacy(String legacy) {
        String l = legacy.toLowerCase(Locale.ROOT);
        if (l.equals("water")) return WATER;
        if (l.equals("lava")) return LAVA;
        if (l.equals("milk")) return MILK;
        for (FluidSpec f : ALL) if (f.legacyName().equals(l)) return "mariculture:" + f.id();
        return null;
    }

    public static String molten(String metal) {
        return "mariculture:molten_" + metal;
    }
}
