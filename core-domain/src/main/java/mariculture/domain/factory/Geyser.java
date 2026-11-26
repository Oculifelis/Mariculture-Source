package mariculture.domain.factory;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.content.Face;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * Geyser (factory/tile/TileGeyser.java, 1.3.0): a jet of water that lifts entities or pushes them sideways.
 *
 * <p>Water reaches 8 blocks, pressurised water 16 (canUseFluid:31); the jet stops at the first solid block
 * (doSquirt:66). It works unless powered by redstone, checked once a second (updateEntity:52). Every 100 ticks it
 * uses 1 mB and pulls one item out of the inventory behind it, dropping it on itself (pullFromInventory:94).
 *
 * <p>Fixed: legacy only drew fluid from a tank behind it when that tank was not a block entity at all (the fluid
 * branch sat in the {@code else} of a null check, BUG-063); here it takes 1000 mB from a fluid handler behind it
 * when the tank has room.
 */
public final class Geyser extends Machine {
    public static final MachineType TYPE = new MachineType("geyser", Geyser::new, null);
    public static final int CAPACITY = 8000, WATER_REACH = 8, PRESSURISED_REACH = 16, USE_EVERY = 100, PULL = 1000;
    /** doSquirt: sideways jets add 0.1 per tick; upward jets add 0.25 while the entity rises slower than 0.24. */
    public static final double SIDE_PUSH = 0.1, LIFT = 0.25, LIFT_BELOW = 0.24, ITEM_LIFT = 0.015;
    private static final int[] NONE = {};
    private final Tank tank = new Tank(CAPACITY, f -> reach(f) > 0);
    private int tick;
    private boolean canWork;

    public Geyser() {
        tanks.add(tank);
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
    public boolean keepsContents() {
        return true;
    }

    /** How far the jet reaches with this fluid; 0 for fluids it cannot use. */
    public static int reach(String fluid) {
        if ("minecraft:water".equals(fluid)) return WATER_REACH;
        if ("mariculture:pressurised_water".equals(fluid)) return PRESSURISED_REACH;
        return 0;
    }

    public boolean working() {
        return canWork;
    }

    @Override
    public void tick(MachineWorld world) {
        tick++;
        if (tick % 20 == 0) canWork = !tank.isEmpty() && reach(tank.fluid()) > 0 && !world.poweredByRedstone();
        if (!canWork) return;
        squirt(world);
        if (tick % USE_EVERY == 0) {
            tank.drain(1, false);
            pull(world);
        }
    }

    /** The block offsets the jet passes through. */
    public static List<int[]> jet(MachineWorld world, Face dir, int reach) {
        int[] d = offset(dir);
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < reach; i++) {
            int[] o = {d[0] * i, d[1] * i, d[2] * i};
            if (i > 0 && !world.passable(o[0], o[1], o[2])) break;
            out.add(o);
        }
        return out;
    }

    private void squirt(MachineWorld world) {
        Face dir = world.facing();
        List<int[]> jet = jet(world, dir, reach(tank.fluid()));
        world.moveEntities(jet, m -> push(m, dir));
        if (tick % 4 == 0) {
            int[] d = offset(dir);
            for (int[] o : jet) world.particle("cloud", o[0] + 0.5 + d[0] * 0.3, o[1] + 0.5 + d[1] * 0.3, o[2] + 0.5 + d[2] * 0.3);
        }
    }

    /** doSquirt's push on one entity; null leaves it. */
    public static MachineWorld.EntityMotion push(MachineWorld.EntityMotion m, Face dir) {
        if (dir != Face.UP) {
            int[] d = offset(dir);
            return new MachineWorld.EntityMotion(m.x() + d[0] * SIDE_PUSH, m.y() + d[1] * SIDE_PUSH, m.z() + d[2] * SIDE_PUSH, m.item());
        }
        if (m.y() > LIFT_BELOW) return null;
        if (m.item()) return new MachineWorld.EntityMotion(0, m.y() + LIFT + ITEM_LIFT, 0, true);
        return new MachineWorld.EntityMotion(m.x(), m.y() + LIFT, m.z(), false);
    }

    private void pull(MachineWorld world) {
        Face back = opposite(world.facing());
        Stack taken = world.takeFrom(back, 64);
        if (!taken.isEmpty()) {
            world.drop(taken);
            return;
        }
        if (tank.amount() + PULL < tank.capacity()) {
            FluidAmount f = world.drainFrom(back, PULL, false);
            if (f != null && reach(f.fluid()) > 0 && tank.fill(f.fluid(), f.amount(), true) == f.amount()) {
                FluidAmount got = world.drainFrom(back, f.amount(), true);
                if (got != null) tank.fill(got.fluid(), got.amount(), false);
            }
        }
    }

    public static int[] offset(Face f) {
        return switch (f) {
            case DOWN -> new int[] {0, -1, 0};
            case UP -> new int[] {0, 1, 0};
            case NORTH -> new int[] {0, 0, -1};
            case SOUTH -> new int[] {0, 0, 1};
            case WEST -> new int[] {-1, 0, 0};
            case EAST -> new int[] {1, 0, 0};
        };
    }

    public static Face opposite(Face f) {
        return switch (f) {
            case DOWN -> Face.UP;
            case UP -> Face.DOWN;
            case NORTH -> Face.SOUTH;
            case SOUTH -> Face.NORTH;
            case WEST -> Face.EAST;
            case EAST -> Face.WEST;
        };
    }

    @Override
    public void save(java.util.Map<String, Object> out) {
        super.save(out);
        out.put("CanWork", canWork);
    }

    @Override
    public void load(java.util.Map<String, ?> in) {
        super.load(in);
        canWork = bool(in.get("CanWork"));
    }
}
