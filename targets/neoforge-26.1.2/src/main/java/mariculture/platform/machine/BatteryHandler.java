package mariculture.platform.machine;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Machine;

/** Exposes a machine's domain battery as FE through the 26.1 transfer API; a cancelled transaction restores it. */
public class BatteryHandler implements EnergyHandler {
    private final Battery battery;
    private final Machine machine;
    private final Journal journal = new Journal();

    public BatteryHandler(Machine machine) {
        this.machine = machine;
        this.battery = machine.battery();
    }

    @Override
    public long getAmountAsLong() {
        return battery.stored();
    }

    @Override
    public long getCapacityAsLong() {
        return battery.capacity();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        int in = battery.receive(amount, true);
        if (in <= 0) return 0;
        journal.updateSnapshots(transaction);
        return battery.receive(in, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        int out = battery.extract(amount, true);
        if (out <= 0) return 0;
        journal.updateSnapshots(transaction);
        return battery.extract(out, false);
    }

    private final class Journal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return battery.stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            battery.set(snapshot);
        }

        @Override
        protected void onRootCommit(Integer original) {
            machine.slotChanged(-1);
        }
    }
}
