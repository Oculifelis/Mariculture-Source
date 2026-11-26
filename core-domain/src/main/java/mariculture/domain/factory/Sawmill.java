package mariculture.domain.factory;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Face;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;

/**
 * The sawmill (factory/tile/TileSawmill.java, 1.3.0): six blocks, one for each face, and a selected plan make
 * (plan size x {@code machines.sawmill_output_multiplier}, doubled by the ethereal upgrade) sawmill blocks of the
 * plan's shape that wear those faces. A feather leaves its face see-through. Work per batch is
 * {@code machines.sawmill_speed} / 100; the plan is kept, each block is used up. Each face's slot fills from that
 * side of the machine; the output comes out of every side.
 *
 * <p>Changed: the plan is picked with a button that steps through the three plan slots (legacy right-clicked the slot).
 */
public final class Sawmill extends StandardMachine {
    public static final int PLAN = 3, TOP = 6, NORTH = 7, SOUTH = 8, WEST = 9, EAST = 10, BOTTOM = 11, OUT = 12, SLOTS = 13;
    public static final int BUTTON_PLAN = 2;
    public static final String FEATHER = "minecraft:feather";
    private static final int[] OUTPUTS = {OUT};

    public static final MachineType TYPE = new MachineType("sawmill", Sawmill::new, Layout.builder(176)
            .slot(PLAN, 11, 20).slot(PLAN + 1, 11, 40).slot(PLAN + 2, 11, 60)
            .button(BUTTON_PLAN, 29, 39, 14, 18, "gui.mariculture.plan", G_EXTRA)
            .slot(TOP, 61, 22).slot(NORTH, 43, 40).slot(EAST, 61, 40).slot(SOUTH, 79, 40).slot(WEST, 97, 40).slot(BOTTOM, 61, 58)
            .progress(G_PROGRESS, G_MAX, 119, 39, 24, 16, "right", 0xFF8B5A2B)
            .slot(OUT, 149, 40)
            .standardSidePanel(true, true)
            .build());

    private int selected = PLAN;

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
        return Math.max(1, Settings.SAWMILL_SPEED.get() / 100);
    }

    @Override
    protected int[] outputs() {
        return OUTPUTS;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM;
    }

    /** The material slot for each face, in {@link Face} order. */
    public static int slot(Face face) {
        return switch (face) {
            case DOWN -> BOTTOM;
            case UP -> TOP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public int selected() {
        return selected;
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot >= PLAN && slot < TOP) return Plans.isPlan(stack);
        if (slot >= TOP && slot < OUT) return !Plans.isPlan(stack) && !CustomBlocks.isCustom(stack.item());
        return super.validStack(slot, stack);
    }

    @Override
    public int[] slots(Face side) {
        return side == null ? super.slots(null) : new int[] {slot(side), OUT};
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return side != null && slot == slot(side) && isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == OUT;
    }

    /** A feather gives a see-through face; any other item must be a block. Null when a face cannot be cut. */
    private static String faceBlock(MachineWorld world, Stack s) {
        if (s.isEmpty()) return null;
        if (s.is(FEATHER)) return CustomFaces.AIR;
        return world.blockOfItem(s.item());
    }

    /** The batch the sawmill would make now, or EMPTY (getResult). */
    public Stack result(MachineWorld world) {
        Stack plan = inv.get(selected);
        if (!Plans.isPlan(plan)) return Stack.EMPTY;
        String[] faces = new String[6];
        for (Face f : Face.values()) {
            faces[f.ordinal()] = faceBlock(world, inv.get(slot(f)));
            if (faces[f.ordinal()] == null) return Stack.EMPTY;
        }
        Plans.Type type = Plans.type(plan);
        int count = type.size * Settings.SAWMILL_MULTIPLIER.get() * (upgrades.has("ethereal") ? 2 : 1);
        return CustomFaces.of(faces).onto(type.blockId(), count);
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        Stack r = result(world);
        if (r.isEmpty()) return false;
        if (eject.ejectsItems()) return true;
        Stack out = inv.get(OUT);
        return out.isEmpty() || out.sameItem(r) && out.count() + r.count() < 64;
    }

    @Override
    protected void process(MachineWorld world) {
        Stack r = result(world);
        if (r.isEmpty()) return;
        insertOutput(world, r);
        for (int i = TOP; i < OUT; i++) inv.shrink(i, 1);
    }

    @Override
    public void button(int id) {
        if (id == BUTTON_PLAN) selected = selected + 1 >= TOP ? PLAN : selected + 1;
        super.button(id);
    }

    @Override
    public int[] gauges() {
        return gauges(selected - PLAN + 1);
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("plan_selected", selected);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        int s = in.get("plan_selected") instanceof Number n ? n.intValue() : PLAN;
        selected = s >= PLAN && s < TOP ? s : PLAN;
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot >= PLAN && slot < TOP) {
            java.util.List<String> g = new java.util.ArrayList<>();
            for (Plans.Type t : Plans.Type.values()) g.add("mariculture:" + t.itemPath());
            return SlotInfo.input("plan", g, Plans::isPlan);
        }
        if (slot >= TOP && slot < OUT) return SlotInfo.input("block", java.util.List.of("minecraft:oak_planks", "minecraft:stone", "minecraft:bricks"), null);
        return super.info(slot);
    }
}
