package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Boosters;
import mariculture.domain.fish.Breeding;
import mariculture.domain.fish.Egg;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fishery.Hatchery;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Ethereal;

/**
 * Incubator (maritech/tile/TileIncubator.java, 1.3.0): nine egg slots (4-12) hatch into nine output slots (13-21).
 * Progress is the speed upgrade total x 10 per tick (updateUpgrades:89), less the heat total every 70 ticks
 * (updateMasterMachine:105), against 400 (ExtensionMachines:23). Each completion makes heat x 4 + 3 hatch attempts
 * (process:126) on a random egg slot: fish eggs hatch through the hatchery rules with birth boost 9 + purity x 8
 * (getBirthChanceBoost:215) and mutation modifier 1, 3 with an ethereal upgrade (getMutationModifier:178);
 * chicken eggs give a chicken spawn egg 1 in 8 and are used up (openEgg:192); dragon eggs give a dragon spawn egg
 * 1 in 64000 (48000 ethereal) and are used up 1 in 10 (openEgg:199). 50000 FE base (getRFCapacity:51); a working tick
 * uses 4 + (36 + (speed - 1) x 40 + heat x 80) x the RF modifier, 1 above 300000 RF upgrades (updatePowerPerTick:147),
 * and it only works with two ticks of energy stored (hasPower:142).
 *
 * <p>Changed: one block instead of the base and two tops (as the crucible and sifter). The debug "incubator" upgrade
 * is not ported.
 */
public final class Incubator extends PoweredMachine {
    public static final int MAX = 400, HEAT_TICK = 70, CHICKEN = 8, DRAGON = 64000, DRAGON_ETHEREAL = 48000, DRAGON_USE = 10;
    public static final int IN = 4, OUT = 13, SLOTS = 22;
    public static final String DRAGON_EGG = "minecraft:dragon_egg", CHICKEN_EGG = "minecraft:egg";
    private static final int[] INPUTS = range(IN, 9), OUTPUTS = range(OUT, 9), SIDED = range(IN, 18);

    public static final MachineType TYPE = new MachineType("incubator", Incubator::new, Layout.builder(176)
            .grid(IN, 3, 3, 26, 17)
            .progress(G_PROGRESS, G_MAX, 84, 35, 24, 8, "right", 0xFF4A90E2)
            .grid(OUT, 3, 3, 116, 17)
            .slot(BATTERY_SLOT, 8, 53)
            .progress(G_EXTRA, G_EXTRA + 1, 10, 17, 4, 32, "up", 0xFFCC2222)
            .label("gui.mariculture.energy", G_EXTRA, 8, 78)
            .standardSidePanel(true, true)
            .build());

    private static int[] range(int first, int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = first + i;
        return r;
    }

    public Incubator() {
        super(50000);
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
        return mariculture.domain.config.Settings.INCUBATOR_SPEED.get();
    }

    @Override
    protected int[] outputs() {
        return OUTPUTS;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM;
    }

    private int heat() {
        return upgrades.temperature();
    }

    @Override
    protected int speed() {
        return upgrades.speed() * 10;
    }

    @Override
    public int usage() {
        if (upgrades.rf() > 300000) return 1;
        return 4 + (int) (rfModifier() * (36 + (upgrades.speed() - 1) * 40 + heat() * 80));
    }

    public static boolean isEgg(Stack s) {
        return !s.isEmpty() && (LiveFish.isEgg(s) || s.item().equals(CHICKEN_EGG) || s.item().equals(DRAGON_EGG));
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot >= IN && slot < OUT) return isEgg(stack);
        if (slot >= OUT) return false;
        return super.validStack(slot, stack);
    }

    @Override
    public int[] slots(Face side) {
        return SIDED;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot >= IN && slot < OUT && isEgg(stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot >= OUT;
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        return hasPower(2) && hasEgg() && hasRoom();
    }

    private boolean hasEgg() {
        for (int s : INPUTS) if (!inv.get(s).isEmpty()) return true;
        return false;
    }

    @Override
    protected int progressStep() {
        int step = super.progressStep();
        if (step > 0 && lastTime % HEAT_TICK == 0) step -= heat();
        return step;
    }

    private long lastTime;

    @Override
    protected void update(MachineWorld world) {
        lastTime = world.gameTime();
        super.update(world);
    }

    @Override
    protected void process(MachineWorld world) {
        int loop = heat() * 4 + 3;
        for (int i = 0; i < loop; i++) hatchOne(world);
    }

    /** hatchEgg:154: tries the egg slots in random order until one is opened. */
    private void hatchOne(MachineWorld world) {
        List<Integer> order = new ArrayList<>(INPUTS.length);
        for (int s : INPUTS) order.add(s);
        for (int i = order.size() - 1; i > 0; i--) {
            int j = world.random().nextInt(i + 1);
            Integer t = order.get(i);
            order.set(i, order.get(j));
            order.set(j, t);
        }
        for (int s : order) if (!inv.get(s).isEmpty() && open(world, s)) return;
    }

    private boolean ethereal() {
        return upgrades.has(Ethereal.UPGRADE);
    }

    private boolean open(MachineWorld world, int slot) {
        Stack s = inv.get(slot);
        if (LiveFish.isEgg(s)) {
            Egg egg = LiveFish.egg(s);
            if (egg == null) return false;
            Breeding.HatchResult r = Breeding.hatch(egg, world.random(), 9 + upgrades.purity() * 8, ethereal() ? 3.0 : 1.0,
                    LiveFish.registry(), Boosters.LEGACY_DEFAULTS, Hatchery.OPTIONS);
            inv.set(slot, r.egg() == null ? Stack.EMPTY : LiveFish.eggStack(r.egg()));
            for (Genome g : r.fish()) insertOutput(world, LiveFish.stack(g));
            if (r.failedBreeds() > 0) insertOutput(world, Stack.of("minecraft:cod", r.failedBreeds()));
            return false;
        }
        if (s.item().equals(CHICKEN_EGG)) {
            if (world.random().nextInt(CHICKEN) == 0) insertOutput(world, Stack.of("minecraft:chicken_spawn_egg", 1));
            inv.shrink(slot, 1);
            return true;
        }
        if (s.item().equals(DRAGON_EGG)) {
            if (world.random().nextInt(ethereal() ? DRAGON_ETHEREAL : DRAGON) == 0) insertOutput(world, Stack.of("mariculture:dragon_egg_item", 1));
            if (world.random().nextInt(DRAGON_USE) == 0) {
                inv.shrink(slot, 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] gauges() {
        return gauges(battery.stored(), battery.capacity());
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot >= IN && slot < OUT) return SlotInfo.input("egg", java.util.List.of(LiveFish.EGGS, CHICKEN_EGG, DRAGON_EGG), Incubator::isEgg);
        return super.info(slot);
    }
}
