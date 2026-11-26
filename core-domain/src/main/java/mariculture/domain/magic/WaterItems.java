package mariculture.domain.magic;

/**
 * Infiwater and the Hydrophobic Initiator (magic/ItemInfiwater.java, ItemWaterPlacer.java, MagicEventHandler.java:187-203,
 * core/items/ItemFluidStorage.java, 1.3.0).
 *
 * <p>Infiwater always holds 1000 mB of water and never empties or fills: right-clicking places water like a bucket
 * (not where water evaporates) and right-clicking a tank gives it up to 1000 mB per click. Here it is a fluid item
 * that hands out water endlessly, so anything that drains fluid items can use it.
 *
 * <p>Hydrophobic Initiator: while it is anywhere in the inventory, right-clicking the air with a block in hand places
 * that block on top of the water source the player looks at (like a lily pad). No durability.
 */
public final class WaterItems {
    public static final String INFIWATER = "mariculture:infiwater", INITIATOR = "mariculture:hydrophobic_initiator";
    /** ItemInfiwater.getFluid: water, 1000 mB per use. */
    public static final int INFIWATER_AMOUNT = 1000;
    public static final String INFIWATER_FLUID = "minecraft:water";

    private WaterItems() {}

    /** MagicEventHandler.onPlayerInteract: a right-click into the air places on water when holding a block with the initiator carried. */
    public static boolean placesOnWater(boolean holdingBlock, boolean carriesInitiator) {
        return holdingBlock && carriesInitiator;
    }

    /** How much water Infiwater gives for a request: up to 1000 mB of water, nothing of other fluids. */
    public static int infiwaterGives(String fluid, int requested) {
        return INFIWATER_FLUID.equals(fluid) ? Math.max(0, Math.min(requested, INFIWATER_AMOUNT)) : 0;
    }
}
