package mariculture.domain.fishing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.config.Settings;
import mariculture.domain.json.JsonObject;

/**
 * The fishing speed multiplier (FishMechanics.java:37, "Give me fish NAOW", default 50): legacy's hook took it off
 * the wait for a bite every tick on top of the normal countdown (EntityHook.java:254). The config value
 * ({@link Settings#FISHING_SPEED}) can be overridden by data packs, which can also make single rods faster:
 *
 * <pre>data/&lt;ns&gt;/mariculture/fishing_speed/*.json: {"speed_multiplier": 20, "rods": {"mariculture:titanium_fishing_rod": 10}}</pre>
 *
 * Files are read in id order; a later file's multiplier replaces an earlier one and rod bonuses add up.
 */
public final class FishingSpeed {
    public static final int SCHEMA = 1;

    /** One file: the multiplier it sets (null to keep the config) and per-rod bonuses. */
    public record Entry(Integer multiplier, Map<String, Integer> rods) {
        public Entry {
            rods = Map.copyOf(rods);
        }
    }

    private static volatile Integer override;
    private static volatile Map<String, Integer> rodBonus = Map.of();

    private FishingSpeed() {}

    public static Entry parse(String json, String source) {
        JsonObject o = JsonObject.parse(json, source);
        if (o.integer("schema", SCHEMA) != SCHEMA) throw new IllegalArgumentException(source + ": schema must be " + SCHEMA);
        Integer m = o.has("speed_multiplier") ? o.integer("speed_multiplier") : null;
        if (m != null && m < 0) throw new IllegalArgumentException(source + ": speed_multiplier must not be negative");
        Map<String, Integer> rods = new HashMap<>();
        if (o.has("rods")) {
            JsonObject r = o.object("rods");
            for (String k : r.keys()) rods.put(k, r.integer(k));
        }
        return new Entry(m, rods);
    }

    /** The data packs' entries, in file order; replaces what the last reload set. */
    public static void setData(List<Entry> entries) {
        Integer m = null;
        Map<String, Integer> rods = new HashMap<>();
        for (Entry e : entries) {
            if (e.multiplier() != null) m = e.multiplier();
            e.rods().forEach((k, v) -> rods.merge(k, v, Integer::sum));
        }
        override = m;
        rodBonus = Map.copyOf(rods);
    }

    /** Ticks taken off the bite wait every tick for a hook cast with this rod item; never negative. */
    public static int forRod(String rodItem) {
        Integer m = override;
        int base = m != null ? m : Settings.FISHING_SPEED.get();
        return Math.max(0, base + rodBonus.getOrDefault(rodItem, 0));
    }
}
