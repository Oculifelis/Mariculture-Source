package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.Species;

/**
 * Mariculture fish on Tide rods and in Tide's journal (compat study T2, PLUG-032): one
 * {@code data/mariculture/fishing/fish/<group>/<species>.json} per species that has its own raw item and can be caught.
 * Only Tide reads that folder, so the files load harmlessly without it. Tide 2.1 on Forge 1.20.1 and NeoForge 1.21.1
 * share the schema; Tide has no 26.1.2 release.
 *
 * <p>Catch dimension picks the dimension and fluid (lava in the nether, the void in the end); salinity picks Tide's
 * saltwater or freshwater condition (brackish fish bite on beaches and swamps); active time carries over; rod tier picks
 * the journal rarity and selection weight. The journal description is one line built from the species data.
 */
public final class TideFish {
    private TideFish() {}

    public static boolean eligible(Species sp, String rawItem) {
        return StarcatcherFish.eligible(sp, rawItem);
    }

    /** Tide journal group, also the data sub-folder. */
    public static String group(Species sp) {
        return switch (StarcatcherFish.dimension(sp)) {
            case "the_nether" -> "lava";
            case "the_end" -> "void";
            default -> sp.lava() ? "lava" : sp.salinityBase() == Salinity.SALINE ? "saltwater" : "freshwater";
        };
    }

    public static String rarity(Species.Rod rod) {
        return switch (rod) {
            case OLD -> "common";
            case GOOD -> "uncommon";
            case DIRE -> "rare";
            default -> "very_rare";
        };
    }

    static double weight(Species.Rod rod) {
        return switch (rod) {
            case OLD -> 40.0;
            case GOOD -> 25.0;
            case DIRE -> 12.0;
            default -> 6.0;
        };
    }

    public static String descriptionKey(String id) {
        return "journal.description.mariculture." + id;
    }

    /** One plain line for the journal, from the species' water, temperature and active time. */
    public static String description(Species sp) {
        String water = sp.lava() ? "lava" : switch (sp.salinityBase()) {
            case SALINE -> "salt water";
            case BRACKISH -> "brackish water";
            default -> "fresh water";
        };
        String time = switch (sp.activeTime()) {
            case DAY -> " It bites by day.";
            case NIGHT -> " It bites at night.";
            default -> "";
        };
        return "A Mariculture fish of " + water + " near " + sp.temperatureBase() + " degrees." + time;
    }

    public static Map<String, Object> json(String id, Species sp, String rawItem) {
        String dim = StarcatcherFish.dimension(sp);
        String group = group(sp);
        List<Object> c = new ArrayList<>();
        c.add(m("type", "tide:dimension", "dimensions", List.of("minecraft:" + dim)));
        c.add(m("type", "tide:fluid", "fluid", dim.equals("the_end") ? "void" : sp.lava() || dim.equals("the_nether") ? "lava" : "water"));
        String location;
        if (dim.equals("overworld") && !sp.lava() && !sp.ignoresSalinity()) {
            if (sp.salinityBase() == Salinity.SALINE) {
                c.add(m("type", "tide:saltwater"));
                location = "saltwater";
            } else if (sp.salinityBase() == Salinity.BRACKISH) {
                c.add(m("type", "tide:found_in", "biomes", List.of("#minecraft:is_beach", "minecraft:swamp", "minecraft:mangrove_swamp")));
                location = "coast";
            } else {
                c.add(m("type", "tide:freshwater"));
                location = "freshwater";
            }
        } else {
            location = switch (group) {
                case "lava" -> "lava";
                case "void" -> "void";
                default -> sp.salinityBase() == Salinity.SALINE ? "saltwater" : "freshwater";
            };
        }
        switch (sp.activeTime()) {
            case DAY -> c.add(m("type", "tide:time_of_day", "ranges", List.of(m("min", 0, "max", 12700))));
            case NIGHT -> c.add(m("type", "tide:time_of_day", "ranges", List.of(m("min", 12700, "max", 24000))));
            default -> { }
        }
        Map<String, Object> j = new LinkedHashMap<>();
        j.put("conditions", c);
        j.put("fish", rawItem);
        j.put("journal_profile", m("description", descriptionKey(id), "group", group,
                "location", "journal.info.location." + location, "rarity", rarity(sp.rod())));
        j.put("selection_weight", weight(sp.rod()));
        j.put("size", m("record_high_cm", 70.0, "typical_high_cm", 50.0, "typical_low_cm", 25.0));
        return j;
    }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }
}
