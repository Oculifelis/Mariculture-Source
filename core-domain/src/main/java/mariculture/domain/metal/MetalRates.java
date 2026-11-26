package mariculture.domain.metal;

/**
 * Molten metal volumes (core/lib/MetalRates, config GeneralStuff "Molten Metal Nugget mB Value" 16 and Machines
 * "Crucible Furnace > mB per Ore" = nugget * 9 * 2).
 */
public final class MetalRates {
    public static final int NUGGET = 16;
    public static final int INGOT = NUGGET * 9;
    public static final int BLOCK = INGOT * 9;
    public static final int ORE = NUGGET * 9 * 2;
    /** Pick, shovel, axe, sword, hoe. */
    public static final int[] TOOLS = {INGOT * 3, INGOT, INGOT * 3, INGOT * 2, INGOT * 2};
    /** Helmet, chestplate, leggings, boots. */
    public static final int[] ARMOR = {INGOT * 5, INGOT * 8, INGOT * 7, INGOT * 4};

    private MetalRates() {}
}
