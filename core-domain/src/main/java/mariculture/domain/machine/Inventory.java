package mariculture.domain.machine;

/**
 * A machine's item slots, owned by the target's block entity. Reads convert the loader's stack to a
 * {@link Stack}; writes replace the slot.
 */
public interface Inventory {
    int size();

    Stack get(int slot);

    void set(int slot, Stack stack);

    /** The item's own stack limit (64 for most, 1 for tools, 16 for eggs...). */
    int maxStackSize(Stack stack);

    /** The slot's own limit (a machine's upgrade or fish slot holds one). */
    default int slotLimit(int slot) {
        return 64;
    }

    /** Enchantments on the item in a slot as {level, weight} pairs; empty when it has none. */
    default java.util.List<int[]> enchantments(int slot) {
        return java.util.List.of();
    }

    /**
     * Moves one item from {@code from} into the empty slot {@code to} with its enchantments removed and everything
     * else (damage, name) kept; false when {@code to} is not empty (crucible ethereal melting).
     */
    default boolean moveDisenchanted(int from, int to) {
        return false;
    }

    /**
     * Whether the item in a slot carries loader data a {@link Stack} cannot hold (damage, enchantments, a name).
     * Such items are left in the slot rather than passed around as stacks, which would lose that data.
     */
    default boolean hasLoaderData(int slot) {
        return false;
    }

    /** Puts enchantments (registry id to level) on the item in a slot, replacing any it had (finished jewelry). */
    default void enchant(int slot, java.util.Map<String, Integer> enchantments) {}

    /** Replaces the item data of a slot, keeping the rest of the item (its enchantments) as it is. */
    default void setData(int slot, java.util.Map<String, Object> data) {
        set(slot, get(slot).withData(data));
    }

    /** The level of one enchantment ("minecraft:lure") on the item in a slot. */
    default int enchantLevel(int slot, String enchantment) {
        return 0;
    }

    /** Wears the item in a slot as a tool used without a player (unbreaking applies); it is removed when it breaks. */
    default void damage(int slot, int amount) {}

    default boolean isEmpty(int slot) {
        return get(slot).isEmpty();
    }

    /** Removes {@code n} from a slot. */
    default void shrink(int slot, int n) {
        set(slot, get(slot).shrink(n));
    }

    /**
     * Merges {@code stack} into the given slots, existing matching stacks first, then empty slots
     * (legacy InventoryHelper.addItemStackToInventory). Returns what did not fit.
     */
    default Stack insert(Stack stack, int[] slots, boolean simulate) {
        if (stack.isEmpty()) return Stack.EMPTY;
        int left = stack.count();
        for (int slot : slots) {
            int limit = Math.min(maxStackSize(stack), slotLimit(slot));
            Stack in = get(slot);
            if (in.sameItem(stack) && in.count() < limit) {
                int add = Math.min(left, limit - in.count());
                if (!simulate) set(slot, in.grow(add));
                left -= add;
                if (left <= 0) return Stack.EMPTY;
            }
        }
        for (int slot : slots) {
            if (get(slot).isEmpty()) {
                int add = Math.min(left, Math.min(maxStackSize(stack), slotLimit(slot)));
                if (!simulate) set(slot, stack.withCount(add));
                left -= add;
                if (left <= 0) return Stack.EMPTY;
            }
        }
        return stack.withCount(left);
    }
}
