package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.List;

/** An in-memory inventory for machine tests; every item stacks to 64. */
public final class ListInventory implements Inventory {
    private final List<Stack> slots = new ArrayList<>();
    private final Machine machine;

    public ListInventory(Machine machine) {
        this.machine = machine;
        for (int i = 0; i < machine.slotCount(); i++) slots.add(Stack.EMPTY);
        machine.attach(this);
    }

    @Override
    public int size() {
        return slots.size();
    }

    @Override
    public Stack get(int slot) {
        return slots.get(slot);
    }

    @Override
    public void set(int slot, Stack stack) {
        slots.set(slot, stack);
        machine.slotChanged(slot);
    }

    @Override
    public int maxStackSize(Stack stack) {
        return 64;
    }

    @Override
    public int slotLimit(int slot) {
        return machine.slotLimit(slot);
    }

    /** Enchantments ({level, weight} pairs) of the item in a slot, for ethereal melting tests. */
    public final java.util.Map<Integer, List<int[]>> enchanted = new java.util.HashMap<>();

    @Override
    public List<int[]> enchantments(int slot) {
        return enchanted.getOrDefault(slot, List.of());
    }

    @Override
    public boolean moveDisenchanted(int from, int to) {
        if (!get(to).isEmpty() || get(from).isEmpty()) return false;
        Stack s = get(from);
        set(to, s.withCount(1));
        set(from, s.shrink(1));
        if (get(from).isEmpty()) enchanted.remove(from);
        return true;
    }

    @Override
    public boolean hasLoaderData(int slot) {
        return enchanted.containsKey(slot);
    }

    /** Enchantments put on a slot's item by {@link #enchant}, for jewelry finishing tests. */
    public final java.util.Map<Integer, java.util.Map<String, Integer>> applied = new java.util.HashMap<>();

    @Override
    public void enchant(int slot, java.util.Map<String, Integer> enchantments) {
        applied.put(slot, enchantments);
    }
}
