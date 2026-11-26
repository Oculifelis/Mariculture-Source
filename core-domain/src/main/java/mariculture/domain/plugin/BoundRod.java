package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import mariculture.domain.config.Settings;
import mariculture.domain.random.Rng;

/**
 * Blood Magic's bound fishing rod (plugins/bloodmagic/ItemBoundRod.java, 1.3.0). Sneak-use toggles it; used while
 * activated with a weak blood shard in the hotbar slot left (else right) of the rod, it consumes the shard and fishes
 * every water block in a 7x7x7 cube around the player at once. The Blood Magic soul network cost (500 LP a fish) is
 * not charged: it needs Blood Magic's Java API.
 */
public final class BoundRod {
    public static final String ID = "bound_fishing_rod";
    public static final String WEAK_SHARD = "bloodmagic:weakbloodshard";
    /** Neo Vitae's weak blood shard, which powers the rod the same way. */
    public static final String NEO_VITAE_SHARD = "neovitae:weak_blood_shard";
    /** Item data key for the activated state (legacy NBT "isActive"). */
    public static final String ACTIVE = "isActive";
    public static final int RADIUS = 3;

    private BoundRod() {}

    /** Water blocks that catch, as offsets from the player's block, and where lightning strikes (or null). */
    public record Result(List<int[]> catches, int[] lightning) {}

    /** Fish a shard allows, or -1 when the item is no shard. */
    public static int shardLimit(String item) {
        return WEAK_SHARD.equals(item) || NEO_VITAE_SHARD.equals(item) ? Settings.BOUND_ROD_WEAK_SHARD_FISH.get() : -1;
    }

    /**
     * ItemBoundRod.getShard (ItemBoundRod.java:40-56): the slot left of the selected one, wrapping to 8, else the slot
     * to its right. Legacy wrapped the right slot when it reached 8 rather than 9, so a rod in slot 7 never looked at
     * slot 8; here the right slot wraps from 9 to 0 (BUG-072). Returns the slot or -1.
     */
    public static int shardSlot(int selected, IntPredicate isShard) {
        int left = selected == 0 ? 8 : selected - 1;
        if (isShard.test(left)) return left;
        int right = selected == 8 ? 0 : selected + 1;
        return isShard.test(right) ? right : -1;
    }

    /**
     * ItemBoundRod.catchFish (ItemBoundRod.java:72-110): x, y, z from -3 to 3, each water block catches on a 1 in 5
     * roll, lightning strikes the first block whose 1 in 2048 roll passes (water or not), and it stops once more than
     * {@code maximum} fish are caught (so maximum + 1, as legacy did).
     *
     * @param isWater whether the block at an offset is water, the offset packed by {@link #pack}
     */
    public static Result catches(IntPredicate isWater, int maximum, Rng rng) {
        List<int[]> out = new ArrayList<>();
        int[] strike = null;
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (strike == null && rng.nextInt(2048) == 0) strike = new int[] {x, y, z};
                    if (isWater.test(pack(x, y, z)) && rng.nextInt(5) == 0) {
                        out.add(new int[] {x, y, z});
                        if (out.size() > maximum) return new Result(out, strike);
                    }
                }
            }
        }
        return new Result(out, strike);
    }

    public static int pack(int dx, int dy, int dz) {
        return ((dx + 8) << 8) | ((dy + 8) << 4) | (dz + 8);
    }

    public static int dx(int packed) {
        return (packed >> 8) - 8;
    }

    public static int dy(int packed) {
        return ((packed >> 4) & 15) - 8;
    }

    public static int dz(int packed) {
        return (packed & 15) - 8;
    }
}
