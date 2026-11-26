package mariculture.domain.metal;

import mariculture.domain.machine.SlotInfo;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.ItemTags;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;
import mariculture.domain.machine.Tank;

/**
 * Crucible furnace (core/tile/TileCrucible, CrucibleFuelHandler, 1.3.0). Fuel in slot 7, or a fuel fluid drained
 * 10 mB at a time from the block below, heats it up to 25000 internal units (2000 degrees); it cools by
 * (60 - biome temperature) / 5 every second. Two input slots melt into the tank once the temperature passes their
 * melting point; progress gains 100 + heat upgrades * 25 * a modifier that grows with the spare temperature, and
 * both slots melt when it reaches 40000. Bonus items go to slot 8. Molten fluid is pushed out of every side but the
 * bottom in whole units (5000, block, 1000, ore, ingot, nugget, 1 mB).
 *
 * <p>Changes: one block instead of the two-high multiblock (as the sifter, FISH-014). The fluid eject interval was
 * 100 - 5 * speed ticks and divided by zero from 20 speed (BUG-055): it is at least 1 tick. With an ethereal upgrade,
 * enchanted items melt into liquid experience ({@link Ethereal}); the disenchanted item goes into the output slot,
 * which must be empty (legacy could also eject it, but an ejected stack would lose its damage and name here, so it
 * waits in the slot for a hopper or a player).
 */
public final class Crucible extends StandardMachine {
    public static final int MAX_TEMP = 25000;
    public static final int MAX = 40000;
    public static final int FLUID_IN = 3, FLUID_OUT = 4, IN_A = 5, IN_B = 6, FUEL = 7, OUT = 8, SLOTS = 9;
    private static final int[] INPUTS = {IN_A, IN_B};
    private static final int[] OUTPUTS = {OUT};
    private static final int[] SIDED = {FLUID_IN, FLUID_OUT, IN_A, IN_B, FUEL, OUT};
    private static final int[] EJECT_AMOUNTS = {5000, MetalRates.BLOCK, 1000, MetalRates.ORE, MetalRates.INGOT, MetalRates.NUGGET, 1};
    public static final int G_TEMP = G_EXTRA, G_BURN = G_EXTRA + 1;

    public static final MachineType TYPE = new MachineType("crucible", Crucible::new, Layout.builder(176)
            .tank(0, 98, 19, 34, 54)
            .slot(IN_A, 44, 19).slot(IN_B, 64, 19).slot(FUEL, 54, 55).slot(OUT, 72, 55)
            .slot(FLUID_IN, 142, 19).slot(FLUID_OUT, 142, 55)
            .progress(G_PROGRESS, G_MAX, 46, 39, 34, 4, "right", 0xFFFFAA33)
            .progressFixed(G_BURN, 12, 37, 55, 4, 16, "up", 0xFFFF5522)
            .label("gui.mariculture.crucible.temperature", G_TEMP, 8, 78)
            .standardSidePanel(true, true)
            .build());

    private final Tank tank = new Tank(20000);
    private int temp;
    private int cooling;
    private double meltingModifier = 1.0;
    private final FluidAmount[] pending = new FluidAmount[2];
    private final boolean[] ethereal = new boolean[2];
    /** The burning fuel (null when none), its ticks and the heat it gave so far. */
    private CrucibleFuels.Fuel fuel;
    private int fuelTick;
    private int usedHeat;

    public Crucible() {
        tanks.add(tank);
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
        return mariculture.domain.config.Settings.CRUCIBLE_SPEED.get();
    }

    @Override
    protected int[] outputs() {
        return OUTPUTS;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM_AND_FLUID;
    }

    /** Legacy getTankCapacity: 20 buckets plus one per storage level. */
    @Override
    protected void upgradesChanged() {
        tank.setCapacity(20000 + upgrades.storage() * 1000);
    }

    public int temperature() {
        return temp * 2000 / MAX_TEMP;
    }

    private int heat() {
        return upgrades.temperature();
    }

    private int legacySpeed() {
        return upgrades.speed() - 1;
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        if (slot == FUEL) return isFuel(stack);
        if (slot == FLUID_IN) return !isFuel(stack) && !LiveFish.isLiveFish(stack) && !inputCandidate(stack);
        return slot == IN_A || slot == IN_B;
    }

    private boolean inputCandidate(Stack s) {
        return !s.isEmpty() && Melting.canMelt(s.item(), t -> ItemTags.has(s.item(), t), LiveFish.registry());
    }

    private static boolean isFuel(Stack s) {
        return !s.isEmpty() && CrucibleFuels.item(s.item(), t -> ItemTags.has(s.item(), t)) != null;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == OUT || slot == FLUID_OUT;
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        return temp > 0 && (!inv.get(IN_A).isEmpty() || !inv.get(IN_B).isEmpty()) && (canMelt(world, 0) | canMelt(world, 1));
    }

    @Override
    protected int progressStep() {
        return (int) (100 + (heat() * 25) * meltingModifier);
    }

    @Override
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        super.describe(out);
        out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.temperature", temperature()));
    }

    @Override
    protected void update(MachineWorld world) {
        heatUp(world);
        coolDown(world);
        drainContainers(world);
        int rate = Math.max(1, 100 - legacySpeed() * 5);
        if (world.gameTime() % rate == 0 && !tank.isEmpty() && mode.canWork(world.poweredByRedstone()) && eject.ejectsFluids()) {
            world.ejectFluidExact(tank, EJECT_AMOUNTS, Face.DOWN);
        }
    }

    /** TileCrucible.canMelt: a recipe at this temperature with room for its fluid and bonus item; sets the modifier. */
    private boolean canMelt(MachineWorld world, int i) {
        pending[i] = null;
        ethereal[i] = false;
        Stack in = inv.get(INPUTS[i]);
        if (canMeltEthereal(world, i)) return true;
        Melting.Recipe r = in.isEmpty() ? null : Melting.find(in.item(), temperature(), t -> world.itemHasTag(in.item(), t), LiveFish.registry());
        if (r == null) return false;
        FluidAmount f = Melting.fluid(r, world.random(), world::tagHasItems);
        if (f == null || tank.fill(f.fluid(), f.amount(), true) < f.amount()) return false;
        if (r.bonus() != null && !eject.ejectsItems()) {
            Stack out = inv.get(OUT);
            String bonus = bonusItem(world, r);
            if (!out.isEmpty() && (bonus == null || !out.is(bonus) || out.count() >= 64)) return false;
        }
        int realTemp = temperature();
        meltingModifier = 1.0 + Math.min(realTemp, 2000 - r.temp()) * 3.333333 / 2000;
        pending[i] = f;
        return true;
    }

    /** CrucibleHandler.getResult with ethereal set: checked before the melting table. */
    private boolean canMeltEthereal(MachineWorld world, int i) {
        if (!upgrades.has(Ethereal.UPGRADE) || temperature() < Ethereal.MELTING_POINT) return false;
        List<int[]> enchantments = inv.enchantments(INPUTS[i]);
        if (enchantments.isEmpty() || !inv.get(OUT).isEmpty()) return false;
        int xp = Ethereal.experience(enchantments, world.maxEnchantmentWeight());
        if (tank.fill(Fluids.XP, xp, true) < xp) return false;
        meltingModifier = 1.0 + Math.min(temperature(), 2000 - Ethereal.MELTING_POINT) * 3.333333 / 2000;
        pending[i] = new FluidAmount(Fluids.XP, xp);
        ethereal[i] = true;
        return true;
    }

    private static String bonusItem(MachineWorld world, Melting.Recipe r) {
        if (r.bonus() == null) return null;
        return r.bonus().startsWith("#") ? world.firstItemInTag(r.bonus().substring(1)) : r.bonus();
    }

    @Override
    protected void process(MachineWorld world) {
        for (int i = 0; i < 2; i++) {
            if (!canMelt(world, i)) continue;
            if (ethereal[i]) {
                if (inv.moveDisenchanted(INPUTS[i], OUT)) tank.fill(pending[i].fluid(), pending[i].amount(), false);
                continue;
            }
            Stack in = inv.get(INPUTS[i]);
            Melting.Recipe r = Melting.find(in.item(), temperature(), t -> world.itemHasTag(in.item(), t), LiveFish.registry());
            FluidAmount f = pending[i];
            inv.shrink(INPUTS[i], 1);
            tank.fill(f.fluid(), f.amount(), false);
            if (r.bonus() != null && r.chance() > 0 && mariculture.domain.config.Settings.CRUCIBLE_DUSTS.get() && world.random().nextInt(r.chance()) == 0) {
                String bonus = bonusItem(world, r);
                if (bonus != null) insertOutput(world, Stack.of(bonus));
            }
        }
    }

    // ---- fuel ----------------------------------------------------------------------------

    private boolean canFuel(MachineWorld world) {
        if (fuel != null || !mode.canWork(world.poweredByRedstone())) return false;
        Stack f = inv.get(FUEL);
        if (!f.isEmpty() && CrucibleFuels.item(f.item(), t -> world.itemHasTag(f.item(), t)) != null) return true;
        FluidAmount below = world.drainFrom(Face.DOWN, 10, false);
        return below != null && CrucibleFuels.fluid(below.fluid()) != null;
    }

    /** TileCrucible.getInfo: uses one fuel item, or drains 10 mB of fuel fluid from below. */
    private CrucibleFuels.Fuel takeFuel(MachineWorld world) {
        Stack f = inv.get(FUEL);
        CrucibleFuels.Fuel info = f.isEmpty() ? null : CrucibleFuels.item(f.item(), t -> world.itemHasTag(f.item(), t));
        if (info != null) {
            inv.shrink(FUEL, 1);
            return info;
        }
        FluidAmount below = world.drainFrom(Face.DOWN, 10, true);
        return below == null ? null : CrucibleFuels.fluid(below.fluid());
    }

    private void setFuel(CrucibleFuels.Fuel f) {
        fuel = f;
        fuelTick = 0;
        usedHeat = 0;
    }

    private void heatUp(MachineWorld world) {
        if (world.gameTime() % 20 == 0 && canFuel(world)) setFuel(takeFuel(world));
        if (fuel == null) return;
        int realUsed = usedHeat * 2000 / MAX_TEMP;
        int maxPer = fuel.maxTempPer() * (upgrades.purity() + 1);
        fuelTick++;
        if (realUsed < maxPer && temperature() < fuel.maxTemp()) {
            int jump = heat() / 3 + 1;
            // CrucibleExplosionTickHandler: 1 in n, a block-breaking explosion when a player is near, and 1000x heat.
            if (fuel.explosive() && world.random().nextInt(fuel.explosionChance()) == 0) {
                if (world.playerNearby(128)) world.explode(fuel.explosionRadius());
                jump *= 1000;
            }
            temp += jump;
            usedHeat += jump;
        }
        if (temp > MAX_TEMP) temp = MAX_TEMP;
        if (realUsed >= maxPer || fuelTick >= fuel.ticksPer()) {
            fuel = null;
            setFuel(canFuel(world) ? takeFuel(world) : null);
        }
    }

    private void coolDown(MachineWorld world) {
        if (cooling <= 0) cooling = Math.max(1, (60 - world.biomeTemperature()) / 5);
        if (world.gameTime() % 20 == 0) temp = Math.max(0, temp - cooling);
    }

    private void drainContainers(MachineWorld world) {
        Stack in = inv.get(FLUID_IN);
        if (in.isEmpty() || world.gameTime() % 20 != 0 || !inv.get(FLUID_OUT).isEmpty()) return;
        MachineWorld.ContainerResult r = world.processContainer(in, tank);
        if (r != null) {
            inv.shrink(FLUID_IN, 1);
            Stack left = inv.insert(r.container(), new int[] {FLUID_OUT}, false);
            if (!left.isEmpty()) world.drop(left);
            markDirty();
        }
    }

    @Override
    public int[] gauges() {
        int burn = fuel == null ? 0 : Math.max(0, 12 - fuelTick * 12 / Math.max(1, fuel.ticksPer()));
        return gauges(temperature(), burn);
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("temperature", temp);
        out.put("cooling", cooling);
        out.put("melting_modifier", (int) Math.round(meltingModifier * 1000));
        if (fuel != null) {
            out.put("fuel", java.util.List.of(fuel.maxTemp(), fuel.maxTempPer(), fuel.ticksPer(), Math.round(fuel.explosionRadius()), fuel.explosionChance()));
            out.put("fuel_tick", fuelTick);
            out.put("used_heat", usedHeat);
        }
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        temp = integer(in.get("temperature"), 0);
        cooling = integer(in.get("cooling"), 0);
        meltingModifier = integer(in.get("melting_modifier"), 1000) / 1000.0;
        fuel = null;
        if (in.get("fuel") instanceof java.util.List<?> l && l.size() == 5) {
            fuel = new CrucibleFuels.Fuel(integer(l.get(0), 0), integer(l.get(1), 0), integer(l.get(2), 1), integer(l.get(3), 0), integer(l.get(4), 0));
            fuelTick = integer(in.get("fuel_tick"), 0);
            usedHeat = integer(in.get("used_heat"), 0);
        }
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == FLUID_IN) return SlotInfo.fluidIn();
        if (slot == FLUID_OUT) return SlotInfo.OUTPUT;
        if (slot == IN_A || slot == IN_B) {
            java.util.LinkedHashSet<String> g = new java.util.LinkedHashSet<>();
            for (Melting.Recipe r : Melting.table()) g.add(r.input());
            return SlotInfo.input("melting", g, this::inputCandidate);
        }
        if (slot == FUEL) return SlotInfo.of(SlotInfo.Role.FUEL, "fuel", CrucibleFuels.items().keySet(), Crucible::isFuel);
        return super.info(slot);
    }
}
