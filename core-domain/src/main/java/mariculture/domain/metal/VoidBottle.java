package mariculture.domain.metal;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Tank;

/**
 * Void bottle (core/tile/TileVoidBottle: a placed void bottle takes any amount of any fluid and destroys it; it
 * gives none back). The tank is emptied every tick.
 */
public final class VoidBottle extends Machine {
    public static final MachineType TYPE = new MachineType("void_bottle", VoidBottle::new, null);
    private static final int[] NONE = {};
    private final Tank sink = new Tank(Integer.MAX_VALUE);

    public VoidBottle() {
        tanks.add(sink);
    }

    @Override
    public MachineType type() {
        return TYPE;
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
    public boolean canDrain(int tank, Face side) {
        return false;
    }

    @Override
    public void slotChanged(int slot) {
        sink.set(null, 0);
    }

    @Override
    public void tick(MachineWorld world) {
        sink.set(null, 0);
    }
}
