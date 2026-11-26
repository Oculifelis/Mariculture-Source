package mariculture.platform.machine;

import java.util.List;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import mariculture.domain.content.Face;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.Tank;

/** Exposes domain tanks as a loader fluid handler, asking the machine which sides may fill or drain. */
public class TankHandler implements IFluidHandler {
    private final List<Tank> tanks;
    private final Machine machine;
    private final Face side;

    public TankHandler(Machine machine, Face side) {
        this.tanks = machine.tanks();
        this.machine = machine;
        this.side = side;
    }

    public TankHandler(List<Tank> tanks) {
        this.tanks = tanks;
        this.machine = null;
        this.side = null;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        Tank t = tanks.get(tank);
        Fluid f = MachineBlockEntity.fluid(t.fluid());
        return f == null || t.isEmpty() ? FluidStack.EMPTY : new FluidStack(f, t.amount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks.get(tank).capacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tanks.get(tank).accepts(MachineBlockEntity.fluidId(stack.getFluid()));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getFluid() == Fluids.EMPTY) return 0;
        String id = MachineBlockEntity.fluidId(resource.getFluid());
        for (int i = 0; i < tanks.size(); i++) {
            if (machine != null && !machine.canFill(i, side, id)) continue;
            int filled = tanks.get(i).fill(id, resource.getAmount(), action.simulate());
            if (filled > 0) {
                if (machine != null && action.execute()) machine.slotChanged(-1);
                return filled;
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        String id = MachineBlockEntity.fluidId(resource.getFluid());
        for (int i = 0; i < tanks.size(); i++) {
            if (machine != null && !machine.canDrain(i, side)) continue;
            int drained = tanks.get(i).drain(id, resource.getAmount(), action.simulate());
            if (drained > 0) {
                if (machine != null && action.execute()) machine.slotChanged(-1);
                return new FluidStack(resource.getFluid(), drained);
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < tanks.size(); i++) {
            if (machine != null && !machine.canDrain(i, side)) continue;
            FluidAmount out = tanks.get(i).drain(maxDrain, action.simulate());
            if (!out.isEmpty()) {
                if (machine != null && action.execute()) machine.slotChanged(-1);
                Fluid f = MachineBlockEntity.fluid(out.fluid());
                return f == null ? FluidStack.EMPTY : new FluidStack(f, out.amount());
            }
        }
        return FluidStack.EMPTY;
    }
}
