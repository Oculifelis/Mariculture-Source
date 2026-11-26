package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The shared behaviour of legacy TileMachine / TileMachineTank (core/tile/base): three upgrade slots,
 * redstone mode, eject setting, a progress counter that advances by the speed upgrade total and runs
 * {@link #process} when it reaches {@link #max()}, and output slots that items go to (or out of the
 * machine when ejecting).
 *
 * <p>Legacy re-evaluated canWork whenever a slot or tank changed. Here a change marks the machine dirty
 * and canWork is re-evaluated on the next tick, plus every 20 ticks so world conditions (time of day,
 * redstone) are noticed even without a change.
 */
public abstract class StandardMachine extends Machine {
    public static final int G_REDSTONE = 0;
    public static final int G_EJECT = 1;
    public static final int G_PROGRESS = 2;
    public static final int G_MAX = 3;
    /** First gauge index free for a subclass. */
    public static final int G_EXTRA = 4;
    public static final int BUTTON_REDSTONE = 0;
    public static final int BUTTON_EJECT = 1;
    /** core/config/Machines.java "Item Eject Tick" and "Fluid Eject Tick". */
    public static final int ITEM_EJECT_TICK = 20;
    public static final int FLUID_EJECT_TICK = 100;
    public static final int[] UPGRADE_SLOTS = {0, 1, 2};

    protected RedstoneMode mode = RedstoneMode.LOW;
    protected EjectSetting eject = EjectSetting.NONE;
    protected Upgrades.Totals upgrades = Upgrades.Totals.NONE;
    protected int processed;
    protected boolean canWork;
    private boolean dirty = true;

    /** Ticks of work (scaled by speed) per {@link #process} call. */
    protected abstract int max();

    /** Output slots, in insertion order. */
    protected abstract int[] outputs();

    /** What this machine can eject (legacy getEjectType). */
    protected EjectSetting ejectType() {
        return outputs().length > 0 ? EjectSetting.ITEM : EjectSetting.NONE;
    }

    protected boolean hasUpgradeSlots() {
        return true;
    }

    /** Legacy canWork(): whether processing may advance right now. Redstone is checked by the caller. */
    protected abstract boolean canWork(MachineWorld world);

    /**
     * True when the machine stopped only for lack of power. Its progress then waits instead of resetting (changed from
     * legacy, which reset it, so a DNA machine fed by batteries that ran dry never finished).
     */
    protected boolean waitingForPower(MachineWorld world) {
        return false;
    }

    /** Called each time progress completes. */
    protected abstract void process(MachineWorld world);

    /** Progress added per working tick (legacy: the speed upgrade total; the crucible adds heat). */
    protected int progressStep() {
        return upgrades.speed();
    }

    /** Called every tick before progress (legacy update()). */
    protected void update(MachineWorld world) {}

    /** Called when the upgrade totals change (tank capacities etc.). */
    protected void upgradesChanged() {}

    @Override
    public final void tick(MachineWorld world) {
        if (dirty || world.gameTime() % 20 == 0) {
            dirty = false;
            refreshUpgrades();
            canWork = mode.canWork(world.poweredByRedstone()) && canWork(world);
        }
        update(world);
        autoEject(world);
        if (canWork) {
            processed += progressStep();
            if (processed >= max()) {
                process(world);
                processed = 0;
                canWork = mode.canWork(world.poweredByRedstone()) && canWork(world);
            }
        } else if (!waitingForPower(world)) {
            processed = 0;
        }
    }

    protected final void markDirty() {
        dirty = true;
    }

    private void refreshUpgrades() {
        if (!hasUpgradeSlots()) return;
        List<Stack> stacks = new ArrayList<>(3);
        for (int slot : UPGRADE_SLOTS) stacks.add(inv.get(slot));
        Upgrades.Totals t = Upgrades.totals(stacks);
        if (!t.equals(upgrades)) {
            upgrades = t;
            upgradesChanged();
        }
    }

    public Upgrades.Totals upgrades() {
        return upgrades;
    }

    /** Legacy autoeject(): every 20 ticks, push output slot contents out when item ejecting is on. */
    private void autoEject(MachineWorld world) {
        if (outputs().length == 0 || world.gameTime() % mariculture.domain.config.Settings.ITEM_EJECT_TICK.get() != 0 || !eject.ejectsItems()) return;
        for (int slot : outputs()) {
            Stack s = inv.get(slot);
            if (s.isEmpty() || inv.hasLoaderData(slot)) continue;
            inv.set(slot, Stack.EMPTY);
            insertOutput(world, s);
        }
    }

    /**
     * Legacy BlockTransferHelper.insertStack: neighbours first when ejecting items, then the output
     * slots, then dropped on top of the machine.
     */
    protected final void insertOutput(MachineWorld world, Stack stack) {
        if (stack.isEmpty()) return;
        if (eject.ejectsItems()) stack = world.ejectToNeighbours(stack);
        if (stack.isEmpty()) return;
        stack = inv.insert(stack, outputs(), false);
        if (!stack.isEmpty()) world.drop(stack);
        markDirty();
    }

    /** Legacy hasRoom: ejecting always has room, otherwise an empty output slot is needed. */
    protected final boolean hasRoom() {
        if (eject.ejectsItems()) return true;
        for (int slot : outputs()) if (inv.get(slot).isEmpty()) return true;
        return false;
    }

    protected final boolean isUpgradeSlot(int slot) {
        return hasUpgradeSlots() && slot >= 0 && slot < 3;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        if (isUpgradeSlot(slot)) return Upgrades.isUpgrade(stack);
        return validStack(slot, stack);
    }

    @Override
    public final SlotInfo slotInfo(int slot) {
        if (isUpgradeSlot(slot)) return SlotInfo.upgrade();
        for (int out : outputs()) if (out == slot) return SlotInfo.OUTPUT;
        return info(slot);
    }

    /** What a non-upgrade, non-output slot is for. */
    protected SlotInfo info(int slot) {
        return super.slotInfo(slot);
    }

    /** Validity of a non-upgrade slot. */
    protected boolean validStack(int slot, Stack stack) {
        for (int out : outputs()) if (out == slot) return false;
        return true;
    }

    @Override
    public int slotLimit(int slot) {
        return isUpgradeSlot(slot) ? 1 : 64;
    }

    @Override
    public void slotChanged(int slot) {
        markDirty();
    }

    @Override
    public int[] gauges() {
        return new int[] {mode.ordinal(), eject.ordinal(), processed, max()};
    }

    /** Builds a gauge array with the standard four first and the given extras after. */
    protected final int[] gauges(int... extra) {
        int[] g = new int[G_EXTRA + extra.length];
        g[G_REDSTONE] = mode.ordinal();
        g[G_EJECT] = eject.ordinal();
        g[G_PROGRESS] = processed;
        g[G_MAX] = max();
        System.arraycopy(extra, 0, g, G_EXTRA, extra.length);
        return g;
    }

    @Override
    public void describe(java.util.List<mariculture.domain.view.RecipeViews.Line> out) {
        super.describe(out);
        if (processed > 0 && max() > 0) out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.progress", processed * 100L / max()));
    }

    @Override
    public void button(int id) {
        if (id == BUTTON_REDSTONE) mode = mode.next();
        if (id == BUTTON_EJECT) eject = eject.toggle(ejectType());
        markDirty();
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("redstone_mode", mode.ordinal());
        out.put("eject", eject.ordinal());
        out.put("processed", processed);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        mode = RedstoneMode.byOrdinal(in.get("redstone_mode") instanceof Number n ? n.intValue() : RedstoneMode.LOW.ordinal());
        eject = EjectSetting.byOrdinal(in.get("eject") instanceof Number n ? n.intValue() : 0);
        processed = in.get("processed") instanceof Number n ? n.intValue() : 0;
        markDirty();
    }
}
