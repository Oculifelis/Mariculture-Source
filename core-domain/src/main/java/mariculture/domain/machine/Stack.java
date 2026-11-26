package mariculture.domain.machine;

import java.util.Map;
import java.util.Objects;

import mariculture.domain.content.Ids;

/**
 * An immutable item stack as the domain sees it: registry id, count and Mariculture item data (the
 * key/value tree targets keep in the stack's NBT on 1.20.1 or its custom-data component later). Two
 * stacks merge only when item and data are equal.
 */
public record Stack(String item, int count, Map<String, Object> data) {
    public static final Stack EMPTY = new Stack("minecraft:air", 0, Map.of());

    public Stack {
        Objects.requireNonNull(item);
        data = data == null ? Map.of() : Map.copyOf(data);
        if (count < 0) count = 0;
    }

    public static Stack of(String item) {
        return of(item, 1);
    }

    public static Stack of(String item, int count) {
        return count <= 0 ? EMPTY : new Stack(Ids.full(item), count, Map.of());
    }

    public static Stack of(String item, int count, Map<String, Object> data) {
        return count <= 0 ? EMPTY : new Stack(Ids.full(item), count, data);
    }

    public boolean isEmpty() {
        return count <= 0 || item.equals("minecraft:air");
    }

    public boolean is(String id) {
        return !isEmpty() && item.equals(Ids.full(id));
    }

    public Stack withCount(int n) {
        return n <= 0 ? EMPTY : new Stack(item, n, data);
    }

    public Stack withData(Map<String, Object> newData) {
        return isEmpty() ? EMPTY : new Stack(item, count, newData);
    }

    public Stack grow(int n) {
        return withCount(count + n);
    }

    public Stack shrink(int n) {
        return withCount(count - n);
    }

    /** Same item and data, ignoring count. */
    public boolean sameItem(Stack other) {
        if (isEmpty() || other.isEmpty()) return false;
        return item.equals(other.item) && data.equals(other.data);
    }

    public int intData(String key, int fallback) {
        return data.get(key) instanceof Number n ? n.intValue() : fallback;
    }

    public String stringData(String key, String fallback) {
        return data.get(key) instanceof String s ? s : fallback;
    }
}
