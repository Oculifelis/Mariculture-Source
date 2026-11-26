package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;

/**
 * Platform-neutral machine logic. A target block entity owns the item slots (exposed as an
 * {@link Inventory}), calls {@link #tick} on the server and saves {@link #save} output next to the
 * slots. Everything a machine needs from the world comes through {@link MachineWorld}.
 */
public abstract class Machine {
    protected Inventory inv;
    protected final List<Tank> tanks = new ArrayList<>();

    public void attach(Inventory inventory) {
        this.inv = inventory;
    }

    public Inventory inventory() {
        return inv;
    }

    public abstract MachineType type();

    public abstract int slotCount();

    /** Server tick. */
    public void tick(MachineWorld world) {}

    /** Whether a player may put this stack into the slot (menu slots). */
    public boolean isValid(int slot, Stack stack) {
        return true;
    }

    /** Automation (hoppers, pipes) insertion. */
    public boolean canInsert(int slot, Stack stack, Face side) {
        return isValid(slot, stack);
    }

    public boolean canExtract(int slot, Face side) {
        return false;
    }

    /** Slots automation can see from a side. */
    public int[] slots(Face side) {
        int[] all = new int[slotCount()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return all;
    }

    /** Called after any slot changes (menu, automation or the machine itself). */
    public void slotChanged(int slot) {}

    /**
     * Ghost slots hold a one-item copy set by clicking with a stack (legacy SlotFake): the player's stack is not
     * taken, right-click clears, an empty-hand click presses button {@link #ghostButton}. They never drop.
     */
    public boolean isGhost(int slot) {
        return false;
    }

    /** The button an empty-hand left click on a ghost slot presses, or -1. */
    public int ghostButton(int slot) {
        return -1;
    }

    /** What a slot is for, shown in the menu (border, tooltip, faint item when empty). */
    public SlotInfo slotInfo(int slot) {
        return isGhost(slot) ? SlotInfo.FILTER : SlotInfo.ANY;
    }

    /**
     * The machine slots a shift-clicked stack goes to, in the order to try: slots made for the stack, then slots for
     * any item, then slots that accept it though made for something else; within each, fuel, catalyst and upgrade
     * slots first, then inputs, then storage. Never outputs, filters, ghost slots, slots that refuse the stack, or
     * fuel, catalyst and upgrade slots the stack is not meant for.
     */
    public int[] quickMoveOrder(Stack stack) {
        List<int[]> found = new ArrayList<>();
        for (int s = 0; s < slotCount(); s++) {
            if (isGhost(s) || slotLimit(s) <= 0 || !isValid(s, stack)) continue;
            SlotInfo info = slotInfo(s);
            int order = info.role().order();
            if (order < 0) continue;
            int fit = info.fits() == null ? 1 : info.fits().test(stack) ? 0 : 2;
            if (fit == 2 && order == 0) continue; // fuel, catalyst and upgrade slots take only what they are for
            found.add(new int[] {fit * 10 + order, s});
        }
        found.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        int[] out = new int[found.size()];
        for (int i = 0; i < out.length; i++) out[i] = found.get(i)[1];
        return out;
    }

    /** Per-slot stack limit (1 for upgrade slots). */
    public int slotLimit(int slot) {
        return 64;
    }

    /** Values the open menu shows (progress, temperatures...). Index meaning is fixed per machine type. */
    public int[] gauges() {
        return new int[0];
    }

    /** A menu button press (legacy handleButtonClick). */
    public void button(int id) {}

    /** The energy buffer outside energy handlers may fill or drain, or null for machines without one. */
    public Battery battery() {
        return null;
    }

    public List<Tank> tanks() {
        return Collections.unmodifiableList(tanks);
    }

    /** Whether outside fluid handlers may fill/drain a tank from a side (null side means internal). */
    public boolean canFill(int tank, Face side, String fluid) {
        return true;
    }

    public boolean canDrain(int tank, Face side) {
        return true;
    }

    public void save(Map<String, Object> out) {
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).save(out, "tank" + i);
        if (battery() != null) battery().save(out);
    }

    public void load(Map<String, ?> in) {
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).load(in, "tank" + i);
        if (battery() != null) battery().load(in);
    }

    /** Reads a saved flag; targets may store booleans as numbers (NBT bytes). */
    protected static boolean bool(Object o) {
        return Boolean.TRUE.equals(o) || (o instanceof Number n && n.intValue() != 0);
    }

    protected static int integer(Object o, int fallback) {
        return o instanceof Number n ? n.intValue() : fallback;
    }

    /** Light the block emits (feeder with glowing fish). */
    public int light() {
        return 0;
    }

    /** Whether the broken block's item keeps the machine's saved state (tanks). Such machines have no slots. */
    public boolean keepsContents() {
        return false;
    }

    /**
     * A right-click (without sneaking) handled by the machine instead of opening its screen: the stacks to hand to
     * the player, or null to open the screen (legacy casters give out their casts).
     */
    public java.util.List<Stack> onUse() {
        return null;
    }

    /**
     * Whether a plain right-click goes to {@link #onUse} instead of opening the screen. Separate from onUse so a block
     * can ask without running it (BUG-071: asking used to empty the caster before it gave anything out).
     */
    public boolean usesClick() {
        return false;
    }

    /**
     * A right-click holding an item that the machine uses itself (the air compressor charges from batteries and fills
     * scuba tanks): the held item afterwards, or null when the machine does nothing with it.
     */
    public Stack useHeld(Stack held, boolean sneaking) {
        return null;
    }

    /** Called when the block is broken, before the slots drop. */
    public void removed(MachineWorld world) {}

    /**
     * Status lines for block tooltips (Jade): every non-empty tank, then whatever the machine adds. Arguments are
     * numbers, strings, or {@link mariculture.domain.view.RecipeViews.Name} for an item or fluid name.
     */
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        for (Tank t : tanks()) {
            if (t.isEmpty()) continue;
            out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.tank", mariculture.domain.view.RecipeViews.Name.fluid(t.fluid()),
                    t.amount(), t.capacity()));
        }
    }
}
