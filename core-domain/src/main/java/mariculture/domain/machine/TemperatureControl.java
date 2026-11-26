package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Temperature control (fishery/items/ItemTemperatureControl, 1.3.0): an upgrade that holds up to five heating or
 * cooling upgrades and counts as the sum of their temperatures in one upgrade slot.
 *
 * <p>Change: legacy filled it through a five-slot item GUI. Here the held control takes the upgrade from the other
 * hand on use, and sneak-use gives the last one back; the contents live in the item data ("upgrades", item ids).
 */
public final class TemperatureControl {
    public static final String ITEM = "mariculture:temperature_control";
    public static final String KEY = "upgrades";
    public static final int SLOTS = 5;

    private TemperatureControl() {}

    /** ItemTemperatureControl.isItemValid: an upgrade with a non-zero temperature (never another control). */
    public static boolean accepts(Stack s) {
        Upgrades.Spec spec = s.isEmpty() ? null : Upgrades.byItem(s.item());
        return spec != null && spec.temperature() != 0;
    }

    public static List<String> contents(Stack control) {
        List<String> out = new ArrayList<>();
        if (control.data().get(KEY) instanceof List<?> l) for (Object o : l) if (o instanceof String id) out.add(id);
        return out;
    }

    /** ItemTemperatureControl.getTemperature: the sum of the stored upgrades. */
    public static int temperature(Stack control) {
        int t = 0;
        for (String id : contents(control)) {
            Upgrades.Spec spec = Upgrades.byItem(id);
            if (spec != null) t += spec.temperature();
        }
        return t;
    }

    /** The control with one more upgrade, or null when it is full or the upgrade does not fit. */
    public static Stack add(Stack control, Stack upgrade) {
        List<String> c = contents(control);
        if (c.size() >= SLOTS || !accepts(upgrade)) return null;
        c.add(upgrade.item());
        return with(control, c);
    }

    /** The last stored upgrade's id, or null when empty. */
    public static String last(Stack control) {
        List<String> c = contents(control);
        return c.isEmpty() ? null : c.get(c.size() - 1);
    }

    public static Stack withoutLast(Stack control) {
        List<String> c = contents(control);
        if (c.isEmpty()) return control;
        c.remove(c.size() - 1);
        return with(control, c);
    }

    private static Stack with(Stack control, List<String> ids) {
        Map<String, Object> data = new LinkedHashMap<>(control.data());
        if (ids.isEmpty()) data.remove(KEY);
        else data.put(KEY, List.copyOf(ids));
        return control.withData(data);
    }
}
