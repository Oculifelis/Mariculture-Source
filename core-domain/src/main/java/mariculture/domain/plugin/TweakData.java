package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.json.JsonObject;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.Casting;
import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.VatRecipes;

/**
 * Machine-table changes from data packs ({@code data/<ns>/mariculture/tweaks/*.json}): the same {@link Tweaks.Change}s
 * the CraftTweaker classes make, written as JSON, so packs (and KubeJS, whose {@code kubejs/data} folder is a data
 * pack) can change the vat, crucible, anvil, casters and fishing loot on every edition. Schema in docs/tweaks.md.
 *
 * <pre>{"requires": ["othermod"], "changes": [{"type": "melting_remove", "input": "minecraft:iron_ingot"}, ...]}</pre>
 */
public final class TweakData {
    public static final int SCHEMA = 1;

    private TweakData() {}

    /** Every change in one file, in file order. {@code id} names the file (used for vat recipe ids). */
    public static List<Tweaks.Change> parse(String id, String json, String source) {
        JsonObject root = JsonObject.parse(json, source);
        if (root.integer("schema", SCHEMA) != SCHEMA) throw new IllegalArgumentException(source + ": schema must be " + SCHEMA);
        // "requires": mod ids that must all be loaded, or the whole file is skipped (compat tables for other mods).
        if (root.has("requires")) for (String mod : root.strings("requires")) if (!Plugins.modLoaded(mod)) return List.of();
        List<Tweaks.Change> out = new ArrayList<>();
        List<JsonObject> changes = root.has("type") ? List.of(root) : root.objects("changes");
        for (int n = 0; n < changes.size(); n++) out.add(change(id + "/" + n, changes.get(n)));
        return out;
    }

    static Tweaks.Change change(String id, JsonObject o) {
        String type = o.string("type");
        return switch (type) {
            case "vat_add" -> new Tweaks.VatAdd(new VatRecipes.Recipe(o.string("id", id), fluid(o, "fluid1"), o.has("fluid2") ? fluid(o, "fluid2") : null,
                    o.string("item", null), o.has("item") ? o.integer("count", 1) : 0, o.has("output_fluid") ? fluid(o, "output_fluid") : null,
                    o.has("output_item") ? stack(o.object("output_item")) : null, Math.max(1, o.integer("time", 1)), "data"));
            case "vat_remove" -> {
                if (!o.has("output_item") && !o.has("output_fluid")) throw new IllegalArgumentException(o.path() + ": needs output_item or output_fluid");
                yield new Tweaks.VatRemove(o.string("output_item", null), o.string("output_fluid", null));
            }
            case "melting_add" -> new Tweaks.MeltAdd(new Melting.Recipe(o.string("input"), o.integer("temperature"), o.string("fluid"), o.integer("amount"),
                    o.string("bonus", null), o.has("bonus") ? o.integer("bonus_chance", 1) : 0, List.of(), List.of()));
            case "melting_remove" -> new Tweaks.MeltRemove(o.string("input"));
            case "fuel_add" -> new Tweaks.FuelAdd(fuelKey(o), o.has("fluid"), new CrucibleFuels.Fuel(o.integer("max_temperature"),
                    o.integer("temperature_per_tick"), Math.max(1, o.integer("ticks_per_step", 1)), 0, 0));
            case "fuel_remove" -> new Tweaks.FuelRemove(fuelKey(o), o.has("fluid"));
            case "anvil_add" -> new Tweaks.AnvilAdd(new Anvil.Recipe(o.string("input"), o.string("output"), o.integer("count", 1), Math.max(1, o.integer("hits"))));
            case "anvil_remove" -> new Tweaks.AnvilRemove(o.string("input"));
            case "casting_add" -> new Tweaks.CastAdd(kind(o), o.string("fluid"), o.string("result"), o.integer("amount", kind(o).amount()));
            case "casting_remove" -> new Tweaks.CastRemove(kind(o), o.string("result"));
            case "loot_add" -> new Tweaks.LootAdd(new FishingRules.Loot(o.string("item"), o.number("chance"), rarity(o), dimension(o), rod(o)),
                    o.bool("exact", false));
            case "loot_remove" -> new Tweaks.LootRemove(o.string("item"));
            default -> throw new IllegalArgumentException(o.path() + ": unknown type " + type);
        };
    }

    private static FluidAmount fluid(JsonObject o, String key) {
        JsonObject f = o.object(key);
        return new FluidAmount(f.string("fluid"), f.integer("amount"));
    }

    private static Stack stack(JsonObject o) {
        return new Stack(o.string("item"), o.integer("count", 1), java.util.Map.of());
    }

    /** An item id, "#tag" (from "tag") or a fluid id or name. */
    private static String fuelKey(JsonObject o) {
        if (o.has("fluid")) return o.string("fluid");
        if (o.has("tag")) return "#" + o.string("tag");
        return o.string("item");
    }

    private static Casting.Kind kind(JsonObject o) {
        return Casting.Kind.valueOf(o.string("caster").toUpperCase(Locale.ROOT));
    }

    private static FishingRules.Rarity rarity(JsonObject o) {
        return FishingRules.Rarity.valueOf(o.string("rarity").toUpperCase(Locale.ROOT));
    }

    /** "overworld_like", "nether", "end", or "any"/absent for every dimension. */
    private static String dimension(JsonObject o) {
        String d = o.string("dimension", "any");
        if (d.equals("any")) return null;
        if (!List.of("overworld_like", "nether", "end").contains(d)) throw new IllegalArgumentException(o.path() + ".dimension: unknown " + d);
        return d;
    }

    /** The lowest rod tier that catches it; "any" for every rod (default DIRE, as the CraftTweaker handlers). */
    private static Species.Rod rod(JsonObject o) {
        String r = o.string("rod", "dire");
        return r.equals("any") ? null : Species.Rod.valueOf(r.toUpperCase(Locale.ROOT));
    }
}
