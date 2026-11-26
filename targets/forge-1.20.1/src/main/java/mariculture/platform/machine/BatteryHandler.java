package mariculture.platform.machine;

import net.minecraftforge.energy.IEnergyStorage;

import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Machine;

/** Exposes a machine's domain battery as FE. */
public class BatteryHandler implements IEnergyStorage {
    private final Battery battery;
    private final Machine machine;

    public BatteryHandler(Machine machine) {
        this.machine = machine;
        this.battery = machine.battery();
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
        int in = battery.receive(max, simulate);
        if (in > 0 && !simulate) machine.slotChanged(-1);
        return in;
    }

    @Override
    public int extractEnergy(int max, boolean simulate) {
        int out = battery.extract(max, simulate);
        if (out > 0 && !simulate) machine.slotChanged(-1);
        return out;
    }

    @Override
    public int getEnergyStored() {
        return battery.stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return battery.capacity();
    }

    @Override
    public boolean canExtract() {
        return battery.canExtract();
    }

    @Override
    public boolean canReceive() {
        return battery.canReceive();
    }
}
