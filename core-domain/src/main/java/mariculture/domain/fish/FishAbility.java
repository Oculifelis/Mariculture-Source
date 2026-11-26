package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mariculture.domain.content.Ids;
import mariculture.domain.json.JsonObject;

/**
 * A typed tank ability read from a species' {@code abilities} list. A working feeder applies area abilities of the
 * fish whose turn it is (every effect tick, male and female in turn); the other kinds change the feeder's own rules
 * for that fish. Every number sits in the species JSON, so packs tune or replace them freely.
 *
 * <p>Area abilities share {@code radius} (blocks beyond the tank water's bounds, default 4), {@code targets}
 * (players, hostile or all; default players), {@code chance_percent} (default 100) and {@code when} (any, day,
 * night; default any).
 */
public sealed interface FishAbility {
    enum Targets { PLAYERS, HOSTILE, ALL;
        public boolean accepts(FishEffects.Living e) {
            return switch (this) {
                case PLAYERS -> e.player();
                case HOSTILE -> e.hostile();
                case ALL -> true;
            };
        }
    }

    enum When { ANY, DAY, NIGHT;
        public boolean allows(boolean day) {
            return this == ANY || (this == DAY) == day;
        }
    }

    record Area(int radius, Targets targets, int chancePercent, When when) {
        public Area {
            if (radius < 0 || radius > 16) throw new IllegalArgumentException("radius must be 0-16");
            if (chancePercent < 0 || chancePercent > 100) throw new IllegalArgumentException("chance_percent must be 0-100");
        }
    }

    /** An ability that acts on living things or blocks near the tank. */
    sealed interface AreaAbility extends FishAbility permits Potion, Ignite, Heal, Damage, Xp, GrowPlants {
        Area area();

        /** The actions for one living entity in range; empty when it is not a target. */
        default List<FishEffects.Action> actions(FishEffects.Living e) {
            return List.of();
        }
    }

    record Potion(Area area, String effect, int amplifier, int seconds, boolean ambient) implements AreaAbility {
        @Override public List<FishEffects.Action> actions(FishEffects.Living e) {
            return area.targets().accepts(e) ? List.of(new FishEffects.Potion(effect, seconds * 20, amplifier, ambient)) : List.of();
        }
    }

    record Ignite(Area area, int seconds) implements AreaAbility {
        @Override public List<FishEffects.Action> actions(FishEffects.Living e) {
            return area.targets().accepts(e) ? List.of(new FishEffects.Fire(seconds)) : List.of();
        }
    }

    record Heal(Area area, float amount) implements AreaAbility {
        @Override public List<FishEffects.Action> actions(FishEffects.Living e) {
            return area.targets().accepts(e) ? List.of(new FishEffects.Heal(amount)) : List.of();
        }
    }

    record Damage(Area area, float amount) implements AreaAbility {
        @Override public List<FishEffects.Action> actions(FishEffects.Living e) {
            return area.targets().accepts(e) ? List.of(new FishEffects.Damage("generic", amount)) : List.of();
        }
    }

    record Xp(Area area, int points) implements AreaAbility {
        @Override public List<FishEffects.Action> actions(FishEffects.Living e) {
            return area.targets().accepts(e) && e.player() ? List.of(new FishEffects.Xp(points)) : List.of();
        }
    }

    /** Bonemeals up to {@code tries} random crops or saplings in range (blocks in minecraft:crops or minecraft:saplings). */
    record GrowPlants(Area area, int tries) implements AreaAbility {}

    /** Each product roll of this fish gets a second roll with this chance. */
    record ProductBonus(int chancePercent) implements FishAbility {}

    /** This fish loses that much less life per cycle. */
    record Longevity(int percent) implements FishAbility {}

    /** This fish skips its fish food use with that chance. */
    record FoodSaver(int percent) implements FishAbility {}

    /**
     * Adds {@code amount} magic energy to the aura at the tank (Thaumaturge vis) on each effect tick, with
     * {@code chance_percent}. Does nothing without an aura mod.
     */
    record VisRefill(float amount, int chancePercent) implements FishAbility {}

    /**
     * Drains up to {@code amount} aura pollution at the tank (Thaumaturge flux) on each effect tick, with
     * {@code chance_percent}. Does nothing without an aura mod.
     */
    record FluxEat(float amount, int chancePercent) implements FishAbility {}

    static FishAbility parse(JsonObject o) {
        String type = o.string("type");
        return switch (type) {
            case "potion" -> new Potion(area(o), Ids.vanilla(o.string("effect")), o.integer("amplifier", 0), positive(o, "seconds", 15), o.bool("ambient", true));
            case "ignite" -> new Ignite(area(o), positive(o, "seconds", 4));
            case "heal" -> new Heal(area(o), (float) amount(o));
            case "damage" -> new Damage(area(o, Targets.HOSTILE), (float) amount(o));
            case "xp" -> new Xp(area(o), positive(o, "points", 1));
            case "grow_plants" -> new GrowPlants(area(o), positive(o, "tries", 1));
            case "product_bonus" -> new ProductBonus(percent(o, "chance_percent"));
            case "longevity" -> new Longevity(percent(o, "percent"));
            case "food_saver" -> new FoodSaver(percent(o, "percent"));
            case "vis_refill" -> new VisRefill((float) amount(o), percentOr(o, "chance_percent", 100));
            case "flux_eat" -> new FluxEat((float) amount(o), percentOr(o, "chance_percent", 100));
            default -> throw new IllegalArgumentException(o.path() + ": unknown ability type " + type);
        };
    }

    static List<FishAbility> parseAll(List<JsonObject> list) {
        List<FishAbility> out = new ArrayList<>();
        for (JsonObject o : list) out.add(parse(o));
        return out;
    }

    private static Area area(JsonObject o) {
        return area(o, Targets.PLAYERS);
    }

    private static Area area(JsonObject o, Targets fallback) {
        try {
            return new Area(o.integer("radius", 4), Targets.valueOf(o.string("targets", fallback.name()).toUpperCase(Locale.ROOT)),
                    o.integer("chance_percent", 100), When.valueOf(o.string("when", "any").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(o.path() + ": " + e.getMessage(), e);
        }
    }

    private static int positive(JsonObject o, String key, int fallback) {
        int v = o.integer(key, fallback);
        if (v < 1) throw new IllegalArgumentException(o.path() + ": " + key + " must be at least 1");
        return v;
    }

    private static int percentOr(JsonObject o, String key, int fallback) {
        return o.has(key) ? percent(o, key) : fallback;
    }

    private static int percent(JsonObject o, String key) {
        int v = o.integer(key);
        if (v < 0 || v > 100) throw new IllegalArgumentException(o.path() + ": " + key + " must be 0-100");
        return v;
    }

    private static double amount(JsonObject o) {
        double v = o.number("amount");
        if (v <= 0) throw new IllegalArgumentException(o.path() + ": amount must be above 0");
        return v;
    }
}
