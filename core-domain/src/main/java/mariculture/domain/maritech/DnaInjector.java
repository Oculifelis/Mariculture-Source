package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import java.util.LinkedHashMap;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * DNA injector (maritech/tile/TileInjector.java, 1.3.0): with a live fish in slot 8, 5000 mB of blood and 5000 mB of
 * liquid flux (in either tank) and more than 10000 FE, each completion (DNA speed x 5) writes every DNA in slots 10-12
 * into both sides of one fish (process:150, beta: all three), wears each DNA by one, puts the fish out in slot 9 and
 * drains 5000 mB from both tanks. 250000 FE; usage (400 + (speed - 1) x 400) x the RF modifier; tanks
 * (1 + storage) x 5000 mB, filled from containers in slots 4 and 5 (empties to 6 and 7).
 *
 * <p>Changed: the tanks take blood and liquid flux only (Blood Magic's life essence arrives with that plugin).
 */
public final class DnaInjector extends PoweredMachine {
    public static final int FLUID = 5000, MIN_ENERGY = 10000;
    public static final int IN_1 = 4, IN_2 = 5, OUT_1 = 6, OUT_2 = 7, FISH = 8, OUT = 9, DNA = 10, SLOTS = 13;
    private static final int[] OUTPUTS = {OUT};
    private static final int[] SIDED = {IN_1, IN_2, OUT_1, OUT_2, FISH, OUT};

    public static final MachineType TYPE = new MachineType("dna_injector", DnaInjector::new, Layout.builder(176)
            .tank(0, 26, 17, 16, 54).tank(1, 48, 17, 16, 54)
            .slot(IN_1, 26, 72).slot(IN_2, 48, 72)
            .slot(OUT_1, 70, 17).slot(OUT_2, 70, 53)
            .grid(DNA, 3, 1, 94, 17)
            .slot(FISH, 94, 47)
            .progress(G_PROGRESS, G_MAX, 114, 51, 18, 8, "right", 0xFF4A90E2)
            .slot(OUT, 136, 47)
            .slot(BATTERY_SLOT, 8, 53)
            .progress(G_EXTRA, G_EXTRA + 1, 10, 17, 4, 32, "up", 0xFFCC2222)
            .standardSidePanel(true, true)
            .build());

    private final Tank tank1 = new Tank(FLUID, DnaInjector::accepts), tank2 = new Tank(FLUID, DnaInjector::accepts);

    public DnaInjector() {
        super(250000);
        tanks.add(tank1);
        tanks.add(tank2);
    }

    static boolean accepts(String fluid) {
        return fluid.equals(Fluids.BLOOD) || fluid.equals(Fluids.FLUX);
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
        return mariculture.domain.config.Settings.DNA_SPEED.get() * 5;
    }

    @Override
    protected int[] outputs() {
        return OUTPUTS;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM;
    }

    @Override
    protected void upgradesChanged() {
        super.upgradesChanged();
        tank1.setCapacity((1 + upgrades.storage()) * FLUID);
        tank2.setCapacity((1 + upgrades.storage()) * FLUID);
    }

    @Override
    public int usage() {
        if (upgrades.rf() > 300000) return 1;
        return (int) (rfModifier() * (400 + (upgrades.speed() - 1) * 400));
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == FISH) return LiveFish.isLiveFish(stack);
        if (slot >= DNA) return Dna.isDna(stack);
        if (slot == OUT || slot == OUT_1 || slot == OUT_2) return false;
        return super.validStack(slot, stack);
    }

    @Override
    public int slotLimit(int slot) {
        return slot >= DNA ? 1 : super.slotLimit(slot);
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        if (slot == IN_1 || slot == IN_2) return true;
        return slot == FISH && LiveFish.isLiveFish(stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == OUT_1 || slot == OUT_2 || slot == OUT;
    }

    private static boolean has(Tank t, String fluid) {
        return t.contains(fluid) && t.amount() >= FLUID;
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        return ready() && battery.stored() > MIN_ENERGY;
    }

    private boolean ready() {
        return LiveFish.isLiveFish(inv.get(FISH)) && (has(tank1, Fluids.BLOOD) || has(tank2, Fluids.BLOOD))
                && (has(tank1, Fluids.FLUX) || has(tank2, Fluids.FLUX)) && hasRoom();
    }

    /**
     * Everything but power is there. Not tied to the energy level: canWork is only rechecked every 20 ticks, and a battery
     * that lifts the buffer above the threshold before that recheck must not reset the progress.
     */
    @Override
    protected boolean waitingForPower(MachineWorld world) {
        return ready();
    }

    @Override
    protected void update(MachineWorld world) {
        super.update(world);
        if (world.gameTime() % 20 != 0) return;
        container(world, IN_1, OUT_1, tank1);
        container(world, IN_2, OUT_2, tank2);
    }

    private void container(MachineWorld world, int in, int out, Tank tank) {
        Stack s = inv.get(in);
        if (s.isEmpty() || !inv.get(out).isEmpty()) return;
        MachineWorld.ContainerResult r = world.processContainer(s, tank);
        if (r == null) return;
        inv.shrink(in, 1);
        Stack left = inv.insert(r.container(), new int[] {out}, false);
        if (!left.isEmpty()) world.drop(left);
        markDirty();
    }

    @Override
    protected void process(MachineWorld world) {
        Stack fishStack = inv.get(FISH);
        Genome fish = LiveFish.genome(fishStack);
        if (fish == null) return;
        boolean used = false;
        for (int slot = DNA; slot < SLOTS; slot++) {
            Stack dna = inv.get(slot);
            if (!Dna.isDna(dna)) continue;
            Genome next = Dna.apply(dna, fish);
            inv.set(slot, Dna.wear(dna));
            if (next == null) continue;
            fish = next;
            used = true;
        }
        if (!used) return;
        inv.shrink(FISH, 1);
        insertOutput(world, inject(fishStack, fish));
        tank1.drain(FLUID, false);
        tank2.drain(FLUID, false);
    }

    /** The fish with its new genes; its life is kept, and its item follows the (possibly new) species. */
    public static Stack inject(Stack fishStack, Genome genome) {
        Map<String, Object> data = new LinkedHashMap<>(fishStack.data());
        data.putAll(genome.toData());
        return Stack.of(LiveFish.itemFor(genome.species()), 1, data);
    }

    @Override
    public int[] gauges() {
        return gauges(battery.stored(), battery.capacity());
    }

    @Override
    public void load(Map<String, ?> in) {
        tank1.setCapacity(Integer.MAX_VALUE);
        tank2.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        int cap = (1 + upgrades.storage()) * FLUID;
        tank1.setCapacity(Math.max(cap, tank1.amount()));
        tank2.setCapacity(Math.max(cap, tank2.amount()));
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == IN_1 || slot == IN_2) return SlotInfo.fluidIn();
        if (slot == OUT_1 || slot == OUT_2) return SlotInfo.OUTPUT;
        if (slot == FISH) return SlotInfo.liveFish();
        if (slot >= DNA) return SlotInfo.of(SlotInfo.Role.CATALYST, "dna", java.util.List.of(Dna.ITEM), Dna::isDna);
        return super.info(slot);
    }
}
