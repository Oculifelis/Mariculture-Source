package mariculture.domain.fishery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Map;

import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Stack;

/**
 * Droplets (fishery/items/ItemDroplet, core/lib/DropletMeta, 1.3.0). Each one holds a little fluid
 * (ItemDroplet.getFluidStack with the legacy "balanced" amounts from FluidHelper.addFluid) and empties into
 * a useless droplet (Fishery.addDropletRecipes). Metal droplets hold one nugget of molten metal (16 mB).
 */
public final class Droplets {
    public static final int NUGGET_MB = 16;
    public static final String USELESS = "mariculture:droplet_useless";
    /**
     * One droplet for any other mod's metal (new, no legacy item): its fluid, amount and tint colour live in its item
     * data, so a data pack fish can make droplets of any molten metal without a new item.
     */
    public static final String METAL = "mariculture:metal_droplet";

    /** {@code fluid} is null for the useless droplet. */
    public record Droplet(String id, String englishName, int legacyMeta, String fluid, int amount) {}

    private static final List<Droplet> ALL_LIST = new ArrayList<>();
    public static final List<Droplet> ALL = Collections.unmodifiableList(ALL_LIST);

    static {
        add("earth", "Earthen Droplet", 0, "mariculture:liquid_dirt", 100);
        add("plant", "Herbal Droplet", 1, "mariculture:chlorophyll", 500);
        add("frozen", "Frozen Droplet", 2, "mariculture:liquid_ice", 125);
        add("water", "Water Droplet", 3, Fluids.WATER, 1000);
        add("nether", "Nether Droplet", 4, Fluids.LAVA, 100);
        add("ender", "Ender Droplet", 5, "mariculture:liquid_ender", 25);
        add("attack", "Destruction Droplet", 6, "mariculture:molten_gunpowder", 125);
        add("aqua", "Aquatic Droplet", 7, Fluids.PRESSURISED_WATER, 250);
        add("poison", "Poison Droplet", 8, "mariculture:liquid_poison", 250);
        add("magic", "Magic Droplet", 9, "mariculture:liquid_mana", 100);
        add("electric", "Flux Droplet", 10, "mariculture:flux", 100);
        add("health", "Regeneration Droplet", 11, "mariculture:blood", 100);
        add("useless", "Useless Droplet", 12, null, 0);
        add("air", "Air Droplet", 13, "mariculture:liquid_wind", 100);
        add("experience", "Experience Droplet", 14, Fluids.XP, 100);
        String[] metals = {"iron", "gold", "copper", "aluminum", "rutile", "magnesium", "silver", "lead", "tin", "platinum", "nickel",
                "cobalt", "ardite", "osmium", "zinc"};
        String[] names = {"Iron", "Gold", "Copper", "Aluminium", "Rutile", "Magnesium", "Silver", "Lead", "Tin", "Platinum", "Nickel",
                "Cobalt", "Ardite", "Osmium", "Zinc"};
        for (int i = 0; i < metals.length; i++) add(metals[i], names[i] + " Droplet", 15 + i, Fluids.molten(metals[i]), NUGGET_MB);
    }

    private Droplets() {}

    private static void add(String type, String name, int meta, String fluid, int amount) {
        ALL_LIST.add(new Droplet("droplet_" + type, name, meta, fluid, amount));
    }

    /** A metal droplet holding {@code amount} mB of {@code fluid}, tinted {@code color} (0xRRGGBB). */
    public static Stack metal(String fluid, int amount, int color) {
        return Stack.of(METAL, 1, Map.of("fluid", fluid, "amount", amount, "color", color & 0xFFFFFF));
    }

    /** The droplet a stack is, reading a metal droplet's fluid from its data; null for other items or a blank one. */
    public static Droplet of(Stack stack) {
        if (!stack.is(METAL)) return byItem(stack.item());
        String fluid = stack.stringData("fluid", null);
        int amount = stack.intData("amount", NUGGET_MB);
        return fluid == null || amount < 1 ? null : new Droplet("metal_droplet", "Metal Droplet", -1, fluid, amount);
    }

    /** Tint for a metal droplet's sprite (0xRRGGBB), white when it has none. */
    public static int color(Stack stack) {
        return stack.intData("color", 0xFFFFFF);
    }

    public static Droplet byItem(String item) {
        for (Droplet d : ALL) if (("mariculture:" + d.id()).equals(item)) return d;
        return null;
    }
}
