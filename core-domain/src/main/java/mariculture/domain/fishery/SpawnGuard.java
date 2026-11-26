package mariculture.domain.fishery;

import java.util.HashMap;
import java.util.Map;

/**
 * The Stargazer's "Prevent Mob Spawns" effect (fishery/Fish.java:151; FishStargazer.java:94-118, commented out in
 * 1.3.0). Legacy added, for every water block of the tank, all 33x17x33 neighbours to one global set that was never
 * cleaned up (BUG-004). Here each feeder holds at most one box, its tank's bounds widened by 16 blocks sideways and
 * 8 up and down, in a per-dimension map keyed by the feeder's position; the feeder replaces or clears it as its fish
 * change, and the block entity clears it when it is broken or unloaded. A spawn check walks the few boxes of its
 * dimension. The box covers the corners between water blocks that the per-block union would leave out.
 */
public final class SpawnGuard {
    public static final int HORIZONTAL = 16;
    public static final int VERTICAL = 8;

    private static final Map<String, Map<Long, int[]>> BOXES = new HashMap<>();

    private SpawnGuard() {}

    /** Sets (box = minX, minY, minZ, maxX, maxY, maxZ in world coordinates) or clears (null) a feeder's box. */
    public static synchronized void set(String dimension, long feeder, int[] box) {
        if (box == null) {
            Map<Long, int[]> m = BOXES.get(dimension);
            if (m != null && m.remove(feeder) != null && m.isEmpty()) BOXES.remove(dimension);
        } else {
            BOXES.computeIfAbsent(dimension, d -> new HashMap<>()).put(feeder, box.clone());
        }
    }

    public static synchronized boolean blocks(String dimension, int x, int y, int z) {
        Map<Long, int[]> m = BOXES.get(dimension);
        if (m == null) return false;
        for (int[] b : m.values()) {
            if (x >= b[0] && y >= b[1] && z >= b[2] && x <= b[3] && y <= b[4] && z <= b[5]) return true;
        }
        return false;
    }

    public static synchronized int size() {
        int n = 0;
        for (Map<Long, int[]> m : BOXES.values()) n += m.size();
        return n;
    }

    /** Server stop. */
    public static synchronized void clear() {
        BOXES.clear();
    }

    /** The guarded box around tank water at these offsets, relative to the feeder; null for no water. */
    public static int[] around(Iterable<int[]> water) {
        int[] b = null;
        for (int[] c : water) {
            if (b == null) {
                b = new int[] {c[0], c[1], c[2], c[0], c[1], c[2]};
            } else {
                for (int i = 0; i < 3; i++) {
                    b[i] = Math.min(b[i], c[i]);
                    b[i + 3] = Math.max(b[i + 3], c[i]);
                }
            }
        }
        if (b == null) return null;
        b[0] -= HORIZONTAL; b[2] -= HORIZONTAL; b[3] += HORIZONTAL; b[5] += HORIZONTAL;
        b[1] -= VERTICAL; b[4] += VERTICAL;
        return b;
    }
}
