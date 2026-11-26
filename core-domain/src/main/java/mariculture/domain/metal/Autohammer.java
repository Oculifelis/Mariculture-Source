package mariculture.domain.metal;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Autohammer (core/tile/TileAutohammer, 1.3.0). Four hammer slots, one per horizontal side (north, south, west, east;
 * right-clicking a side puts a held hammer in or takes it out). Every 50 ticks, while unpowered and while the fluid
 * handler below holds liquid experience, each hammer hits the anvil one block out and one down on its side; the
 * experience a hit uses is drained from below (100 mB of Mariculture XP per point, 20 mB for other mods' XP fluids)
 * and the hammer takes a point of damage.
 *
 * <p>Change: the autohammer works anvil recipes but does not repair tools (legacy lent a fake player the tank's
 * experience for repairs; repairs stay a player action).
 */
public final class Autohammer extends Machine {
    public static final String HAMMER = "mariculture:hammer";
    public static final int TICKS = 50;
    private static final Face[] SIDES = {Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST};
    private static final int[][] OFFSETS = {{0, -1, -1}, {0, -1, 1}, {-1, -1, 0}, {1, -1, 0}};
    private static final int[] NONE = {};

    public static final MachineType TYPE = new MachineType("autohammer", Autohammer::new, null);

    private int offset = -1;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 4;
    }

    @Override
    public int slotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return stack.is(HAMMER);
    }

    @Override
    public int[] slots(Face side) {
        return NONE;
    }

    /** The hammer slot for a clicked side, or -1 for top and bottom. */
    public static int slotFor(Face side) {
        for (int i = 0; i < SIDES.length; i++) if (SIDES[i] == side) return i;
        return -1;
    }

    /** Right-click on a side: take its hammer, or put the held hammer in. Returns the stack the player gets back. */
    public Stack use(Face side, Stack held) {
        int slot = slotFor(side);
        if (slot < 0) return null;
        Stack in = inv.get(slot);
        if (!in.isEmpty()) {
            inv.set(slot, Stack.EMPTY);
            return in;
        }
        if (held.is(HAMMER)) {
            inv.set(slot, held.withCount(1));
            return Stack.EMPTY;
        }
        return null;
    }

    /** XP points in a fluid (core/util/XPRegistry: Mariculture's 100 mB per point, OpenBlocks-style juice 20). */
    public static int mbPerPoint(String fluid) {
        if (fluid == null) return 0;
        if (fluid.equals("mariculture:xp")) return 100;
        String path = fluid.substring(fluid.indexOf(':') + 1);
        return path.contains("xp") || path.contains("experience") ? 20 : 0;
    }

    @Override
    public void tick(MachineWorld world) {
        if (world.poweredByRedstone()) return;
        if (offset < 0) offset = world.random().nextInt(80);
        if ((world.gameTime() + offset) % TICKS != 0) return;
        FluidAmount xp = world.drainFrom(Face.DOWN, Integer.MAX_VALUE, false);
        int per = xp == null ? 0 : mbPerPoint(xp.fluid());
        if (per == 0 || xp.amount() / per <= 0) return;
        for (int i = 0; i < 4; i++) {
            Stack hammer = inv.get(i);
            if (hammer.isEmpty()) continue;
            int[] o = OFFSETS[i];
            if (!(world.machine(o[0], o[1], o[2]) instanceof Anvil anvil)) continue;
            FluidAmount left = world.drainFrom(Face.DOWN, Integer.MAX_VALUE, false);
            int points = left == null || mbPerPoint(left.fluid()) == 0 ? 0 : left.amount() / mbPerPoint(left.fluid());
            Integer ret = anvil.work(n -> points > 0, world.random());
            if (ret == null || ret == 0) continue;
            int used = ret >= Anvil.FINISHED ? ret - Anvil.FINISHED : ret < 0 ? 1 : ret;
            if (used > 0) world.drainFrom(Face.DOWN, used * per, true);
            inv.set(i, world.damage(hammer, 1));
        }
    }

    @Override
    public void save(Map<String, Object> out) {
        out.put("offset", offset);
    }

    @Override
    public void load(Map<String, ?> in) {
        offset = integer(in.get("offset"), -1);
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return SlotInfo.of(SlotInfo.Role.CATALYST, "hammer", java.util.List.of(HAMMER), s -> s.is(HAMMER));
    }
}
