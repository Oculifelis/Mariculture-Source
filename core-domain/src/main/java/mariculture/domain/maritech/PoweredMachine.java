package mariculture.domain.maritech;

import java.util.Map;

import mariculture.domain.machine.Battery;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;

/**
 * MariTech powered machines (maritech/tile/base/TileMachinePowered.java, 1.3.0): an energy buffer (FE, legacy RF) of
 * the machine's own size plus its RF upgrades (updateUpgrades:73), filled from neighbours and from an energy item in
 * slot 3, 1000 a tick (update:99). Each working tick uses up to {@link #usage} energy and stops only when none is left.
 *
 * <p>Slots 0-2 are upgrades, 3 the battery slot; the machine's own slots follow.
 */
public abstract class PoweredMachine extends StandardMachine {
    public static final int BATTERY_SLOT = 3, BATTERY_PULL = 1000;
    protected final Battery battery;

    private final int base;

    protected PoweredMachine(int capacity) {
        base = capacity;
        battery = new Battery(capacity);
    }

    /** The machine's own capacity before upgrades (getRFCapacity). */
    protected final int capacity() {
        return base;
    }

    /** Energy used per working tick (updatePowerPerTick). */
    public abstract int usage();

    /** RF upgrades lower usage: modifier 1 - rf / 300000 x 0.75, and usage 1 above 300000 (TileIncubator:148). */
    protected final double rfModifier() {
        return 1D - (upgrades.rf() / 300000D) * 0.75D;
    }

    @Override
    public Battery battery() {
        return battery;
    }

    @Override
    protected void upgradesChanged() {
        battery.setCapacity(capacity() + upgrades.rf());
    }

    protected final boolean hasPower(int ticks) {
        return battery.stored() >= usage() * ticks;
    }

    @Override
    protected void update(MachineWorld world) {
        Stack item = inv.get(BATTERY_SLOT);
        if (item.isEmpty() || EnergyItem.spec(item) == null) return;
        int room = battery.receive(BATTERY_PULL, true);
        if (room <= 0) return;
        EnergyItem.Transfer t = EnergyItem.extract(item, room, false);
        if (t.amount() <= 0) return;
        battery.receive(t.amount(), false);
        inv.set(BATTERY_SLOT, t.stack());
    }

    /**
     * A working tick takes up to its usage (updateMachine:79): any energy at all keeps it going; only an empty buffer
     * stops progress and makes the machine recheck.
     */
    @Override
    protected int progressStep() {
        int used = Math.min(battery.stored(), usage());
        if (used <= 0) {
            markDirty();
            return 0;
        }
        battery.use(used);
        return speed();
    }

    /** Progress per working tick (the speed upgrade total, scaled per machine). */
    protected int speed() {
        return upgrades.speed();
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == BATTERY_SLOT) return EnergyItem.spec(stack) != null;
        return super.validStack(slot, stack);
    }

    @Override
    protected mariculture.domain.machine.SlotInfo info(int slot) {
        return slot == BATTERY_SLOT ? mariculture.domain.machine.SlotInfo.battery() : super.info(slot);
    }

    @Override
    public int slotLimit(int slot) {
        return slot == BATTERY_SLOT ? 1 : super.slotLimit(slot);
    }

    @Override
    public void load(Map<String, ?> in) {
        // Energy stored with RF upgrades is kept until the upgrades are counted on the first tick.
        battery.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        battery.setCapacity(Math.max(capacity() + upgrades.rf(), battery.stored()));
    }

    @Override
    public void describe(java.util.List<mariculture.domain.view.RecipeViews.Line> out) {
        super.describe(out);
        out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.energy", battery.stored(), battery.capacity()));
    }
}
