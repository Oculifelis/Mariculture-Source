package mariculture.domain.fluid;

/**
 * What a fluid block does, from the legacy BlockFluid subclasses (fishery/blocks/fluids/*,
 * core/blocks/BlockPressurisedWater). Numbers are the legacy ones; targets apply them.
 */
public enum FluidEffect {
    /** Plain fluid. */
    NONE,
    /** Molten metal or glass: sets entities on fire like lava (modern addition: legacy molten fluids had no world block). */
    HOT,
    /** BlockBlood: motion / 2 on all axes, falling capped at -0.05. */
    BLOOD,
    /** BlockCustard: horizontal motion / 5, vertical / 5 when still or / 2 when moving, falling capped at -0.05. */
    CUSTARD,
    /** BlockEnder: teleports the entity to a random spot within +-32 blocks (top solid or liquid block, not lava). */
    ENDER,
    /** BlockFishOil: Regeneration I for 100 ticks to living entities that do not have it. */
    FISH_OIL,
    /** BlockIce: Slowness III for 100 ticks to living entities that do not have it. */
    ICE,
    /** BlockPoison: Poison II for 100 ticks to living entities that do not have it. */
    POISON,
    /** BlockMana: players 8 or more blocks from their bed (respawn point) are teleported to it. */
    MANA,
    /** BlockWind: adds uniform(-0.5, 0.5) to each motion axis every tick inside. */
    WIND,
    /** BlockFlux: on each fluid tick, 1 in 8, a lightning bolt near the block. */
    FLUX,
    /** BlockGunpowder: on each fluid tick, 1 in 8, a 1.5 power explosion that does not start fires. */
    GUNPOWDER,
    /** BlockChlorophyll: on each fluid tick, 1 in 8, grows a flower within +-8 blocks. */
    CHLOROPHYLL,
    /** BlockPressurisedWater: pushes entities along the flow at 0.0845 blocks per tick per unit of flow. */
    PRESSURISED,
    /** Natural gas (core/blocks/BlockAir NATURAL_GAS): players inside are slowed and weakened, it catches fire ({@link NaturalGas}). */
    GAS;

    public static final int TICK_EFFECT_ONE_IN = 8;
    public static final float GUNPOWDER_POWER = 1.5f;
    public static final int EFFECT_TICKS = 100;
    public static final int ENDER_RANGE = 32;
    public static final int MANA_MIN_BED_DISTANCE = 8;
    public static final double PRESSURISED_PUSH = 0.0845;
}
