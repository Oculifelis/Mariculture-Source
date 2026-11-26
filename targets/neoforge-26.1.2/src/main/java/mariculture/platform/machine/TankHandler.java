package mariculture.platform.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.Tank;

/**
 * Exposes domain tanks through the 26.1 transfer API. Each tank has a snapshot journal so a cancelled
 * transaction restores its fluid and amount.
 */
public class TankHandler implements ResourceHandler<FluidResource> {
    private final List<Tank> tanks;
    private final Machine machine;
    private final Face side;
    private final List<Journal> journals = new ArrayList<>();

    public TankHandler(Machine machine, Face side) {
        this(machine.tanks(), machine, side);
    }

    public TankHandler(List<Tank> tanks) {
        this(tanks, null, null);
    }

    private TankHandler(List<Tank> tanks, Machine machine, Face side) {
        this.tanks = tanks;
        this.machine = machine;
        this.side = side;
        for (Tank t : tanks) journals.add(new Journal(t));
    }

    @Override
    public int size() {
        return tanks.size();
    }

    @Override
    public FluidResource getResource(int index) {
        Fluid f = MachineBlockEntity.fluid(tanks.get(index).fluid());
        return f == null ? FluidResource.EMPTY : FluidResource.of(f);
    }

    @Override
    public long getAmountAsLong(int index) {
        return tanks.get(index).amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return resource.isEmpty() || isValid(index, resource) ? tanks.get(index).capacity() : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && tanks.get(index).accepts(MachineBlockEntity.fluidId(resource.getFluid()));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        String id = MachineBlockEntity.fluidId(resource.getFluid());
        if (machine != null && !machine.canFill(index, side, id)) return 0;
        Tank t = tanks.get(index);
        int fits = t.fill(id, amount, true);
        if (fits <= 0) return 0;
        journals.get(index).updateSnapshots(transaction);
        return t.fill(id, fits, false);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        if (machine != null && !machine.canDrain(index, side)) return 0;
        Tank t = tanks.get(index);
        String id = MachineBlockEntity.fluidId(resource.getFluid());
        int out = t.drain(id, amount, true);
        if (out <= 0) return 0;
        journals.get(index).updateSnapshots(transaction);
        return t.drain(id, out, false);
    }

    /** Snapshot of one tank: its fluid and amount. */
    private final class Journal extends SnapshotJournal<Object[]> {
        private final Tank tank;

        Journal(Tank tank) {
            this.tank = tank;
        }

        @Override
        protected Object[] createSnapshot() {
            return new Object[] {tank.fluid(), tank.amount()};
        }

        @Override
        protected void revertToSnapshot(Object[] snapshot) {
            tank.set((String) snapshot[0], (Integer) snapshot[1]);
        }

        @Override
        protected void onRootCommit(Object[] original) {
            if (machine != null) machine.slotChanged(-1);
        }
    }
}
