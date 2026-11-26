package mariculture.domain.metal;

import java.util.ArrayList;
import java.util.List;

/**
 * Hammer (core/items/ItemHammer, tool material BRICK: level 1, 1000 uses, speed 3, damage 1.2, enchantability 12;
 * Core.java:173, recipe Recipes.java:35, repaired with burnt bricks). A pickaxe that is three times faster on stone
 * and, when it breaks a block it is effective on (not the anvil), also breaks blocks of the same type in a fixed
 * pattern below and up to two blocks further in the direction the player looks (ItemHammer.onBlockDestroyed).
 */
public final class Hammer {
    public static final int USES = 1000;
    public static final float SPEED = 3.0f;
    public static final float DAMAGE = 1.2f;
    public static final int ENCHANTABILITY = 12;
    public static final float STONE_SPEED_FACTOR = 3.0f;

    private Hammer() {}

    /** Offsets broken with the block, for the horizontal look direction (dx, dz). */
    public static List<int[]> area(int dx, int dz) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[] {0, -1, 0});
        for (int i = 1; i <= 2; i++) {
            int x = i * dx, z = i * dz;
            out.add(new int[] {x, 0, z});
            out.add(new int[] {x, -1, z});
            out.add(new int[] {x - 1, 0, z});
            out.add(new int[] {x + 1, -1, z});
            out.add(new int[] {x, 0, z - 1});
            out.add(new int[] {x, -1, z + 1});
        }
        return out;
    }
}
