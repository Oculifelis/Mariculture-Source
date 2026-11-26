package mariculture.domain.fishery;

import mariculture.domain.machine.SlotInfo;

import java.util.Map;
import java.util.Set;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Boosters;
import mariculture.domain.fish.Breeding;
import mariculture.domain.fish.Egg;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Hatchery (fishery/tile/TileHatchery, 1.3.0): warms one egg (slot 0), fish come out into slots 1-2.
 * Progress per tick is max(1, (27 - |hatcheries - water|) * hatcheries) over the 3x3x3 around it, against 100.
 *
 * <p>BUG-043 / BUG-029 fixed: legacy compared each neighbour with itself, so it always counted one hatchery
 * (speed 26 in water instead of the advertised stacking). Neighbouring hatcheries now count, as intended.
 */
public final class Hatchery extends Machine {
    public static final int SPEED = 100;
    public static final int BIRTH_BOOST = 19;
    /** Server options mutations may require (FishMechanics.ENABLE_METAL_FISH on; the commented-out extra recipes off). */
    public static final Set<String> OPTIONS = Set.of("enable_metal_fish");
    private static final int[] OUT = {1, 2};

    public static final MachineType TYPE = new MachineType("hatchery", Hatchery::new, Layout.builder(150)
            .slot(0, 52, 30)
            .progressFixed(0, SPEED, 74, 34, 24, 8, "right", 0xFF4A90E2)
            .slot(1, 104, 30).slot(2, 122, 30)
            .build());

    private int processed;
    private int speed = 1;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 3;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return slot == 0 && LiveFish.isEgg(stack);
    }

    @Override
    public int slotLimit(int slot) {
        return 1;
    }

    @Override
    public int[] slots(Face side) {
        return side == Face.DOWN ? OUT : new int[] {0};
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return slot == 0 && side != Face.DOWN && inv.get(0).isEmpty() && stack.count() == 1 && LiveFish.isEgg(stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return side == Face.DOWN && slot > 0;
    }

    @Override
    public void tick(MachineWorld world) {
        if (world.gameTime() % 20 == 0) speed = speed(world);
        boolean canWork = LiveFish.isEgg(inv.get(0)) && (inv.get(1).isEmpty() || inv.get(2).isEmpty());
        if (!canWork) {
            processed = 0;
            return;
        }
        processed += speed;
        if (processed >= mariculture.domain.config.Settings.HATCHERY_SPEED.get()) {
            processed = 0;
            hatch(world);
        }
    }

    static int speed(int hatcheries, int water) {
        return Math.max(1, (27 - Math.abs(hatcheries - water)) * hatcheries);
    }

    private static int speed(MachineWorld world) {
        int hatcheries = 0, water = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (world.machine(x, y, z) instanceof Hatchery) hatcheries++;
                    if (world.fluid(x, y, z).equals("minecraft:water")) water++;
                }
            }
        }
        return speed(hatcheries, water);
    }

    /** FishyHelper.attemptToHatchEgg through the domain rules (Breeding.hatch). */
    private void hatch(MachineWorld world) {
        Egg egg = LiveFish.egg(inv.get(0));
        if (egg == null) return;
        Breeding.HatchResult r = Breeding.hatch(egg, world.random(), BIRTH_BOOST, 1.0, LiveFish.registry(), Boosters.LEGACY_DEFAULTS, OPTIONS);
        inv.set(0, r.egg() == null ? Stack.EMPTY : LiveFish.eggStack(r.egg()));
        for (Genome g : r.fish()) eject(world, LiveFish.stack(g));
        if (r.failedBreeds() > 0) eject(world, Stack.of("minecraft:cod", r.failedBreeds()));
    }

    private void eject(MachineWorld world, Stack s) {
        Stack left = inv.insert(s, OUT, false);
        if (!left.isEmpty()) world.drop(left);
    }

    @Override
    public int[] gauges() {
        return new int[] {processed};
    }

    @Override
    public void save(Map<String, Object> out) {
        out.put("processed", processed);
    }

    @Override
    public void load(Map<String, ?> in) {
        processed = in.get("processed") instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return slot == 0 ? SlotInfo.input("fish_eggs", java.util.List.of(LiveFish.EGGS), LiveFish::isEgg) : SlotInfo.OUTPUT;
    }
}
