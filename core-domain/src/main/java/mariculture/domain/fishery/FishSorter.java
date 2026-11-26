package mariculture.domain.fishery;

import mariculture.domain.machine.SlotInfo;

import java.util.Arrays;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Fish sorter (factory/tile/TileFishSorter, factory/gui/ContainerFishSorter, fishery/gui/SlotSorter, 1.3.0). 21
 * filter slots hold one-item copies (ghost slots); each filter sends matching items out of one face, everything else
 * goes out of the default face. Items enter slot 21 and are pushed into the inventory on that face, or dropped in
 * front of it when there is none.
 *
 * <p>Matching (TileFishSorter.getSlotForStack, ItemFishy.isSame with isPerfectMatch false): eggs match any eggs; a
 * live fish matches a fish with the same species pair in either order; anything else matches the same item. The
 * first matching filter wins.
 *
 * <p>Changes: filters cycle their face with an empty-hand click (SlotSorter.handle), right-click clears, as before;
 * items go into hoppers too (legacy skipped hoppers); the legacy fish-tank merge that ignored stack limits is replaced
 * by normal insertion (fish tanks hold normal stacks, BUG-033).
 */
public final class FishSorter extends Machine {
    public static final int FILTERS = 21;
    public static final int INPUT = 21;
    public static final int SLOTS = 22;
    public static final int BUTTON_DEFAULT_SIDE = 0;
    public static final int BUTTON_EJECT = 1;
    /** Button id of filter {@code i} is FILTER_BUTTON + i (cycle its face). */
    public static final int FILTER_BUTTON = 100;
    public static final int G_DEFAULT_SIDE = 0;
    public static final int G_EJECT = 1;
    /** Gauge of filter {@code i}'s face is G_FILTER + i. */
    public static final int G_FILTER = 2;
    private static final int[] INPUT_ONLY = {INPUT};

    public static final MachineType TYPE = new MachineType("fish_sorter", FishSorter::new, layout());

    private static Layout layout() {
        Layout.Builder b = Layout.builder(166).grid(0, 7, 3, 38, 21).slot(INPUT, 9, 25)
                .button(BUTTON_DEFAULT_SIDE, 8, 53, 18, 14, "gui.mariculture.default_side", G_DEFAULT_SIDE)
                .button(BUTTON_EJECT, 8, 69, 18, 14, "gui.mariculture.eject", G_EJECT);
        for (int i = 0; i < FILTERS; i++) b.face(G_FILTER + i, 38 + (i % 7) * 18, 21 + (i / 7) * 18);
        return b.build();
    }

    private final int[] sides = new int[FILTERS];
    private int defaultSide;
    private EjectSetting eject = EjectSetting.ITEM;

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
        return slot < FILTERS ? FILTER_BUTTON + slot : -1;
    }

    @Override
    public int slotLimit(int slot) {
        return slot < FILTERS ? 1 : 64;
    }

    @Override
    public int[] slots(Face side) {
        return INPUT_ONLY;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == INPUT;
    }

    /** TileFishSorter.canExtractItem: when ejecting is off, the input can be pulled from the face it would go to. */
    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == INPUT && side != null && side == faceFor(inv.get(INPUT));
    }

    /** The face a stack leaves through. */
    public Face faceFor(Stack stack) {
        int filter = filterFor(stack);
        return Face.values()[filter < 0 ? defaultSide : sides[filter]];
    }

    /** The first filter matching the stack, or -1. */
    public int filterFor(Stack stack) {
        if (stack.isEmpty()) return -1;
        for (int i = 0; i < FILTERS; i++) {
            Stack f = inv.get(i);
            if (!f.isEmpty() && matches(f, stack)) return i;
        }
        return -1;
    }

    static boolean matches(Stack filter, Stack stack) {
        if (LiveFish.isEgg(filter)) return LiveFish.isEgg(stack);
        if (LiveFish.isLiveFish(filter)) {
            if (!LiveFish.isLiveFish(stack)) return false;
            Object fu = filter.data().get("species"), fl = lower(filter), su = stack.data().get("species"), sl = lower(stack);
            return (fu.equals(su) && fl.equals(sl)) || (fu.equals(sl) && fl.equals(su));
        }
        return filter.item().equals(stack.item());
    }

    private static Object lower(Stack s) {
        Object l = s.data().get("lower_species");
        return l != null ? l : s.data().get("species");
    }

    @Override
    public void tick(MachineWorld world) {
        Stack s = inv.get(INPUT);
        if (s.isEmpty() || !eject.ejectsItems()) return;
        Face face = faceFor(s);
        Stack rest = world.ejectTo(face, s);
        if (!rest.isEmpty()) world.dropAt(face, rest);
        inv.set(INPUT, Stack.EMPTY);
    }

    @Override
    public int[] gauges() {
        int[] g = new int[G_FILTER + FILTERS];
        g[G_DEFAULT_SIDE] = defaultSide;
        g[G_EJECT] = eject.ordinal();
        System.arraycopy(sides, 0, g, G_FILTER, FILTERS);
        return g;
    }

    @Override
    public void button(int id) {
        if (id == BUTTON_DEFAULT_SIDE) defaultSide = (defaultSide + 1) % 6;
        if (id == BUTTON_EJECT) eject = eject.toggle(EjectSetting.ITEM);
        if (id >= FILTER_BUTTON && id < FILTER_BUTTON + FILTERS) sides[id - FILTER_BUTTON] = (sides[id - FILTER_BUTTON] + 1) % 6;
    }

    @Override
    public void save(Map<String, Object> out) {
        out.put("default_side", defaultSide);
        out.put("eject", eject.ordinal());
        out.put("sides", Arrays.stream(sides).boxed().toList());
    }

    @Override
    public void load(Map<String, ?> in) {
        defaultSide = Math.floorMod(integer(in.get("default_side"), 0), 6);
        eject = in.containsKey("eject") ? EjectSetting.byOrdinal(integer(in.get("eject"), 1)) : EjectSetting.ITEM;
        Arrays.fill(sides, 0);
        if (in.get("sides") instanceof java.util.List<?> l) {
            for (int i = 0; i < Math.min(FILTERS, l.size()); i++) sides[i] = Math.floorMod(integer(l.get(i), 0), 6);
        }
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return slot < FILTERS ? SlotInfo.FILTER : SlotInfo.ANY;
    }
}
