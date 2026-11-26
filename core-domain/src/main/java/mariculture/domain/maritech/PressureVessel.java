package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;
import mariculture.domain.machine.Tank;

/**
 * Pressure vessel (maritech/tile/TilePressureVessel.java, 1.3.0): a big tank. It holds (200 + storage x 8) buckets of
 * any one fluid (getTankCapacity:28), takes and gives containers in slots 3 and 4, and tops up a FLUDD in slot 5 from
 * its pressurised water (fillSpecial:96). It never works or ejects on its own (canUpdate false); pipes fill and empty it.
 *
 * <p>Changed: each block keeps its own tank, and every second touching vessels holding the same fluid (or nothing)
 * share it out in proportion to their sizes. Legacy joined them into one tank with a master block; the blocks together
 * hold the same and fill and empty as one. The book's auto-eject never ran in legacy (canUpdate false) and is not ported.
 */
public final class PressureVessel extends StandardMachine {
    public static final int BUCKETS = 200, PER_STORAGE = 8, IN = 3, OUT = 4, SPECIAL = 5, SLOTS = 6;
    private static final int[] NONE = {};
    private static final int[] SIDED = {IN, OUT, SPECIAL};

    public static final MachineType TYPE = new MachineType("pressure_vessel", PressureVessel::new, Layout.builder(176)
            .tank(0, 62, 17, 52, 54)
            .slot(IN, 26, 17).slot(OUT, 26, 53)
            .slot(SPECIAL, 134, 35)
            .standardSidePanel(true, false)
            .build());

    private final Tank tank = new Tank(capacity(0));

    public PressureVessel() {
        tanks.add(tank);
        mode = mariculture.domain.machine.RedstoneMode.DISABLED;
    }

    public static int capacity(int storage) {
        return Math.max(1, (BUCKETS + storage * PER_STORAGE) * 1000);
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return SLOTS;
    }

    @Override
    protected int max() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int[] outputs() {
        return NONE;
    }

    @Override
    protected void upgradesChanged() {
        tank.setCapacity(Math.max(capacity(upgrades.storage()), tank.amount()));
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == SPECIAL) return stack.item().equals(Fludd.ITEM);
        if (slot == OUT) return false;
        return super.validStack(slot, stack);
    }

    @Override
    public int slotLimit(int slot) {
        return slot == SPECIAL ? 1 : super.slotLimit(slot);
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == IN || slot == SPECIAL && stack.item().equals(Fludd.ITEM);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot >= OUT;
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        return false;
    }

    @Override
    protected void process(MachineWorld world) {}

    /** Legacy did this whenever a slot changed; here every 10 ticks. */
    @Override
    protected void update(MachineWorld world) {
        if (world.gameTime() % 20 == 0) share(world);
        if (world.gameTime() % 10 != 0) return;
        Stack fludd = inv.get(SPECIAL);
        if (!fludd.isEmpty()) {
            Stack filled = Fludd.fill(fludd, tank);
            if (!filled.equals(fludd)) {
                inv.setData(SPECIAL, filled.data());
                markDirty();
            }
        }
        Stack in = inv.get(IN);
        if (in.isEmpty() || !inv.get(OUT).isEmpty()) return;
        MachineWorld.ContainerResult r = world.processContainer(in, tank);
        if (r == null) return;
        inv.shrink(IN, 1);
        Stack left = inv.insert(r.container(), new int[] {OUT}, false);
        if (!left.isEmpty()) world.drop(left);
        markDirty();
    }

    /** Balances the fluid with each touching vessel, in proportion to the tank sizes. */
    private void share(MachineWorld world) {
        for (Face f : Face.values()) {
            int[] d = mariculture.domain.factory.Geyser.offset(f);
            if (!(world.machine(d[0], d[1], d[2]) instanceof PressureVessel other)) continue;
            Tank o = other.tank;
            String fluid = tank.isEmpty() ? o.fluid() : tank.fluid();
            if (fluid == null || !o.isEmpty() && !o.contains(fluid)) continue;
            long total = (long) tank.amount() + o.amount();
            int mine = (int) (total * tank.capacity() / ((long) tank.capacity() + o.capacity()));
            if (mine == tank.amount()) continue;
            tank.set(fluid, mine);
            o.set(fluid, (int) (total - mine));
            markDirty();
            other.markDirty();
        }
    }

    @Override
    public void load(Map<String, ?> in) {
        tank.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        tank.setCapacity(Math.max(capacity(upgrades.storage()), tank.amount()));
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == IN) return SlotInfo.fluidIn();
        if (slot == OUT) return SlotInfo.OUTPUT;
        if (slot == SPECIAL) return SlotInfo.of(SlotInfo.Role.CATALYST, "fludd", java.util.List.of(Fludd.ITEM), s -> s.item().equals(Fludd.ITEM));
        return super.info(slot);
    }
}
