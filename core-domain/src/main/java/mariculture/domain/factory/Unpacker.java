package mariculture.domain.factory;

import mariculture.domain.machine.SlotInfo;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Unpacker (factory/tile/TileUnpacker.java, factory/UnpackerHelper.java, 1.3.0): items put in from above are
 * uncrafted with their one-item crafting recipe (a block of iron into nine ingots) into the inventory below. It has
 * no screen; only automation from the top may insert, and only items that uncraft (canInsertItem:25).
 *
 * <p>Changed: legacy uncrafted one item from whatever stack it was handed and kept nothing, so a larger stack put
 * in at once lost the rest; here each item is uncrafted in turn and a result the inventory below cannot take waits
 * in an internal slot until it can.
 */
public final class Unpacker extends Machine {
    public static final MachineType TYPE = new MachineType("unpacker", Unpacker::new, null);
    public static final int INPUT = 0, BUFFER = 1;
    private static final int[] TOP = {INPUT};
    private static final int[] NONE = {};
    private MachineWorld lastWorld;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 2;
    }

    @Override
    public int[] slots(Face side) {
        return side == Face.UP ? TOP : NONE;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return slot == INPUT && (lastWorld == null || !lastWorld.uncraft(stack.withCount(1)).isEmpty());
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return side == Face.UP && isValid(slot, stack);
    }

    @Override
    public void tick(MachineWorld world) {
        lastWorld = world;
        Stack buffer = inv.get(BUFFER);
        if (!buffer.isEmpty()) {
            inv.set(BUFFER, world.ejectTo(Face.DOWN, buffer));
            return;
        }
        Stack in = inv.get(INPUT);
        if (in.isEmpty()) return;
        Stack out = world.uncraft(in.withCount(1));
        if (out.isEmpty()) return;
        inv.shrink(INPUT, 1);
        inv.set(BUFFER, world.ejectTo(Face.DOWN, out));
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return slot == INPUT ? SlotInfo.input("uncraftable", java.util.List.of(), null) : SlotInfo.OUTPUT;
    }
}
