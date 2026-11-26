package mariculture.domain.machine;

import java.util.Map;

/**
 * A single-fluid tank in millibuckets (legacy core/util/Tank over Forge's FluidTank). Fluids are
 * registry ids; an empty tank has no fluid. Targets expose tanks through their loader's fluid API.
 */
public final class Tank {
    private String fluid;
    private int amount;
    private int capacity;
    private final java.util.function.Predicate<String> accepts;

    public Tank(int capacity) {
        this(capacity, f -> true);
    }

    public Tank(int capacity, java.util.function.Predicate<String> accepts) {
        this.capacity = capacity;
        this.accepts = accepts;
    }

    public String fluid() {
        return amount > 0 ? fluid : null;
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    public int space() {
        return Math.max(0, capacity - amount);
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    public boolean isFull() {
        return amount >= capacity;
    }

    public boolean contains(String id) {
        return amount > 0 && fluid.equals(id);
    }

    public boolean accepts(String id) {
        return id != null && accepts.test(id) && (isEmpty() || fluid.equals(id));
    }

    /** Legacy setCapacity followed by clamping the contents (TileMachineTank.updateUpgrades). */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        if (amount > this.capacity) amount = this.capacity;
    }

    /** Adds up to {@code max} of {@code id}; returns how much fits (or was added). */
    public int fill(String id, int max, boolean simulate) {
        if (max <= 0 || !accepts(id)) return 0;
        int added = Math.min(max, space());
        if (!simulate && added > 0) {
            fluid = id;
            amount += added;
        }
        return added;
    }

    /** Removes up to {@code max} of whatever is inside. */
    public FluidAmount drain(int max, boolean simulate) {
        if (isEmpty() || max <= 0) return FluidAmount.NONE;
        int taken = Math.min(max, amount);
        FluidAmount out = new FluidAmount(fluid, taken);
        if (!simulate) {
            amount -= taken;
            if (amount <= 0) {
                amount = 0;
                fluid = null;
            }
        }
        return out;
    }

    /** Removes up to {@code max} only if the tank holds {@code id}. */
    public int drain(String id, int max, boolean simulate) {
        if (!contains(id)) return 0;
        return drain(max, simulate).amount();
    }

    public void set(String id, int amt) {
        if (id == null || amt <= 0) {
            fluid = null;
            amount = 0;
        } else {
            fluid = id;
            amount = Math.min(amt, capacity);
        }
    }

    public void save(Map<String, Object> out, String key) {
        if (!isEmpty()) {
            out.put(key + "_fluid", fluid);
            out.put(key + "_amount", amount);
        }
    }

    public void load(Map<String, ?> in, String key) {
        Object f = in.get(key + "_fluid");
        Object a = in.get(key + "_amount");
        set(f instanceof String s ? s : null, a instanceof Number n ? n.intValue() : 0);
    }
}
