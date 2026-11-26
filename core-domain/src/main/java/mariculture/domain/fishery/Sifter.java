package mariculture.domain.fishery;

import mariculture.domain.machine.SlotInfo;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Sifter (fishery/tile/TileSifter and BlockRenderedMachineMulti, 1.3.0). Right-clicking with a siftable item,
 * or dropping one on top, sifts the whole stack into bait. Right-clicking with a basic storage upgrade gives it
 * a ten-slot inventory that catches the bait and that automation can insert siftables into.
 *
 * <p>Simplified: legacy was a two-block multiblock that sprayed bait from either half; this is one block
 * (decision recorded under FISH-014).
 */
public final class Sifter extends Machine {
    public static final int SLOTS = 10;
    public static final String UPGRADE = "mariculture:basic_storage_upgrade";
    private static final int[] ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public static final MachineType TYPE = new MachineType("sifter", Sifter::new, Layout.builder(150)
            .row(0, 5, 44, 20).row(5, 5, 44, 38)
            .build());

    private boolean hasInventory;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return SLOTS;
    }

    public boolean hasInventory() {
        return hasInventory;
    }

    /** Right-click with the basic storage upgrade: returns true when it was consumed. */
    public boolean addInventory() {
        if (hasInventory) return false;
        hasInventory = true;
        return true;
    }

    @Override
    public boolean isValid(int slot, Stack stack) {
        return hasInventory;
    }

    @Override
    public int slotLimit(int slot) {
        return hasInventory ? 64 : 0;
    }

    @Override
    public int[] slots(Face side) {
        return ALL;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return hasInventory && !stack.item().startsWith("mariculture:bait_");
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return hasInventory;
    }

    @Override
    public void tick(MachineWorld world) {
        // Siftables piped into the inventory are sifted at once, like a held stack.
        if (!hasInventory) return;
        for (int slot : ALL) {
            Stack s = inv.get(slot);
            if (s.isEmpty() || s.item().startsWith("mariculture:bait_")) continue;
            List<Bait.SifterRecipe> recipes = Bait.sifterRecipes(s.item(), t -> world.itemHasTag(s.item(), t));
            if (recipes.isEmpty()) continue;
            inv.set(slot, Stack.EMPTY);
            sift(world, s, recipes);
        }
    }

    /**
     * TileSifter.process: every item of the stack rolls every matching recipe. Returns false when nothing in
     * the stack can be sifted.
     */
    public boolean sift(MachineWorld world, Stack stack) {
        List<Bait.SifterRecipe> recipes = Bait.sifterRecipes(stack.item(), t -> world.itemHasTag(stack.item(), t));
        if (recipes.isEmpty()) return false;
        sift(world, stack, recipes);
        return true;
    }

    private void sift(MachineWorld world, Stack stack, List<Bait.SifterRecipe> recipes) {
        for (int i = 0; i < stack.count(); i++) {
            for (Map.Entry<String, Integer> e : Bait.sift(recipes, world.random()).entrySet()) {
                Stack bait = Stack.of(e.getKey(), e.getValue());
                if (hasInventory) bait = inv.insert(bait, ALL, false);
                if (!bait.isEmpty()) world.drop(bait);
            }
        }
    }

    @Override
    public void save(Map<String, Object> out) {
        out.put("has_inventory", hasInventory);
    }

    @Override
    public void load(Map<String, ?> in) {
        hasInventory = bool(in.get("has_inventory"));
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return SlotInfo.STORAGE;
    }
}
