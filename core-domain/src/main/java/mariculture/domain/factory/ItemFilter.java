package mariculture.domain.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.machine.Stack;

/**
 * Item filter (factory/items/ItemFilter.java, 1.3.0): nine items placed in an Automatic Dictionary Converter's filter
 * slot as one.
 *
 * <p>Changed: legacy edited the nine items in a screen; here the filter is used with an item in the other hand to
 * add it, and used while sneaking to empty it (like the fish DNA scanner).
 */
public final class ItemFilter {
    public static final String ID = "item_filter";
    public static final int SIZE = 9;
    private static final String KEY = "filter_items";

    private ItemFilter() {}

    public static boolean is(Stack s) {
        return s.is("mariculture:" + ID);
    }

    public static List<String> entries(Stack filter) {
        List<String> out = new ArrayList<>();
        if (filter.data().get(KEY) instanceof List<?> l) for (Object o : l) if (o instanceof String id) out.add(id);
        return out;
    }

    /** The filter with {@code item} added, or null when it is full or already holds it. */
    public static Stack add(Stack filter, String item) {
        List<String> e = entries(filter);
        if (e.size() >= SIZE || e.contains(item) || item.equals("minecraft:air")) return null;
        e.add(item);
        Map<String, Object> data = new LinkedHashMap<>(filter.data());
        data.put(KEY, List.copyOf(e));
        return filter.withData(data);
    }

    public static Stack clear(Stack filter) {
        Map<String, Object> data = new LinkedHashMap<>(filter.data());
        data.remove(KEY);
        return filter.withData(data);
    }
}
