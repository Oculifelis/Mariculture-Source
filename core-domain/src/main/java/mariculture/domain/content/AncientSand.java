package mariculture.domain.content;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.machine.Stack;
import mariculture.domain.random.Rng;

/**
 * Ancient sand drops (core/blocks/BlockGround.getDrops, 1.3.0, "nerf food" off): each roll "nextInt(n) <= fortune"
 * gives a bone (n 7), bone meal (3), an ink sac (15), a gold nugget (64), a chest (48), a trapped chest (52); with
 * nothing, the block itself.
 */
public final class AncientSand {
    private static final Object[][] DROPS = {{"minecraft:bone", 7}, {"minecraft:bone_meal", 3}, {"minecraft:ink_sac", 15},
            {"minecraft:gold_nugget", 64}, {"minecraft:chest", 48}, {"minecraft:trapped_chest", 52}};

    private AncientSand() {}

    public static List<Stack> drops(int fortune, Rng rng) {
        List<Stack> out = new ArrayList<>();
        for (Object[] d : DROPS) if (rng.nextInt((Integer) d[1]) <= fortune) out.add(Stack.of((String) d[0]));
        if (out.isEmpty()) out.add(Stack.of("mariculture:ancient_sand"));
        return out;
    }
}
