package mariculture.domain.fluid;

import java.util.Map;

/**
 * Heat bottles (core/items/ItemBottle, FluidHelper.registerHeatBottle; volumes from core/Core.java:196-229 and
 * fishery/Fishery.java:205-208): one bottle holds 2000 mB of water, lava or natural gas, 1000 of pressurised water,
 * quicklime, salt or glass, one ore's worth (288) of a molten metal, 512 of fish food and 2000 of fish oil, milk or
 * custard. A bottle of fish oil heals 7 when drunk, the other drinkable ones 3 (ItemBottle.onEaten; legacy only let
 * fish oil be drunk).
 *
 * <p>Changes: one item holding any of these fluids replaces the 37 bottle metadata; the plain glass-bottle variants
 * (1000 mB fish oil, milk, custard, gas, 256 fish food) are not ported: vanilla bottles cannot hold modded fluids.
 */
public final class HeatBottle {
    public static final int CAPACITY = 2000;
    public static final int FISH_OIL_HEAL = 7;

    private static final Map<String, Integer> VOLUMES = Map.ofEntries(
            Map.entry(Fluids.WATER, 2000), Map.entry(Fluids.LAVA, 2000), Map.entry(Fluids.NATURAL_GAS, 2000),
            Map.entry(Fluids.PRESSURISED_WATER, 1000), Map.entry("mariculture:quicklime", 1000), Map.entry("mariculture:molten_salt", 1000),
            Map.entry("mariculture:molten_glass", 1000), Map.entry(Fluids.FISH_FOOD, 512), Map.entry(Fluids.FISH_OIL, 2000),
            Map.entry(Fluids.MILK, 2000), Map.entry("mariculture:custard", 2000));

    private HeatBottle() {}

    /** The volume one heat bottle holds of the fluid, or 0 when it has no heat bottle. */
    public static int volume(String fluid) {
        if (fluid == null) return 0;
        Integer v = VOLUMES.get(fluid);
        if (v != null) return v;
        return fluid.startsWith("mariculture:molten_") ? mariculture.domain.metal.MetalRates.ORE : 0;
    }

    public static boolean drinkable(String fluid) {
        return Fluids.FISH_OIL.equals(fluid);
    }
}
