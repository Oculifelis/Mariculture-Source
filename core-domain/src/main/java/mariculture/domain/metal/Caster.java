package mariculture.domain.metal;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * Ingot, nugget and block casters (core/tile/TileCooling and its three subclasses, 1.3.0). The tank takes any fluid;
 * while unpowered, with an empty slot and enough of a castable fluid, the freeze counter grows by the cooling speed
 * ((60 - biome temperature) / 5, at least 1) each tick, and on reaching the cast time every empty slot is filled
 * while the fluid lasts. Filling the tank restarts the counter. Slots hold one item; automation only takes out;
 * right-clicking the block hands the casts to the player (BlockRenderedMachine).
 */
public final class Caster extends Machine {
    public static final MachineType INGOT = type("ingot_caster", Casting.Kind.INGOT, 4, MetalRates.INGOT * 4, 800);
    public static final MachineType NUGGET = type("nugget_caster", Casting.Kind.NUGGET, 16, MetalRates.NUGGET * 16, 600);
    public static final MachineType BLOCK = type("block_caster", Casting.Kind.BLOCK, 1, MetalRates.BLOCK + MetalRates.INGOT, 1200);
    public static final int G_FREEZE = 0, G_TIME = 1;

    private final MachineType type;
    private final Casting.Kind kind;
    private final int slots;
    private final int time;
    private final Tank tank;
    private final int[] all;
    private int freezeTick;
    private int cooling;
    private boolean canWork;
    private boolean dirty = true;

    private static MachineType type(String id, Casting.Kind kind, int slots, int capacity, int time) {
        MachineType[] self = new MachineType[1];
        self[0] = new MachineType(id, () -> new Caster(self[0], kind, slots, capacity, time), null);
        return self[0];
    }

    private Caster(MachineType type, Casting.Kind kind, int slots, int capacity, int time) {
        this.type = type;
        this.kind = kind;
        this.slots = slots;
        this.time = time;
        this.tank = new Tank(capacity);
        this.all = new int[slots];
        for (int i = 0; i < slots; i++) all[i] = i;
        tanks.add(tank);
    }

    @Override
    public MachineType type() {
        return type;
    }

    @Override
    public int slotCount() {
        return slots;
    }

    @Override
    public int slotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return false;
    }

    @Override
    public int[] slots(Face side) {
        return all;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return true;
    }

    @Override
    public void slotChanged(int slot) {
        if (slot < 0) freezeTick = 0; // the tank changed (legacy fill restarts the freeze)
        dirty = true;
    }

    private Casting.Result result(MachineWorld world) {
        return Casting.result(kind, tank.fluid(), tank.amount(), world::firstItemInTag);
    }

    private boolean hasRoom() {
        for (int i = 0; i < slots; i++) if (inv.get(i).isEmpty()) return true;
        return false;
    }

    @Override
    public void tick(MachineWorld world) {
        if (cooling <= 0) cooling = Math.max(1, (60 - world.biomeTemperature()) / 5);
        // Legacy re-checked on every change plus a "kick up the butt" every 1000 ticks; this checks on change and each second.
        if (dirty || world.gameTime() % 20 == 0) {
            dirty = false;
            canWork = !world.poweredByRedstone() && hasRoom() && result(world) != null;
        }
        if (!canWork) return;
        freezeTick += cooling;
        if (freezeTick < time) return;
        for (int i = 0; i < slots; i++) {
            Casting.Result r = result(world);
            if (r == null) break;
            if (!inv.get(i).isEmpty()) continue;
            tank.drain(r.amount(), false);
            inv.set(i, Stack.of(r.item()));
        }
        freezeTick = 0;
        dirty = true;
    }

    @Override
    public boolean usesClick() {
        return true;
    }

    /** Right-click: every cast goes to the player. */
    @Override
    public java.util.List<Stack> onUse() {
        java.util.List<Stack> out = new java.util.ArrayList<>();
        for (int i = 0; i < slots; i++) {
            Stack s = inv.get(i);
            if (s.isEmpty()) continue;
            out.add(s);
            inv.set(i, Stack.EMPTY);
        }
        dirty = true;
        return out;
    }

    @Override
    public int[] gauges() {
        return new int[] {freezeTick, time};
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("freeze_tick", freezeTick);
        out.put("cooling", cooling);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        freezeTick = integer(in.get("freeze_tick"), 0);
        cooling = integer(in.get("cooling"), 0);
        dirty = true;
    }
}
