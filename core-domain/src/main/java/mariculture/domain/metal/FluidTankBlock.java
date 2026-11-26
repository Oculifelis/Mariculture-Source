package mariculture.domain.metal;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.Tank;

/**
 * Copper, aluminium and titanium tanks (core/tile/TileTankBlock: 32000, 64000, 128000 mB). Any fluid, filled and
 * drained from every side; the broken block keeps its fluid in the item (BlockTank.removedByPlayer,
 * onBlockPlacedBy). No screen.
 */
public final class FluidTankBlock extends Machine {
    public static final MachineType COPPER = type("copper_tank", 32000);
    public static final MachineType ALUMINUM = type("aluminum_tank", 64000);
    public static final MachineType TITANIUM = type("titanium_tank", 128000);
    private static final int[] NONE = {};

    private final MachineType type;

    private static MachineType type(String id, int capacity) {
        MachineType[] self = new MachineType[1];
        self[0] = new MachineType(id, () -> new FluidTankBlock(self[0], capacity), null);
        return self[0];
    }

    private FluidTankBlock(MachineType type, int capacity) {
        this.type = type;
        tanks.add(new Tank(capacity));
    }

    @Override
    public MachineType type() {
        return type;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public int[] slots(Face side) {
        return NONE;
    }

    @Override
    public boolean keepsContents() {
        return true;
    }
}
