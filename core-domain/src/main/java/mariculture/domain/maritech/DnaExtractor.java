package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Gene;
import mariculture.domain.fish.Genes;
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
 * DNA extractor (maritech/tile/TileExtractor.java, 1.3.0): with a pure fish in slot 6, 10000 mB of fish oil and more
 * than 10000 FE (canWork:73), each completion (1000, ExtensionMachines:24) uses the oil and copies one gene of the fish
 * into a DNA item: up to 20 rounds, each going through the species (1 in 10) and the genes in legacy order, each with
 * its own chance (FishDNABase.getCopyChance), taking the first that hits (process:80). The fish stays. 100000 FE;
 * usage (120 + (speed - 1) x 60) x the RF modifier; the tank holds 10000 + storage x 10000 mB. One extraction takes
 * 120000 FE, more than the buffer, so it needs power coming in; short of power it waits with its progress kept.
 *
 * <p>BUG-069 fixed: legacy compared a gene's upper value with itself, so it copied genes the fish carried on one side
 * only; a gene is copied only when both sides agree. Changed: the tank takes fish oil only (legacy took any fluid and
 * then never worked).
 */
public final class DnaExtractor extends PoweredMachine {
    public static final int OIL = 10000, MIN_ENERGY = 10000, ROUNDS = 20;
    public static final int FLUID_IN = 4, FLUID_OUT = 5, FISH = 6, OUT = 7, SLOTS = 13;
    private static final int[] OUTPUTS = {7, 8, 9, 10, 11, 12};
    private static final int[] SIDED = {FLUID_IN, FLUID_OUT, FISH, 7, 8, 9, 10, 11, 12};

    public static final MachineType TYPE = new MachineType("dna_extractor", DnaExtractor::new, Layout.builder(176)
            .tank(0, 30, 17, 16, 54)
            .slot(FLUID_IN, 52, 17).slot(FLUID_OUT, 52, 53)
            .slot(FISH, 76, 35)
            .progress(G_PROGRESS, G_MAX, 96, 39, 18, 8, "right", 0xFF4A90E2)
            .grid(OUT, 3, 2, 116, 26)
            .slot(BATTERY_SLOT, 8, 53)
            .progress(G_EXTRA, G_EXTRA + 1, 10, 17, 4, 32, "up", 0xFFCC2222)
            .label("gui.mariculture.energy", G_EXTRA, 8, 78)
            .standardSidePanel(true, true)
            .build());

    private final Tank tank = new Tank(10000, f -> f.equals(Fluids.FISH_OIL));

    public DnaExtractor() {
        super(100000);
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
        return mariculture.domain.config.Settings.DNA_SPEED.get();
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
        tank.setCapacity(10000 + upgrades.storage() * 10000);
    }

    @Override
    public int usage() {
        if (upgrades.rf() > 300000) return 1;
        return (int) (rfModifier() * (120 + (upgrades.speed() - 1) * 60));
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == FISH) return LiveFish.isLiveFish(stack);
        if (slot >= OUT) return false;
        return super.validStack(slot, stack);
    }

    @Override
    public int slotLimit(int slot) {
        return slot == FISH ? 1 : super.slotLimit(slot);
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    /** Legacy canInsertItem/canExtractItem both return false: only ejecting moves its items. */
    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return false;
    }

    private Genome fish() {
        Stack s = inv.get(FISH);
        if (!LiveFish.isLiveFish(s)) return null;
        Genome g = LiveFish.genome(s);
        return g != null && g.isPure() ? g : null;
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        return ready() && battery.stored() > MIN_ENERGY;
    }

    private boolean ready() {
        return fish() != null && tank.contains(Fluids.FISH_OIL) && tank.amount() >= OIL && hasRoom();
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
    protected void process(MachineWorld world) {
        Genome fish = fish();
        if (fish == null) return;
        tank.drain(OIL, false);
        Stack dna = extract(fish, world.random());
        if (dna != null) insertOutput(world, dna);
    }

    /** process:80 as a rule: up to 20 rounds over species then each gene, the first whose chance hits. */
    public static Stack extract(Genome fish, mariculture.domain.random.Rng rng) {
        for (int round = 0; round < ROUNDS; round++) {
            if (fish.isPure() && rng.nextInt(Genes.SPECIES_EXTRACTOR_ONE_IN) == 0) return Dna.ofSpecies(fish.species());
            for (Gene g : Genes.ALL) {
                if (fish.upper(g) != fish.lower(g)) continue;
                if (rng.nextInt(Math.max(1, g.extractorOneIn())) == 0) return Dna.ofGene(g, fish.upper(g));
            }
        }
        return null;
    }

    @Override
    public int[] gauges() {
        return gauges(battery.stored(), battery.capacity());
    }

    @Override
    public void load(Map<String, ?> in) {
        tank.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        tank.setCapacity(Math.max(10000 + upgrades.storage() * 10000, tank.amount()));
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == FLUID_IN) return SlotInfo.input("fish_oil", java.util.List.of("mariculture:fish_oil_bucket"), SlotInfo::isFluidContainer);
        if (slot == FLUID_OUT) return SlotInfo.OUTPUT;
        if (slot == FISH) return SlotInfo.liveFish();
        return super.info(slot);
    }
}
