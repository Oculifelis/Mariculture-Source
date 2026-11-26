package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.factory.Geyser;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.RedstoneMode;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;
import mariculture.domain.machine.Tank;

/**
 * FLUDD stand (maritech/tile/TileFLUDDStand.java, 1.3.0): a FLUDD used on a block. While powered by redstone (mode HIGH
 * by default) and holding pressurised water, it shoots a jet the way it faces, (purity + 1) x 8 blocks long, stopping at
 * the first block with a collision box (doSquirt:52). Entities in the jet lose their fall and are pushed along it by
 * speed x 0.025 a tick (sideways or down; upward jets lift by speed x 0.15/0.1/0.05/0.025 by speed band while the entity
 * rises slower than that minus 0.1). Heating upgrades scald living things in the jet (heat / 2 a tick); cooling upgrades
 * wear down the block beyond each jet block, breaking blocks up to -heat x 2 hardness at a chance of 1 in hardness x 10.
 * An ethereal upgrade reverses sideways jets and puts items one block out into the inventory behind it. Every 100 ticks
 * it uses distance / 8 + |heat| + speed (+10 ethereal) mB. Tank 20 + storage x 4 buckets; broken, it drops the FLUDD
 * holding its water (ExtensionRenderedMachine.onBlockBroken:126).
 *
 * <p>Fixed BUG-070: legacy broke blocks of negative hardness (bedrock) with cooling upgrades.
 */
public final class FluddStand extends StandardMachine {
    public static final int BUCKET = 1000, BASE = 20, PER_STORAGE = 4, IN = 3, OUT = 4, SLOTS = 5, USE_EVERY = 100;
    private static final int[] NONE = {};
    private static final int[] SIDED = {IN, OUT};

    public static final MachineType TYPE = new MachineType("fludd_stand", FluddStand::new, Layout.builder(176)
            .tank(0, 62, 17, 52, 54)
            .slot(IN, 26, 17).slot(OUT, 26, 53)
            .standardSidePanel(true, false)
            .build());

    private final Tank tank = new Tank(capacity(0), f -> f.equals(Fluids.PRESSURISED_WATER));

    public FluddStand() {
        tanks.add(tank);
        mode = RedstoneMode.HIGH;
    }

    public static int capacity(int storage) {
        return BUCKET * BASE + storage * BUCKET * PER_STORAGE;
    }

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
        return Integer.MAX_VALUE;
    }

    @Override
    protected int[] outputs() {
        return NONE;
    }

    @Override
    protected void upgradesChanged() {
        tank.setCapacity(Math.max(capacity(upgrades.storage()), tank.amount()));
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        return slot != OUT && super.validStack(slot, stack);
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == IN;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == OUT;
    }

    /** canMachineWork: pressurised water in the tank (the redstone mode is checked by the base). */
    @Override
    protected boolean canWork(MachineWorld world) {
        return tank.contains(Fluids.PRESSURISED_WATER) && tank.amount() > 0;
    }

    /** Progress never completes: the stand works every tick it can. */
    @Override
    protected void process(MachineWorld world) {}

    @Override
    protected int progressStep() {
        return 0;
    }

    public boolean working() {
        return canWork;
    }

    /** The water placed with a FLUDD (onTilePlaced:103). */
    public void placedFrom(Stack fludd) {
        tank.set(Fluids.PRESSURISED_WATER, Math.max(0, Fludd.water(fludd)));
        markDirty();
    }

    @Override
    public void removed(MachineWorld world) {
        world.drop(Fludd.of(tank.amount()));
    }

    public int distance() {
        return (upgrades.purity() + 1) * 8;
    }

    /** getWaterUsage:211. */
    public int usage() {
        int heat = upgrades.temperature();
        return distance() / 8 + Math.abs(heat) + (upgrades.has("ethereal") ? 10 : 0) + upgrades.speed();
    }

    public static double boostXZ(int speed) {
        return speed * 0.025;
    }

    public static double boostY(int speed) {
        double b = speed <= 4 ? 0.15 : speed <= 8 ? 0.1 : speed <= 12 ? 0.05 : 0.025;
        return speed * b;
    }

    @Override
    protected void update(MachineWorld world) {
        if (world.gameTime() % 20 == 0) container(world);
        if (!canWork) return;
        squirt(world);
        if (world.gameTime() % USE_EVERY == 0) {
            tank.drain(usage(), false);
            markDirty();
        }
    }

    private void container(MachineWorld world) {
        Stack in = inv.get(IN);
        if (in.isEmpty() || !inv.get(OUT).isEmpty()) return;
        MachineWorld.ContainerResult r = world.processContainer(in, tank);
        if (r == null) return;
        inv.shrink(IN, 1);
        Stack left = inv.insert(r.container(), new int[] {OUT}, false);
        if (!left.isEmpty()) world.drop(left);
        markDirty();
    }

    private void squirt(MachineWorld world) {
        Face dir = world.facing();
        int[] d = Geyser.offset(dir);
        int heat = upgrades.temperature(), speed = upgrades.speed();
        boolean reverse = upgrades.has("ethereal");
        List<int[]> jet = Geyser.jet(world, dir, distance());
        if (jet.isEmpty()) return;
        world.moveEntities(jet, m -> push(m, dir, speed, reverse));
        if (reverse && jet.size() > 1 && world.gameTime() % 20 == 0) {
            Face back = Geyser.opposite(dir);
            for (MachineWorld.ItemEntityView item : world.itemsAt(List.of(jet.get(1)))) {
                item.setStack(world.ejectTo(back, item.stack()));
            }
        }
        if (heat > 0 && heat / 2 > 0) {
            for (int[] o : jet) world.hurtLiving(new double[] {o[0], o[1], o[2], o[0] + 1, o[1] + 1, o[2] + 1}, heat / 2);
        }
        if (heat < 0) {
            float max = -heat * 2;
            for (int[] o : jet) {
                int x = o[0] + d[0], y = o[1] + d[1], z = o[2] + d[2];
                if (world.isAir(x, y, z)) continue;
                float hardness = world.hardness(x, y, z);
                if (hardness < 0) continue;
                int chance = hardness * 10 > 0 ? (int) (hardness * 10) : 10;
                if (world.random().nextInt(chance) == 0 && hardness <= max) world.breakBlock(x, y, z);
            }
        }
        if (world.gameTime() % 4 == 0) {
            for (int[] o : jet) world.particle("cloud", o[0] + 0.5 + d[0] * 0.3, o[1] + 0.5 + d[1] * 0.3, o[2] + 0.5 + d[2] * 0.3);
        }
    }

    /** doSquirt's push on one entity; null leaves it. */
    public static MachineWorld.EntityMotion push(MachineWorld.EntityMotion m, Face dir, int speed, boolean reverse) {
        if (dir != Face.UP) {
            int[] d = Geyser.offset(dir);
            double b = boostXZ(speed) * (reverse ? -1 : 1);
            return new MachineWorld.EntityMotion(m.x() + d[0] * b, m.y() + d[1] * b, m.z() + d[2] * b, m.item());
        }
        double lift = boostY(speed);
        if (m.y() > lift - 0.1) return null;
        if (m.item()) return new MachineWorld.EntityMotion(0, m.y() + lift + 0.015, 0, true);
        return new MachineWorld.EntityMotion(m.x(), m.y() + lift, m.z(), false);
    }

    @Override
    public void load(Map<String, ?> in) {
        tank.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        tank.setCapacity(Math.max(capacity(upgrades.storage()), tank.amount()));
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == IN) return SlotInfo.fluidIn();
        if (slot == OUT) return SlotInfo.OUTPUT;
        return super.info(slot);
    }
}
