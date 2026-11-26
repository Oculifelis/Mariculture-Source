package mariculture.domain.metal;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * Vat (core/tile/TileVat, 1.3.0, single-block form): two input tanks, an output tank (6000 mB each), an input and an
 * output slot. A recipe is looked up with tank 1 as the first fluid, then tank 2; while one matches, a step counter
 * advances every 30 ticks, and when it reaches the recipe time the recipe runs up to 64 times at once. Fluid fills
 * tank 1 unless tank 2 holds that fluid, else tank 2; draining takes the output tank first, then tank 2, then tank 1.
 *
 * <p>Changes: no 2x2 multiblock (30000 mB tanks, a step every 20 ticks); the output tank's room check compared fluid
 * objects by reference, so a recipe making fluid never ran while the output tank held any (BUG-056): it now accepts the
 * same fluid when it fits.
 */
public final class Vat extends Machine {
    public static final int IN = 0, OUT = 1, CAPACITY = 6000, STEP_TICKS = 30;
    public static final int G_TIME = 0, G_NEEDED = 1;
    private static final int[] SLOTS = {IN, OUT};

    public static final MachineType TYPE = new MachineType("vat", Vat::new, Layout.builder(166)
            .tank(0, 12, 18, 18, 54).tank(1, 34, 18, 18, 54).tank(2, 132, 18, 30, 54)
            .slot(IN, 66, 26).slot(OUT, 104, 44)
            .progress(G_TIME, G_NEEDED, 64, 48, 34, 4, "right", 0xFFAA66FF)
            .build());

    private final Tank tank1 = new Tank(CAPACITY), tank2 = new Tank(CAPACITY), tank3 = new Tank(CAPACITY);
    private int timeNeeded;
    private int timeRemaining;
    private boolean canWork;
    private boolean dirty = true;

    public Vat() {
        tanks.add(tank1);
        tanks.add(tank2);
        tanks.add(tank3);
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 2;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return slot == IN;
    }

    @Override
    public int[] slots(Face side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == IN;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == OUT || side != Face.DOWN;
    }

    @Override
    public boolean canFill(int tank, Face side, String fluid) {
        if (tank == 0) return !tank2.contains(fluid);
        if (tank == 1) return !tank1.contains(fluid);
        return false;
    }

    @Override
    public boolean canDrain(int tank, Face side) {
        if (tank == 2) return true;
        if (tank == 1) return tank3.isEmpty();
        return tank3.isEmpty() && tank2.isEmpty();
    }

    @Override
    public void slotChanged(int slot) {
        dirty = true;
    }

    private record Found(VatRecipes.Recipe recipe, boolean swapped) {}

    private Found find(MachineWorld world) {
        Stack item = inv.get(IN);
        VatRecipes.Recipe r = VatRecipes.find(tank1.fluid(), tank1.amount(), tank2.fluid(), tank2.amount(), item, t -> world.itemHasTag(item.item(), t));
        if (r != null && hasRoom(r)) return new Found(r, false);
        r = VatRecipes.find(tank2.fluid(), tank2.amount(), tank1.fluid(), tank1.amount(), item, t -> world.itemHasTag(item.item(), t));
        if (r != null && hasRoom(r)) return new Found(r, true);
        return null;
    }

    private boolean hasRoom(VatRecipes.Recipe r) {
        FluidAmount f = r.outFluid();
        if (f != null && !tank3.isEmpty() && (!tank3.contains(f.fluid()) || tank3.space() < f.amount())) return false;
        Stack out = inv.get(OUT);
        Stack made = r.outItem();
        return made == null || out.isEmpty() || (out.item().equals(made.item()) && out.data().equals(made.data()) && out.count() + made.count() <= 64);
    }

    @Override
    public void tick(MachineWorld world) {
        if (dirty || world.gameTime() % 20 == 0) {
            dirty = false;
            canWork = !(tank1.isEmpty() && tank2.isEmpty()) && find(world) != null;
        }
        if (!canWork) return;
        if (timeNeeded == 0) {
            Found f = find(world);
            if (f != null) timeNeeded = f.recipe().time();
            timeRemaining = 0;
        }
        if (world.gameTime() % STEP_TICKS == 0) timeRemaining++;
        if (timeRemaining >= timeNeeded) {
            for (int i = 0; i < 64; i++) {
                Found f = find(world);
                if (f == null) break;
                run(f);
            }
            timeRemaining = 0;
            timeNeeded = 0;
            dirty = true;
        }
    }

    /** TileVat.createResult. A fluid that is both an input and the output is only drained by the difference. */
    private void run(Found f) {
        VatRecipes.Recipe r = f.recipe();
        FluidAmount out = r.outFluid();
        Tank first = f.swapped() ? tank2 : tank1, second = f.swapped() ? tank1 : tank2;
        out = drain(first, r.fluid1(), out);
        if (r.fluid2() != null) out = drain(second, r.fluid2(), out);
        if (r.item() != null) inv.shrink(IN, r.itemCount());
        if (out != null && out.amount() > 0) tank3.fill(r.outFluid().fluid(), r.outFluid().amount(), false);
        if (r.outItem() != null) {
            Stack o = inv.get(OUT);
            inv.set(OUT, o.isEmpty() ? r.outItem() : o.withCount(o.count() + r.outItem().count()));
        }
    }

    private static FluidAmount drain(Tank tank, FluidAmount input, FluidAmount output) {
        int amount = input.amount();
        if (output != null && VatRecipes.same(input.fluid(), output.fluid())) {
            amount -= output.amount();
            output = null;
        }
        tank.drain(Math.max(0, amount), false);
        return output;
    }

    @Override
    public int[] gauges() {
        return new int[] {timeRemaining, timeNeeded};
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("time_needed", timeNeeded);
        out.put("time_remaining", timeRemaining);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        timeNeeded = integer(in.get("time_needed"), 0);
        timeRemaining = integer(in.get("time_remaining"), 0);
        dirty = true;
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        if (slot != IN) return SlotInfo.OUTPUT;
        java.util.LinkedHashSet<String> g = new java.util.LinkedHashSet<>();
        for (VatRecipes.Recipe r : VatRecipes.all()) if (r.item() != null) g.add(r.item());
        return SlotInfo.input("vat", g, s -> {
            for (VatRecipes.Recipe r : VatRecipes.all()) if (r.item() != null && r.itemMatches(s, t -> mariculture.domain.machine.ItemTags.has(s.item(), t))) return true;
            return false;
        });
    }
}
