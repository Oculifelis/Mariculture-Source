package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Machine upgrades (core/items/ItemUpgrade, core/handlers/UpgradeHandler). Values are the legacy config
 * defaults (core/config/Machines.java "Upgrade Settings"). The debug upgrades (metas 22, 23, 35-39, CORE-078) have
 * no recipe, as in legacy, and are only in the creative tab.
 */
public final class Upgrades {
    /**
     * One upgrade item. {@code type} is the legacy getType string used by hasUpgrade checks
     * (male, female, ethereal, aquascum...).
     */
    public record Spec(String id, String englishName, int legacyMeta, String type, int storage, int purity, int temperature,
                       int speed, int rf, int salinity) {}

    /** Totals over a machine's upgrade slots (UpgradeHandler.getData; speed starts at 1). */
    public record Totals(int storage, int purity, int temperature, int speed, int rf, int salinity, List<String> types) {
        public static final Totals NONE = new Totals(0, 0, 0, 1, 0, 0, List.of());

        public boolean has(String type) {
            return types.contains(type);
        }
    }

    private static final Map<String, Spec> BY_ID = new LinkedHashMap<>();

    static {
        tiered("storage", 0, 7, 12, 17, "Storage", new int[] {1, 3, 7, 15}, "storage");
        tiered("heating", 1, 8, 13, 18, "Heating", new int[] {1, 2, 5, 14}, "temperature");
        tiered("cooling", 2, 9, 14, 19, "Cooling", new int[] {-1, -2, -5, -14}, "temperature");
        tiered("purity", 3, 10, 15, 20, "Purity", new int[] {1, 2, 3, 4}, "purity");
        tiered("impurity", 4, 11, 16, 21, "Impurity", new int[] {-1, -2, -3, -4}, "purity");
        tiered("speed", 25, 26, 27, 28, "Speed", new int[] {1, 2, 4, 8}, "speed");
        tiered("rf", 29, 30, 31, 32, "RF", new int[] {5000, 20000, 50000, 100000}, "rf");
        add(new Spec("eternal_male_upgrade", "Eternal Male Upgrade", 5, "male", 0, 0, 0, 0, 0, 0));
        add(new Spec("eternal_female_upgrade", "Eternal Female Upgrade", 6, "female", 0, 0, 0, 0, 0, 0));
        add(new Spec("ethereal_upgrade", "Ethereal Upgrade", 24, "ethereal", 0, 0, 0, 0, 0, 0));
        add(new Spec("salinator_upgrade", "Salinator Upgrade", 33, "salinator", 0, 0, 0, 0, 0, 1));
        add(new Spec("filter_upgrade", "Filter Upgrade", 34, "filter", 0, 0, 0, 0, 0, -1));
        add(new Spec("advanced_salinator_upgrade", "Advanced Salinator Upgrade", 40, "salinator", 0, 0, 0, 0, 0, 2));
        add(new Spec("advanced_filter_upgrade", "Advanced Filter Upgrade", 41, "filter", 0, 0, 0, 0, 0, -2));
        add(new Spec("aquascum_upgrade", "AquaScum Upgrade", 42, "aquascum", 0, 0, 0, 0, 0, 0));
        // ItemUpgrade.java:60-170, 227-262, 373-408 (debug metas; names from the legacy lang file).
        add(new Spec("debug_killer_upgrade", "FishKiller Upgrade (Debug)", 22, "debugKill", 0, -10000, 0, 0, 0, 0));
        add(new Spec("debug_live_upgrade", "FishLiveAnyCondition Upgrade (Debug)", 23, "debugLive", 0, 0, 0, 0, 0, 0));
        add(new Spec("debug_hatching_upgrade", "ExtremeHatching Upgrade (Debug)", 35, "incubator", 0, 0, 0, 0, 0, 0));
        add(new Spec("debug_speed_upgrade", "ExtremeSpeed Upgrade (Debug)", 36, "speed", 0, 0, 0, 1024, 0, 0));
        add(new Spec("debug_heating_upgrade", "ExtremeHeating Upgrade (Debug)", 37, "heating", 0, 0, 1000, 0, 0, 0));
        add(new Spec("debug_storage_upgrade", "ExtremeStorage Upgrade (Debug)", 38, "storage", 1000, 0, 0, 0, 5000000, 0));
        add(new Spec("debug_machine_upgrade", "ExtremeMachine Upgrade (Debug)", 39, "debugMachine", 2500, 0, 2500, 1024, 25000000, 0));
    }

    private Upgrades() {}

    private static void tiered(String kind, int basic, int standard, int advanced, int ultimate, String name, int[] values, String stat) {
        String[] tiers = {"basic", "standard", "advanced", "ultimate"};
        int[] metas = {basic, standard, advanced, ultimate};
        for (int i = 0; i < 4; i++) {
            int v = values[i];
            String title = Character.toUpperCase(tiers[i].charAt(0)) + tiers[i].substring(1) + " " + name + " Upgrade";
            add(new Spec(tiers[i] + "_" + kind + "_upgrade", title, metas[i], kind,
                    stat.equals("storage") ? v : 0, stat.equals("purity") ? v : 0, stat.equals("temperature") ? v : 0,
                    stat.equals("speed") ? v : 0, stat.equals("rf") ? v : 0, 0));
        }
    }

    private static void add(Spec spec) {
        if (BY_ID.put("mariculture:" + spec.id(), spec) != null) throw new IllegalStateException("Duplicate upgrade " + spec.id());
    }

    public static List<Spec> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    public static Spec byItem(String item) {
        return BY_ID.get(item);
    }

    public static boolean isUpgrade(Stack stack) {
        return !stack.isEmpty() && (BY_ID.containsKey(stack.item()) || stack.is(TemperatureControl.ITEM));
    }

    public static Totals totals(List<Stack> stacks) {
        int storage = 0, purity = 0, temp = 0, speed = 1, rf = 0, salt = 0;
        List<String> types = new ArrayList<>();
        for (Stack s : stacks) {
            if (s.is(TemperatureControl.ITEM)) {
                temp += TemperatureControl.temperature(s);
                types.add("control");
                continue;
            }
            Spec u = s.isEmpty() ? null : BY_ID.get(s.item());
            if (u == null) continue;
            storage += u.storage();
            purity += u.purity();
            temp += u.temperature();
            speed += u.speed();
            rf += u.rf();
            salt += u.salinity();
            types.add(u.type());
        }
        return new Totals(storage, purity, temp, speed, rf, salt, List.copyOf(types));
    }
}
