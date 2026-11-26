package mariculture.domain.machine;

import java.util.Map;

/**
 * A machine's energy buffer (legacy cofh EnergyStorage, RF). Targets expose it as Forge Energy (FE), which counts
 * the same as RF.
 */
public class Battery {
    private int capacity;
    private int maxReceive;
    private int maxExtract;
    /** Whether the transfer limits follow the capacity (cofh EnergyStorage(capacity)). */
    private final boolean limitsFollow;
    private int stored;

    public Battery(int capacity) {
        this(capacity, capacity, capacity, true);
    }

    public Battery(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, false);
    }

    private Battery(int capacity, int maxReceive, int maxExtract, boolean limitsFollow) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.limitsFollow = limitsFollow;
    }

    /** EnergyStorage.setCapacity: stored energy above the new size is lost (RF upgrades taken out). */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        if (limitsFollow) {
            maxReceive = this.capacity;
            maxExtract = this.capacity;
        }
        stored = Math.min(stored, this.capacity);
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return capacity;
    }

    public boolean canReceive() {
        return maxReceive > 0;
    }

    public boolean canExtract() {
        return maxExtract > 0;
    }

    /** EnergyStorage.receiveEnergy: how much was (or would be) taken. */
    public int receive(int max, boolean simulate) {
        int in = Math.max(0, Math.min(capacity - stored, Math.min(maxReceive, max)));
        if (!simulate) stored += in;
        return in;
    }

    /** EnergyStorage.extractEnergy: how much came (or would come) out. */
    public int extract(int max, boolean simulate) {
        int out = Math.max(0, Math.min(stored, Math.min(maxExtract, max)));
        if (!simulate) stored -= out;
        return out;
    }

    /** Machine-internal use, ignoring the transfer limits. */
    public boolean use(int amount) {
        if (stored < amount) return false;
        stored -= amount;
        return true;
    }

    public void set(int amount) {
        stored = Math.max(0, Math.min(capacity, amount));
    }

    public void save(Map<String, Object> out) {
        out.put("Energy", stored);
    }

    public void load(Map<String, ?> in) {
        set(in.get("Energy") instanceof Number n ? n.intValue() : 0);
    }
}
