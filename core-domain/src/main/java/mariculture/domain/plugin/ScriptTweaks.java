package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.json.JsonObject;

/**
 * The KubeJS binding ({@code Mariculture} in server scripts): the data-pack change format ({@link TweakData}) from a
 * script, for packs that use KubeJS instead of CraftTweaker. KubeJS runs server scripts again on every reload, and
 * each target clears these changes before it does.
 *
 * <pre>
 * Mariculture.add({type: 'anvil_add', input: 'minecraft:cobblestone', output: 'minecraft:gravel', hits: 2})
 * Mariculture.add({changes: [{type: 'loot_remove', item: 'minecraft:nether_star'}]})
 * Mariculture.add('{"type": "melting_remove", "input": "minecraft:iron_ingot"}')
 * </pre>
 */
public final class ScriptTweaks {
    public static final ScriptTweaks INSTANCE = new ScriptTweaks();

    private int count;

    private ScriptTweaks() {}

    /** Clears the script changes; called before each server script load. */
    public synchronized void reset() {
        count = 0;
        Tweaks.clearScript();
    }

    /** One change, {@code {changes: [...]}}, or either as a JSON string. Throws with the reason when it is wrong. */
    public synchronized void add(Object change) {
        String id = "kubejs:script/" + count++;
        List<Tweaks.Change> out;
        if (change instanceof CharSequence json) {
            out = TweakData.parse(id, json.toString(), "KubeJS script");
        } else if (change instanceof Map<?, ?> map) {
            Object plain = plain(map);
            @SuppressWarnings("unchecked")
            JsonObject o = new JsonObject((Map<String, Object>) plain, "KubeJS script:");
            out = new ArrayList<>();
            if (o.has("type")) {
                out.add(TweakData.change(id, o));
            } else {
                List<JsonObject> changes = o.objects("changes");
                for (int n = 0; n < changes.size(); n++) out.add(TweakData.change(id + "/" + n, changes.get(n)));
            }
        } else {
            throw new IllegalArgumentException("Mariculture.add takes an object or a JSON string, not " + change);
        }
        out.forEach(Tweaks::addScript);
    }

    /** Script values (maps, lists, any number type, strings) as the JSON reader's types. */
    static Object plain(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, x) -> out.put(String.valueOf(k), plain(x)));
            return out;
        }
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) out.add(plain(x));
            return out;
        }
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof CharSequence s) return s.toString();
        return v;
    }
}
