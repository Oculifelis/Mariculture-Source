package mariculture.domain.maritech;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mariculture.domain.content.Face;
import mariculture.domain.factory.Geyser;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Tank;

/**
 * Sluice and advanced sluice (maritech/tile/TileSluice.java, TileSluiceAdvanced.java, 1.3.0). Faces any of six ways.
 *
 * <p>Facing sideways it is a water turbine: every 200 ticks (advanced 150), with water behind it, it puts pressurised
 * water in the air block in front of it (without water behind it, it takes that block away), measures the water
 * column behind it and how far the pressurised water runs ahead (up to 16, generateHPWater:192). Every 5 ticks
 * (advanced 4) the rotor at the end of that run gets (height x distance x 25) &gt;&gt; 6 energy, advanced x4, and 50
 * wear (pushToGenerator:209, Machines.java:37,68,72).
 *
 * <p>Every 60 ticks it moves fluids (updateEntity:67): a fluid source block behind it goes into the fluid handler in
 * front of it (water with a column taller than 1 behind it as height x 100 mB of pressurised water, removeFluid:75);
 * fluid from the handler behind it is placed as a block in front of it (pullFromTank:154); and up to 1000 mB moves
 * from the handler behind it to the handler in front of it unless that is a sluice (switchTanks:161). The advanced
 * sluice does either the first (unpowered) or the second (redstone powered), the second filling the area in front of
 * it outward and downward to 33 blocks one block at a time (updateArea:322); it always moves fluid between tanks.
 *
 * <p>Changed: only fluid source blocks are taken (legacy also took flowing water, making 1000 mB from nothing); the
 * advanced sluice's off-by-default "laggy" area drain is not ported; breaking a sluice clears pressurised water beside
 * it, not all water (ExtensionMachine.onBlockBroken:81).
 */
public final class Sluice extends Machine {
    public static final MachineType TYPE = new MachineType("sluice", () -> new Sluice(false), null);
    public static final MachineType ADVANCED = new MachineType("advanced_sluice", () -> new Sluice(true), null);
    public static final String WATER = "minecraft:water", PRESSURISED = "mariculture:pressurised_water";
    public static final int TANK = 10000, MAX_DISTANCE = 16, MULTIPLIER = 25, DAMAGE = 50, MOVE_EVERY = 60, BLOCK = 1000;
    /** ADVANCED_SLUICE_RADIUS: the fill area grows this many times half (ExtendedSettings, updateArea:340). */
    public static final int FILL_STEPS = 66 / 2;
    private static final int[] NONE = {};

    private final boolean advanced;
    private int height;
    private int distance;
    private final Deque<int[]> fill = new ArrayDeque<>();

    private Sluice(boolean advanced) {
        this.advanced = advanced;
        tanks.add(new Tank(TANK));
    }

    @Override
    public MachineType type() {
        return advanced ? ADVANCED : TYPE;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public int[] slots(Face side) {
        return NONE;
    }

    public int height() {
        return height;
    }

    public int distance() {
        return distance;
    }

    /** getEnergyGenerated &gt;&gt; 6: what the rotor gets per push. */
    public int energyPerPush() {
        return (height * distance * MULTIPLIER * (advanced ? 4 : 1)) >> 6;
    }

    @Override
    public void tick(MachineWorld world) {
        long t = world.gameTime();
        Face f = world.facing();
        if (!f.isVertical()) {
            if (t % (advanced ? 4 : 5) == 0) push(world, f);
            if (t % (advanced ? 150 : 200) == 0) pressurise(world, f);
        }
        if (t % MOVE_EVERY == 0) {
            if (!advanced) {
                placeInTank(world, f);
                pullFromTank(world, f);
            } else if (!world.poweredByRedstone()) {
                placeInTank(world, f);
            } else {
                fillArea(world, f);
            }
            switchTanks(world, f);
        }
    }

    private static boolean isWater(MachineWorld world, int x, int y, int z) {
        return WATER.equals(world.fluid(x, y, z));
    }

    private static boolean isPressurised(MachineWorld world, int x, int y, int z) {
        return PRESSURISED.equals(world.fluid(x, y, z));
    }

    /** generateHPWater. */
    void pressurise(MachineWorld world, Face f) {
        int[] d = Geyser.offset(f);
        if (isWater(world, -d[0], 0, -d[2])) {
            if (world.isAir(d[0], 0, d[2])) world.placeFluid(d[0], 0, d[2], PRESSURISED);
        } else if (isPressurised(world, d[0], 0, d[2])) {
            world.setBlock(d[0], 0, d[2], "minecraft:air");
        }
        for (height = 0; height < 256 && isWater(world, -d[0], height, -d[2]); height++) {}
        if (isPressurised(world, d[0], 0, d[2]) && height > 0) {
            for (distance = 1; distance < MAX_DISTANCE && isPressurised(world, d[0] * distance, 0, d[2] * distance); distance++) {}
        } else {
            distance = 0;
        }
        slotChanged(-1);
    }

    /** pushToGenerator. */
    private void push(MachineWorld world, Face f) {
        if (distance <= 0) return;
        int[] d = Geyser.offset(f);
        Rotor.power(world, d[0] * distance, 0, d[2] * distance, energyPerPush(), DAMAGE);
    }

    /** removeFluid: the source block behind goes into the handler in front. */
    private void placeInTank(MachineWorld world, Face f) {
        int[] d = Geyser.offset(f);
        int x = -d[0], y = -d[1], z = -d[2];
        String fluid = world.fluid(x, y, z);
        if (fluid.isEmpty() || !world.isFluidSource(x, y, z)) return;
        FluidAmount take = WATER.equals(fluid) && height > 1 ? new FluidAmount(PRESSURISED, height * 100) : new FluidAmount(fluid, BLOCK);
        if (world.fillTo(f, take.fluid(), take.amount(), false) < take.amount()) return;
        world.setBlock(x, y, z, "minecraft:air");
        world.fillTo(f, take.fluid(), take.amount(), true);
    }

    /** pullFromTank: fluid from the handler behind becomes a block in front. */
    private void pullFromTank(MachineWorld world, Face f) {
        int[] d = Geyser.offset(f);
        place(world, Geyser.opposite(f), d[0], d[1], d[2]);
    }

    private static boolean replaceable(MachineWorld world, int x, int y, int z) {
        return world.isAir(x, y, z) || !world.fluid(x, y, z).isEmpty() && !world.isFluidSource(x, y, z);
    }

    private static boolean place(MachineWorld world, Face from, int x, int y, int z) {
        FluidAmount f = world.drainFrom(from, BLOCK, false);
        if (f == null || f.isEmpty() || f.amount() < BLOCK || !replaceable(world, x, y, z)) return false;
        if (!world.placeFluid(x, y, z, f.fluid())) return false;
        world.drainFrom(from, BLOCK, true);
        return true;
    }

    /** Advanced pullFromTank: the next block of the fill area (updateArea(false), getNextCoordinates). */
    private void fillArea(MachineWorld world, Face f) {
        if (fill.isEmpty()) {
            int[] d = Geyser.offset(f);
            if (!replaceable(world, d[0], d[1], d[2])) return;
            Set<List<Integer>> seen = new LinkedHashSet<>();
            List<List<Integer>> frontier = List.of(List.of(d[0], d[1], d[2]));
            seen.addAll(frontier);
            for (int step = 0; step < FILL_STEPS && !frontier.isEmpty(); step++) {
                List<List<Integer>> grow = new ArrayList<>();
                for (List<Integer> c : frontier) {
                    for (int[] n : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}}) {
                        List<Integer> p = List.of(c.get(0) + n[0], c.get(1) + n[1], c.get(2) + n[2]);
                        if (!seen.contains(p) && replaceable(world, p.get(0), p.get(1), p.get(2))) {
                            seen.add(p);
                            grow.add(p);
                        }
                    }
                }
                frontier = grow;
            }
            for (List<Integer> c : seen) fill.addLast(new int[] {c.get(0), c.get(1), c.get(2)});
        }
        int[] c = fill.pollLast();
        if (c != null) place(world, Geyser.opposite(f), c[0], c[1], c[2]);
    }

    /** switchTanks: up to 1000 mB from behind to in front, never into another sluice. */
    private static void switchTanks(MachineWorld world, Face f) {
        int[] d = Geyser.offset(f);
        if (world.machine(d[0], d[1], d[2]) instanceof Sluice) return;
        FluidAmount from = world.drainFrom(Geyser.opposite(f), BLOCK, false);
        if (from == null || from.isEmpty()) return;
        int fits = world.fillTo(f, from.fluid(), from.amount(), false);
        if (fits <= 0) return;
        FluidAmount moved = world.drainFrom(Geyser.opposite(f), fits, true);
        if (moved != null && !moved.isEmpty()) world.fillTo(f, moved.fluid(), moved.amount(), true);
    }

    @Override
    public void removed(MachineWorld world) {
        if (advanced) return;
        for (Face f : List.of(Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST)) {
            int[] d = Geyser.offset(f);
            if (isPressurised(world, d[0], 0, d[2])) world.setBlock(d[0], 0, d[2], "minecraft:air");
        }
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("Height", height);
        out.put("Distance", distance);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        height = Math.max(0, integer(in.get("Height"), 0));
        distance = Math.max(0, Math.min(MAX_DISTANCE, integer(in.get("Distance"), 0)));
    }

    @Override
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        super.describe(out);
        if (distance > 0) out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.sluice", height, distance, energyPerPush()));
    }
}
