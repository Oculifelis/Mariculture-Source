package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.json.JsonObject;

/**
 * A percentage that may depend on where and when the line is cast. Ordered cases: the first case
 * whose any-of conditions match wins; a case with no conditions is the fallback. Heights are the
 * legacy absolute Y values (FishSpecies subclasses, e.g. FishNight.getCatchChance).
 */
public final class CatchChance {
    public record Context(int y, boolean daytime, String dimension) {}

    /** All set fields must match. Null fields are ignored. */
    public record Condition(Integer yMin, Integer yMax, Boolean daytime, String dimension) {
        boolean matches(Context c) {
            return (yMin == null || c.y() >= yMin) && (yMax == null || c.y() <= yMax)
                    && (daytime == null || c.daytime() == daytime) && (dimension == null || dimension.equals(c.dimension()));
        }
    }

    public record Case(double percent, List<Condition> anyOf) {}

    private final List<Case> cases;

    private CatchChance(List<Case> cases) {
        this.cases = List.copyOf(cases);
    }

    public static CatchChance fixed(double percent) {
        return new CatchChance(List.of(new Case(percent, List.of())));
    }

    public double percent(Context context) {
        for (Case c : cases) {
            if (c.anyOf().isEmpty() || c.anyOf().stream().anyMatch(cond -> cond.matches(context))) return c.percent();
        }
        return 0;
    }

    public boolean alwaysZero() {
        return cases.stream().allMatch(c -> c.percent() == 0);
    }

    static CatchChance parse(JsonObject parent, String key) {
        if (!parent.has(key)) return fixed(0);
        if (parent.raw(key) instanceof Double) return fixed(checkPercent(parent.number(key), parent.path() + "." + key));
        List<Case> cases = new ArrayList<>();
        for (JsonObject c : parent.object(key).objects("cases")) {
            List<Condition> any = new ArrayList<>();
            for (JsonObject cond : c.objects("any_of")) {
                for (String k : cond.keys()) {
                    if (!List.of("y_min", "y_max", "daytime", "dimension").contains(k)) {
                        throw new IllegalArgumentException(cond.path() + ": unknown condition " + k);
                    }
                }
                any.add(new Condition(cond.has("y_min") ? cond.integer("y_min") : null, cond.has("y_max") ? cond.integer("y_max") : null,
                        cond.has("daytime") ? cond.bool("daytime", false) : null, cond.string("dimension", null)));
            }
            cases.add(new Case(checkPercent(c.number("percent"), c.path()), any));
        }
        if (cases.isEmpty()) throw new IllegalArgumentException(parent.path() + "." + key + " has no cases");
        return new CatchChance(cases);
    }

    private static double checkPercent(double p, String where) {
        if (p < 0 || p > 100) throw new IllegalArgumentException(where + ": percent must be 0-100");
        return p;
    }
}
