package mariculture.domain.factory;

import mariculture.domain.machine.SlotInfo;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Automatic Dictionary Converter (factory/tile/TileDictionaryItem.java, factory/gui/ContainerDictionary.java,
 * SlotDictionary.java, 1.3.0). 24 filter slots hold one-item copies or item filters; each item put in the input
 * becomes the first filter item it is the same as ({@link Dictionary}), or stays itself, and goes to the four output
 * slots, or out to neighbouring inventories while ejecting is on (default on, TileDictionaryItem:13).
 *
 * <p>An empty-hand click on a filter copy turns it into the next item of its material tag (SlotDictionary.handle).
 * Changed: the middle click that picked among several ore names is gone; the first material tag is used. Items are
 * converted a stack per tick instead of on insertion.
 */
public final class DictionaryConverter extends Machine {
    public static final int FILTERS = 24, INPUT = 24, SLOTS = 29;
    public static final int[] OUT = {25, 26, 27, 28};
    public static final int BUTTON_EJECT = 0, G_EJECT = 0;
    /** Button id of filter {@code i} is CYCLE_BUTTON + i. */
    public static final int CYCLE_BUTTON = 100;
    private static final int[] SIDES = {24, 25, 26, 27, 28};
    public static final MachineType TYPE = new MachineType("automatic_dictionary_converter", DictionaryConverter::new,
            Layout.builder(180).grid(0, 6, 4, 12, 15).slot(INPUT, 139, 20).row(25, 2, 130, 46).row(27, 2, 130, 64)
                    .button(BUTTON_EJECT, 139, 82, 18, 14, "gui.mariculture.eject", G_EJECT).build());

    private EjectSetting eject = EjectSetting.ITEM;
    private MachineWorld lastWorld;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return SLOTS;
    }

    @Override
    public boolean isGhost(int slot) {
        return slot < FILTERS;
    }

    @Override
    public int ghostButton(int slot) {
        return slot < FILTERS ? CYCLE_BUTTON + slot : -1;
    }

    @Override
    public int slotLimit(int slot) {
        return slot < FILTERS ? 1 : 64;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return slot <= INPUT;
    }

    @Override
    public int[] slots(Face side) {
        return SIDES;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == INPUT;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot > INPUT;
    }

    @Override
    public void tick(MachineWorld world) {
        lastWorld = world;
        Stack in = inv.get(INPUT);
        if (in.isEmpty()) return;
        Stack out = convert(world, in.withCount(1)).withCount(in.count());
        if (eject.ejectsItems()) out = world.ejectToNeighbours(out);
        Stack left = inv.insert(out, OUT, false);
        inv.shrink(INPUT, in.count() - left.count());
    }

    /** TileDictionaryItem.convert: the first filter entry the item is the same as, or the item itself. */
    public Stack convert(MachineWorld world, Stack one) {
        java.util.Set<String> tags = world.itemTags(one.item());
        for (int i = 0; i < FILTERS; i++) {
            Stack f = inv.get(i);
            if (f.isEmpty()) continue;
            List<String> candidates = ItemFilter.is(f) ? ItemFilter.entries(f) : List.of(f.item());
            for (String c : candidates) {
                if (Dictionary.same(c, world.itemTags(c), one.item(), tags)) return Stack.of(c, 1);
            }
        }
        return one;
    }

    @Override
    public int[] gauges() {
        return new int[] {eject.ordinal()};
    }

    @Override
    public void button(int id) {
        if (id == BUTTON_EJECT) eject = eject.toggle(EjectSetting.ITEM);
        if (id >= CYCLE_BUTTON && id < CYCLE_BUTTON + FILTERS && lastWorld != null) {
            int slot = id - CYCLE_BUTTON;
            Stack f = inv.get(slot);
            if (f.isEmpty() || ItemFilter.is(f)) return;
            List<String> tags = Dictionary.materialTags(lastWorld.itemTags(f.item()));
            if (tags.isEmpty()) return;
            inv.set(slot, Stack.of(Dictionary.next(f.item(), lastWorld.itemsInTag(tags.get(0))), 1));
        }
    }

    @Override
    public void save(Map<String, Object> out) {
        out.put("eject", eject.ordinal());
    }

    @Override
    public void load(Map<String, ?> in) {
        eject = in.containsKey("eject") ? EjectSetting.byOrdinal(integer(in.get("eject"), 1)) : EjectSetting.ITEM;
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        if (slot < FILTERS) return SlotInfo.FILTER;
        return slot == INPUT ? SlotInfo.ANY : SlotInfo.OUTPUT;
    }
}
