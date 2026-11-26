package mariculture.domain.fishery;

import mariculture.domain.machine.SlotInfo;

import mariculture.domain.content.Face;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.Stack;

/**
 * Fish Tank (fishery/tile/TileFishTank, 1.3.0): 54 slots of live fish, all reachable by automation from every
 * side (BUG-044), contents kept when the block is broken.
 *
 * <p>BUG-033: legacy slots held up to Integer.MAX_VALUE fish, which modern item stacks cannot represent or sync;
 * each slot holds one normal stack (64 identical fish), so a tank stores 3456 fish.
 */
public final class FishTank extends Machine {
    public static final int SLOTS = 54;

    public static final MachineType TYPE = new MachineType("fish_tank", FishTank::new, Layout.builder(222)
            .grid(0, 9, 6, 8, 18)
            .build());

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return SLOTS;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return LiveFish.isLiveFish(stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return true;
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return SlotInfo.of(SlotInfo.Role.STORAGE, "live_fish", SlotInfo.liveFishItems(), LiveFish::isLiveFish);
    }
}
