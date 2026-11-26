package mariculture.domain.fishing;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mariculture.domain.fish.Species;
import mariculture.domain.json.JsonObject;
import mariculture.domain.plugin.Plugins;

/**
 * Other mods' fishing rods that fish as a Mariculture rod tier (compat study SC2/AQ1), read from data packs:
 *
 * <pre>data/&lt;ns&gt;/mariculture/rod_tiers/*.json:
 * {"schema": 1, "plugin": "aquaculture", "rods": {"aquaculture:iron_fishing_rod": "good"}}</pre>
 *
 * A rod listed here catches Mariculture's catch table at that tier, like the vanilla rod (DIRE). {@code plugin} names a
 * Mariculture plugin whose config switch and mod must be on for the file to count. Files are read in id order; a later
 * file's row replaces an earlier one, and {@code "none"} removes a rod. Shipped: redstone_arsenal.json
 * (PluginRedstoneArsenal.java:29, the flux rod as INFUSED, formerly a Java map) and aquaculture.json.
 */
public final class RodTiers {
    public static final int SCHEMA = 1;

    /** One file: its plugin gate (or null) and rod rows; a null tier removes the rod. */
    public record Layer(String plugin, Map<String, Species.Rod> rods) {}

    private static volatile List<Layer> layers = List.of();
    private static volatile Map<String, Species.Rod> tiers = Map.of();

    private RodTiers() {}

    public static Layer parse(String json, String source) {
        JsonObject o = JsonObject.parse(json, source);
        if (o.integer("schema", SCHEMA) != SCHEMA) throw new IllegalArgumentException(source + ": schema must be " + SCHEMA);
        Map<String, Species.Rod> rods = new HashMap<>();
        JsonObject r = o.object("rods");
        for (String k : r.keys()) {
            String v = r.string(k).toUpperCase(Locale.ROOT);
            rods.put(k, v.equals("NONE") ? null : Species.Rod.valueOf(v));
        }
        return new Layer(o.has("plugin") ? o.string("plugin") : null, rods);
    }

    public static void setData(List<Layer> data) {
        layers = List.copyOf(data);
        rebuild();
    }

    /** Re-applies plugin gates (after the config loads). */
    public static void rebuild() {
        Map<String, Species.Rod> m = new HashMap<>();
        for (Layer l : layers) {
            if (l.plugin() != null) {
                Plugins.Plugin p = Plugins.byName(l.plugin());
                if (p == null || !Plugins.active(p)) continue;
            }
            l.rods().forEach((k, v) -> { if (v == null) m.remove(k); else m.put(k, v); });
        }
        tiers = Map.copyOf(m);
    }

    /** The tier another mod's rod fishes as, or null when it is not listed. */
    public static Species.Rod of(String item) {
        return tiers.get(item);
    }
}
