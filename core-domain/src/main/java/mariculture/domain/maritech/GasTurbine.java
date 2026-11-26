package mariculture.domain.maritech;

import java.util.List;

import mariculture.domain.content.Face;
import mariculture.domain.factory.Geyser;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Tank;

/**
 * Gas turbine tank (factory/tile/TileGasTank.java, 1.3.0): holds 512000 mB of natural gas and keeps it when broken.
 * With gas and a rotor beside it (north, south, west, east, the first found) whose plane, across the line from the
 * tank, is 8 air round the rotor framed by 16 full blocks, every 5 ticks it gives the rotor 5000 energy and 50 wear
 * and uses 1 mB (updateEntity:39, updateRotor:66, pushToRotor:119, Machines.java:73-74). It looks for the rotor every
 * 200 ticks until it has one and again with every 1000 mB used.
 *
 * <p>Changed: legacy only ran with MariTech loaded (canUpdate: HIGH_TECH_ENABLED, MT-027); MariTech is always part of
 * Mariculture here.
 */
public final class GasTurbine extends Machine {
    public static final MachineType TYPE = new MachineType("gas_turbine_tank", GasTurbine::new, null);
    public static final int CAPACITY = 512000, POWER = 5000, DAMAGE = 50, EVERY = 5, LOOK_EVERY = 200, RECHECK_MB = 1000;
    private static final int[] NONE = {};
    private final Tank tank = new Tank(CAPACITY, Fluids.NATURAL_GAS::equals);
    private Face rotor;

    public GasTurbine() {
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

    public boolean hasRotor() {
        return rotor != null;
    }

    @Override
    public void tick(MachineWorld world) {
        long t = world.gameTime();
        if (tank.isEmpty()) return;
        if (rotor == null) {
            if (t % LOOK_EVERY == 0) rotor = findRotor(world);
            return;
        }
        if (t % EVERY != 0) return;
        int[] d = Geyser.offset(rotor);
        if (Rotor.power(world, d[0], 0, d[2], POWER, DAMAGE) < 0) {
            rotor = null;
            return;
        }
        tank.drain(1, false);
        slotChanged(-1);
        if (tank.amount() % RECHECK_MB == 0) rotor = findRotor(world);
    }

    /** updateRotor: the first rotor beside the tank whose frame is right, or null. */
    public static Face findRotor(MachineWorld world) {
        for (Face f : List.of(Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST)) {
            int[] d = Geyser.offset(f);
            if (!(world.machine(d[0], 0, d[2]) instanceof Rotor)) continue;
            return framed(world, d[0], d[2], f) ? f : null;
        }
        return null;
    }

    /** 8 air round the rotor and 16 full blocks round those, in the plane across the tank-rotor line. */
    static boolean framed(MachineWorld world, int rx, int rz, Face f) {
        boolean alongX = f == Face.WEST || f == Face.EAST;
        int air = 0, solid = 0;
        for (int a = -2; a <= 2; a++) {
            for (int y = -2; y <= 2; y++) {
                int x = rx + (alongX ? 0 : a), z = rz + (alongX ? a : 0);
                if (Math.abs(a) <= 1 && Math.abs(y) <= 1) {
                    if (world.isAir(x, y, z)) air++;
                } else if (world.isSolidCube(x, y, z)) {
                    solid++;
                }
            }
        }
        return air == 8 && solid == 16;
    }

    @Override
    public void load(java.util.Map<String, ?> in) {
        super.load(in);
        rotor = null;
    }
}
