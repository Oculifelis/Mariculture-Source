package mariculture.domain.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Typed, path-aware view over a parsed object. Errors name the field path so a broken data pack
 * reports e.g. "fish_species/cod.json: temperature.base must be an integer".
 */
public final class JsonObject {
    private final Map<String, Object> map;
    private final String path;

    public JsonObject(Map<String, Object> map, String path) {
        this.map = map;
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    public static JsonObject parse(String json, String source) {
        Object v = JsonReader.parse(json);
        if (!(v instanceof Map)) throw new IllegalArgumentException(source + ": expected a JSON object");
        return new JsonObject((Map<String, Object>) v, source + ":");
    }

    private String where(String key) {
        return path.endsWith(":") ? path + " " + key : path + "." + key;
    }

    public boolean has(String key) {
        return map.containsKey(key) && map.get(key) != null;
    }

    public Set<String> keys() {
        return map.keySet();
    }

    public Object raw(String key) {
        return map.get(key);
    }

    public int integer(String key) {
        Object v = require(key);
        if (!(v instanceof Double d) || d != Math.rint(d) || Math.abs(d) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(where(key) + " must be an integer");
        }
        return (int) (double) d;
    }

    public int integer(String key, int fallback) {
        return has(key) ? integer(key) : fallback;
    }

    public double number(String key) {
        Object v = require(key);
        if (!(v instanceof Double d) || d.isNaN() || d.isInfinite()) throw new IllegalArgumentException(where(key) + " must be a number");
        return d;
    }

    public boolean bool(String key, boolean fallback) {
        if (!has(key)) return fallback;
        Object v = map.get(key);
        if (!(v instanceof Boolean b)) throw new IllegalArgumentException(where(key) + " must be true or false");
        return b;
    }

    public String string(String key) {
        Object v = require(key);
        if (!(v instanceof String s)) throw new IllegalArgumentException(where(key) + " must be a string");
        return s;
    }

    public String string(String key, String fallback) {
        return has(key) ? string(key) : fallback;
    }

    @SuppressWarnings("unchecked")
    public JsonObject object(String key) {
        Object v = require(key);
        if (!(v instanceof Map)) throw new IllegalArgumentException(where(key) + " must be an object");
        return new JsonObject((Map<String, Object>) v, where(key));
    }

    @SuppressWarnings("unchecked")
    public List<JsonObject> objects(String key) {
        if (!has(key)) return List.of();
        Object v = map.get(key);
        if (!(v instanceof List<?> l)) throw new IllegalArgumentException(where(key) + " must be an array");
        List<JsonObject> out = new ArrayList<>();
        for (int n = 0; n < l.size(); n++) {
            if (!(l.get(n) instanceof Map)) throw new IllegalArgumentException(where(key) + "[" + n + "] must be an object");
            out.add(new JsonObject((Map<String, Object>) l.get(n), where(key) + "[" + n + "]"));
        }
        return out;
    }

    public List<String> strings(String key) {
        if (!has(key)) return List.of();
        Object v = map.get(key);
        if (!(v instanceof List<?> l)) throw new IllegalArgumentException(where(key) + " must be an array");
        List<String> out = new ArrayList<>();
        for (Object o : l) {
            if (!(o instanceof String s)) throw new IllegalArgumentException(where(key) + " must contain only strings");
            out.add(s);
        }
        return out;
    }

    private Object require(String key) {
        if (!has(key)) throw new IllegalArgumentException(where(key) + " is required");
        return map.get(key);
    }

    public String path() {
        return path;
    }
}
