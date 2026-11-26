package mariculture.domain.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mariculture.domain.random.Rng;

/**
 * Natural gas in the world (core/blocks/BlockAir, meta NATURAL_GAS, 1.3.0). A player inside gets mining fatigue III
 * and weakness I for 5 ticks every tick, plus 1 in 500 nausea for 60 ticks with weakness II for 40, otherwise 1 in
 * 1000 poison for 30. With "Natural Gas Can Catch on Fire" ({@code Settings.GAS_CATCHES_FIRE}, default on), fire, a torch or lava in the 3x3x3 around
 * it on a neighbour change sets fire to the first free side (up, down, west, north, east, south).
 *
 * <p>Change: the gas is the natural gas fluid's block (legacy used a separate air-like block); wall torches and soul
 * fire count as fire too (they are split blocks in modern versions).
 */
public final class NaturalGas {
    public record Effect(String id, int ticks, int amplifier) {}

    public static final Set<String> IGNITERS = Set.of("minecraft:fire", "minecraft:soul_fire", "minecraft:torch", "minecraft:wall_torch",
            "minecraft:lava");
    /** dx, dy, dz in the order BlockAir.trySetFire tries them. */
    public static final int[][] FIRE_SIDES = {{0, 1, 0}, {0, -1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};

    /** BlockAir.getFlammability and getFireSpreadSpeed. */
    public static final int FLAMMABILITY = 300, FIRE_SPREAD_SPEED = 1000;
    /** The gas block that natural gas pockets are made of. */
    public static final String BLOCK = "natural_gas";

    private NaturalGas() {}

    public static List<Effect> effects(Rng rng) {
        List<Effect> out = new ArrayList<>();
        out.add(new Effect("mining_fatigue", 5, 2));
        out.add(new Effect("weakness", 5, 0));
        if (rng.nextInt(500) == 0) {
            out.add(new Effect("nausea", 60, 0));
            out.add(new Effect("weakness", 40, 1));
        } else if (rng.nextInt(1000) == 0) {
            out.add(new Effect("poison", 30, 0));
        }
        return out;
    }
}
