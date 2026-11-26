package mariculture.domain.maritech;

import java.util.LinkedHashMap;
import java.util.Map;

import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Stack;

/**
 * Energy items (maritech/items/ItemBattery.java, ExtensionCore.java:31-32; core/items/ItemCrafting.java:194-213,
 * 1.3.0). The charge is kept in the item data under "Energy", the key ItemBattery.make wrote. Targets expose these
 * items as Forge Energy (FE counts the same as RF).
 *
 * <p>Copper battery: 10000, takes 100 and gives 250 a tick. Titanium battery: 100000, takes 1000 and gives 2500.
 * Creative battery: gives whatever is asked and takes nothing (legacy returned 10000000 whatever was asked, so a
 * machine could be told it got more than it asked for; here it gives exactly the amount asked).
 */
public final class EnergyItem {
    public static final String COPPER = "copper_battery", TITANIUM = "titanium_battery", CREATIVE = "creative_battery", FLUX_ROD = "flux_rod";
    public static final String KEY = "Energy";

    /** One energy item; {@code capacity} {@link Integer#MAX_VALUE} for the creative battery. */
    public record Spec(String id, int capacity, int maxReceive, int maxExtract, boolean infinite) {}

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec(COPPER, 10000, 100, 250, false));
        add(new Spec(TITANIUM, 100000, 1000, 2500, false));
        add(new Spec(CREATIVE, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, true));
        // maritech/items/ItemFluxRod.java:83-85: a fishing rod that runs on energy, 100 FE a cast.
        add(new Spec(FLUX_ROD, 250000, 250, 100, false));
    }

    private EnergyItem() {}

    private static void add(Spec spec) {
        SPECS.put("mariculture:" + spec.id(), spec);
    }

    public static Spec spec(String item) {
        return SPECS.get(item);
    }

    public static Spec spec(Stack stack) {
        return stack.isEmpty() ? null : SPECS.get(stack.item());
    }

    public static int stored(Stack stack) {
        Spec s = spec(stack);
        if (s == null) return 0;
        return s.infinite() ? s.capacity() : Math.max(0, Math.min(s.capacity(), stack.intData(KEY, 0)));
    }

    public static Stack withEnergy(Stack stack, int energy) {
        Map<String, Object> data = new LinkedHashMap<>(stack.data());
        data.put(KEY, energy);
        return stack.withData(data);
    }

    /** A move of energy into or out of an item: the amount and the item afterwards. */
    public record Transfer(int amount, Stack stack) {}

    /** IEnergyContainerItem.receiveEnergy for a single item. */
    public static Transfer receive(Stack stack, int max, boolean simulate) {
        Spec s = spec(stack);
        if (s == null || s.infinite() || stack.count() != 1) return new Transfer(0, stack);
        int stored = stored(stack);
        int in = Math.max(0, Math.min(s.capacity() - stored, Math.min(s.maxReceive(), max)));
        return new Transfer(in, simulate || in == 0 ? stack : withEnergy(stack, stored + in));
    }

    /** IEnergyContainerItem.extractEnergy for a single item. */
    public static Transfer extract(Stack stack, int max, boolean simulate) {
        Spec s = spec(stack);
        if (s == null || stack.count() != 1) return new Transfer(0, stack);
        if (s.infinite()) return new Transfer(Math.max(0, max), stack);
        int stored = stored(stack);
        int out = Math.max(0, Math.min(stored, Math.min(s.maxExtract(), max)));
        return new Transfer(out, simulate || out == 0 ? stack : withEnergy(stack, stored - out));
    }

    /**
     * Charges a machine's battery from an energy item, up to {@code max} (the air compressor takes 5000 a click,
     * ExtensionRenderedMachineMulti.java:93; powered machines take 3000 a tick from their battery slot,
     * TileMultiMachinePowered.updateMaster). Returns the item afterwards, or null when nothing moved.
     */
    public static Stack charge(Battery battery, Stack item, int max) {
        int offered = extract(item, max, true).amount();
        if (offered <= 0) return null;
        int taken = battery.receive(offered, false);
        if (taken <= 0) return null;
        return extract(item, taken, false).stack();
    }

    /** The charge bar, 0 to 13 pixels, like ItemBattery.getDisplayDamage. */
    public static int bar(Stack stack) {
        Spec s = spec(stack);
        if (s == null || s.infinite()) return 13;
        return (int) Math.round(13.0 * stored(stack) / s.capacity());
    }
}
