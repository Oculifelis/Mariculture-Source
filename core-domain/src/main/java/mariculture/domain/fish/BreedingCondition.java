package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.content.Ids;
import mariculture.domain.json.JsonObject;

/**
 * A breeding need checked when a feeder lays an egg. A species' {@code breeding} list must all hold or the pair lays
 * no egg; a mutation's breeding conditions must all hold for the egg to carry that mutation.
 *
 * <p>Types: {@code light} (min/max block light 0-15 at the tank water, sky light included), {@code height}
 * (min/max y of the feeder), {@code water} (min tank size in blocks), {@code salinity} ({@code is}: list of
 * fresh/brackish/salt), {@code temperature} (min/max tank temperature), {@code rain} ({@code raining}: true or
 * false), {@code food} ({@code item} or {@code #tag} in the feeder's food slot; one is eaten per egg), {@code fluid}
 * ({@code is}: list of fluid ids the tank must hold) and {@code research} ({@code entry}: a research entry the player
 * who placed the feeder has finished; false while no mod answers research questions).
 */
public sealed interface BreedingCondition {
    /** What a feeder knows when its female lays. */
    interface Context {
        int light();

        int height();

        int water();

        Salinity salinity();

        int temperature();

        boolean raining();

        /** Whether the food slot holds the item (or an item in the {@code #tag}). */
        boolean hasFood(String itemOrTag);

        /** The fluid the tank holds, or null before a scan. */
        default String tankFluid() { return null; }

        /** Whether the player who placed the feeder finished the research entry. */
        default boolean researchDone(String entry) { return false; }
    }

    boolean test(Context c);

    record Light(int min, int max) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.light() >= min && c.light() <= max; }
    }

    record Height(int min, int max) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.height() >= min && c.height() <= max; }
    }

    record Water(int min) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.water() >= min; }
    }

    record SalinityIs(List<Salinity> allowed) implements BreedingCondition {
        public SalinityIs { allowed = List.copyOf(allowed); }
        @Override public boolean test(Context c) { return allowed.contains(c.salinity()); }
    }

    record Temperature(int min, int max) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.temperature() >= min && c.temperature() <= max; }
    }

    record Rain(boolean raining) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.raining() == raining; }
    }

    record Food(String itemOrTag) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.hasFood(itemOrTag); }
    }

    record FluidIs(List<String> allowed) implements BreedingCondition {
        public FluidIs { allowed = List.copyOf(allowed); }
        @Override public boolean test(Context c) { return c.tankFluid() != null && allowed.contains(c.tankFluid()); }
    }

    record Research(String entry) implements BreedingCondition {
        @Override public boolean test(Context c) { return c.researchDone(entry); }
    }

    static boolean all(List<BreedingCondition> list, Context c) {
        for (BreedingCondition b : list) if (!b.test(c)) return false;
        return true;
    }

    /** Parses one condition, or returns null for a type this class does not own (mutation egg_tag and config). */
    static BreedingCondition parse(JsonObject o) {
        return switch (o.string("type")) {
            case "light" -> new Light(o.integer("min", 0), o.integer("max", 15));
            case "height" -> new Height(o.integer("min", Integer.MIN_VALUE), o.integer("max", Integer.MAX_VALUE));
            case "water" -> new Water(o.integer("min"));
            case "salinity" -> {
                List<Salinity> list = new ArrayList<>();
                for (String s : o.strings("is")) list.add(Salinity.byName(s));
                if (list.isEmpty()) throw new IllegalArgumentException(o.path() + ": salinity needs at least one value in is");
                yield new SalinityIs(list);
            }
            case "temperature" -> new Temperature(o.integer("min", Integer.MIN_VALUE), o.integer("max", Integer.MAX_VALUE));
            case "rain" -> new Rain(o.bool("raining", true));
            case "food" -> {
                String v = o.has("tag") ? "#" + Ids.vanilla(o.string("tag")) : Ids.vanilla(o.string("item"));
                yield new Food(v);
            }
            case "fluid" -> {
                List<String> list = new ArrayList<>();
                for (String f : o.strings("is")) list.add(Ids.vanilla(f));
                if (list.isEmpty()) throw new IllegalArgumentException(o.path() + ": fluid needs at least one id in is");
                yield new FluidIs(list);
            }
            case "research" -> new Research(Ids.vanilla(o.string("entry")));
            default -> null;
        };
    }

    static List<BreedingCondition> parseAll(List<JsonObject> list) {
        List<BreedingCondition> out = new ArrayList<>();
        for (JsonObject o : list) {
            BreedingCondition b = parse(o);
            if (b == null) throw new IllegalArgumentException(o.path() + ": unknown breeding condition " + o.string("type"));
            out.add(b);
        }
        return out;
    }
}
