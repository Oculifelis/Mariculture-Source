package mariculture.domain.env;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import mariculture.domain.fish.Salinity;
import mariculture.domain.json.JsonObject;
import mariculture.domain.plugin.Plugins;

/**
 * Biome salinity and temperature, read from data packs (compat study G2):
 *
 * <pre>data/&lt;ns&gt;/mariculture/biome_environment/*.json:
 * {"schema": 1, "plugin": "biomesoplenty", "priority": 0,
 *  "biomes": {"minecraft:ocean": {"salinity": "saline", "temperature": 4}, "#minecraft:is_ocean": {...}}}</pre>
 *
 * A key is a biome id or {@code #}tag. Ids win over tags; for an id, the higher priority file wins, then the later file. Tags are
 * tried by priority (higher first), then file order, then the order in the file. {@code plugin} names a Mariculture
 * plugin whose config switch and mod must be on for the file to count. A biome nothing matches is fresh water at 10.
 *
 * <p>The shipped files hold the legacy tables: vanilla.json (core/handlers/EnvironmentHandler and Core.registerBiomes,
 * 1.3.0, lines 275-316; modern oceans with no 1.7 equivalent are saline, CORE-085), tags.json (EnvironmentHandler's
 * BiomeDictionary fallback order, priority -100) and biomesoplenty.json (PluginBiomesOPlenty.java:63-142). Legacy
 * gave every tag fallback fresh water, so modded oceans read as fresh; tags.json makes #minecraft:is_ocean saline and
 * #minecraft:is_beach brackish (BUG-076).
 */
public final class BiomeEnvironment {
    public static final int SCHEMA = 1;
    public static final Entry DEFAULT = new Entry(Salinity.FRESH, 10);

    public record Entry(Salinity salinity, int temperature) {}

    /** One data file: its plugin gate (or null), tag priority and entries in file order. */
    public record Layer(String plugin, int priority, Map<String, Entry> biomes) {
        public Layer {
            biomes = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(biomes));
        }
    }

    private record TagRule(String tag, Entry entry) {}

    private static volatile List<Layer> layers = List.of();
    private static volatile Map<String, Entry> ids = Map.of();
    private static volatile List<TagRule> tags = List.of();

    private BiomeEnvironment() {}

    public static Layer parse(String json, String source) {
        JsonObject o = JsonObject.parse(json, source);
        if (o.integer("schema", SCHEMA) != SCHEMA) throw new IllegalArgumentException(source + ": schema must be " + SCHEMA);
        Map<String, Entry> biomes = new LinkedHashMap<>();
        JsonObject b = o.object("biomes");
        for (String k : b.keys()) {
            JsonObject e = b.object(k);
            biomes.put(k, new Entry(Salinity.byName(e.string("salinity")), e.integer("temperature")));
        }
        return new Layer(o.has("plugin") ? o.string("plugin") : null, o.integer("priority", 0), biomes);
    }

    /** The data packs' files in load order; replaces what the last reload set. */
    public static void setData(List<Layer> data) {
        layers = List.copyOf(data);
        rebuild();
    }

    /** Re-applies plugin gates (after the config loads). */
    public static void rebuild() {
        Map<String, Entry> idMap = new HashMap<>();
        List<Layer> sorted = new ArrayList<>();
        for (Layer l : layers) {
            if (l.plugin() != null && !gateOpen(l.plugin())) continue;
            sorted.add(l);
        }
        // ids: lowest priority first so higher priority (then later file) overwrites
        List<Layer> up = new ArrayList<>(sorted);
        up.sort(Comparator.comparingInt(Layer::priority));
        for (Layer l : up) l.biomes().forEach((k, v) -> { if (!k.startsWith("#")) idMap.put(k, v); });
        sorted.sort(Comparator.comparingInt(Layer::priority).reversed()); // stable: file order within a priority
        List<TagRule> t = new ArrayList<>();
        for (Layer l : sorted) l.biomes().forEach((k, v) -> { if (k.startsWith("#")) t.add(new TagRule(k.substring(1), v)); });
        ids = Map.copyOf(idMap);
        tags = List.copyOf(t);
    }

    private static boolean gateOpen(String plugin) {
        Plugins.Plugin p = Plugins.byName(plugin);
        return p != null && Plugins.active(p);
    }

    /**
     * @param hasTag whether the biome has a tag (callers map "c:" to "forge:" on 1.20.1)
     */
    public static Entry of(String biome, Predicate<String> hasTag) {
        Entry e = ids.get(biome);
        if (e != null) return e;
        for (TagRule r : tags) if (hasTag.test(r.tag())) return r.entry();
        return DEFAULT;
    }

    /**
     * EnvironmentHandler.getTemperature: biome temperature - 3, plus sun brightness * 9 - 2, minus 0.11255 per block
     * above 60 (the legacy below-68 branch is unreachable for y above 60 and applies below), minus rain * 0.15.
     *
     * @param sunBrightness legacy World.getSunBrightness (0.2 at night to 1.0 at noon)
     * @param rain          rain strength 0..1
     */
    public static int temperature(int biomeTemperature, float sunBrightness, int y, float rain) {
        int t = biomeTemperature - 3;
        t = (int) (t + sunBrightness * 9 - 2); // legacy int += float
        if (y > 60) t = (int) ((60 - y) * 0.11255) + t;
        else if (y < 68) t = (int) (t - (68 - y) * 0.11255);
        t -= (int) (rain * 0.15);
        return t;
    }

    /** Legacy EnvironmentHandler.getSunBrightness (a copy of 1.7 World.getSunBrightness) from the celestial angle 0..1. */
    public static float sunBrightness(float celestialAngle, float rain, float thunder) {
        float f2 = 1.0f - ((float) Math.cos(celestialAngle * Math.PI * 2.0) * 2.0f + 0.2f);
        f2 = Math.max(0f, Math.min(1f, f2));
        f2 = 1.0f - f2;
        f2 = (float) (f2 * (1.0 - rain * 5.0f / 16.0));
        f2 = (float) (f2 * (1.0 - thunder * 5.0f / 16.0));
        return f2 * 0.8f + 0.2f;
    }
}
