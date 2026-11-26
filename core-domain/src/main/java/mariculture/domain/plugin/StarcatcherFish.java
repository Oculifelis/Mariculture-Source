package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.Species;

/**
 * Mariculture fish in Starcatcher's pools and guide (compat study SC1, PLUG-028): one
 * {@code data/mariculture/starcatcher/fish/<species>.json} per species that has its own raw item and can be caught.
 * Only Starcatcher reads that folder, so the files load harmlessly without it. Its hook hands out the raw (dead) fish.
 *
 * <p>The schema is the same in Starcatcher 3.1.4.6 (1.20.1), 3.3.10 (1.21.1) and 3.3.10.2 (26.1.2). Rod tier picks
 * the rarity and the minigame template Starcatcher's own fish of that rarity use; salinity picks the biomes (saline
 * in oceans, brackish on beaches and swamps, fresh anywhere but oceans); catch dimension, active time and lava carry
 * over. Biome tags are vanilla ones, present on all three versions.
 */
public final class StarcatcherFish {
    private StarcatcherFish() {}

    /**
     * Whether a species gets a Starcatcher entry.
     * A5: species limited to biomes or another mod's fluid (catch.biomes, catch.fluid; the Alex's Caves fish) stay out:
     * Starcatcher and Tide reject a fish entry naming biomes or fluids that are not installed, and those fish already
     * bite on Mariculture rods only.
     */
    public static boolean eligible(Species sp, String rawItem) {
        return rawItem.startsWith("mariculture:") && !sp.catchChance().alwaysZero() && sp.catchBiomes().isEmpty() && sp.catchFluid() == null;
    }

    public static String rarity(Species.Rod rod) {
        return switch (rod) {
            case OLD -> "common";
            case GOOD -> "uncommon";
            case DIRE -> "rare";
            default -> "epic";
        };
    }

    public static Map<String, Object> json(Species sp, String rawItem) {
        String rarity = rarity(sp.rod());
        boolean hard = rarity.equals("rare") || rarity.equals("epic");
        Map<String, Object> j = new LinkedHashMap<>();
        j.put("base_chance", switch (rarity) { case "common" -> 5; case "epic" -> 10; default -> 6; });
        j.put("catch_info", m("item", m("identifier", rawItem)));
        Map<String, Object> spot = m("color_as_int", -15732736, "hitbox_size_in_pixels", 22, "is_flip", false, "moving_rate", 1.0,
                "reward", hard ? 20 : 15, "sweetspot_type", "starcatcher:normal",
                "texture_path", "starcatcher:textures/gui/minigame/spots/normal.png", "vanishing_rate", 0.0);
        j.put("difficulty", m("decay", 1.0, "hp", rarity.equals("common") ? 75 : 100, "missPenalty", hard ? 20 : 5,
                "modifiers", List.of(), "speed", hard ? 11 : 7, "sweetspots", List.of(spot, spot)));
        j.put("has_guide_entry", true);
        j.put("rarity", rarity);
        j.put("restrictions", restrictions(sp));
        j.put("size_and_weight", m("average_size_cm", 40.0, "average_weight_grams", 1500.0, "deviation_size_cm", 15.0,
                "deviation_weight_grams", 700.0, "golden_chance", 0.02));
        j.put("skips_minigame", false);
        j.put("textures", m("buttons", "starcatcher:textures/gui/minigame/buttons.png", "handle", "starcatcher:textures/gui/minigame/handle.png",
                "rod", "starcatcher:textures/gui/minigame/rod.png", "tank", "starcatcher:textures/gui/minigame/tanks/" + tank(sp) + ".png",
                "treasure", "starcatcher:textures/gui/minigame/treasure.png", "wheels", "starcatcher:textures/gui/minigame/wheels.png"));
        return j;
    }

    /** Starcatcher's dimension entry for a species' catch dimension ("not_nether" fishes best in the overworld). */
    public static String dimension(Species sp) {
        return switch (sp.catchDimension()) {
            case "nether" -> "the_nether";
            case "end" -> "the_end";
            default -> "overworld";
        };
    }

    static String tank(Species sp) {
        return switch (dimension(sp)) {
            case "the_nether" -> "nether";
            case "the_end" -> "end_void";
            default -> sp.lava() ? "lava_overworld" : "surface";
        };
    }

    static List<Object> restrictions(Species sp) {
        List<Object> r = new ArrayList<>();
        String dim = dimension(sp);
        r.add(m("type", "starcatcher:dimension", "dimension_entry", dim, "translation_override", "dimension.minecraft." + dim));
        if (dim.equals("overworld") && !sp.lava() && !sp.ignoresSalinity()) {
            if (sp.salinityBase() == Salinity.SALINE) r.add(m("type", "starcatcher:biome", "biomes", List.of("#minecraft:is_ocean"), "blacklist", List.of()));
            else if (sp.salinityBase() == Salinity.BRACKISH) r.add(m("type", "starcatcher:biome",
                    "biomes", List.of("#minecraft:is_beach", "minecraft:swamp", "minecraft:mangrove_swamp"), "blacklist", List.of()));
            else r.add(m("type", "starcatcher:biome", "biomes", List.of(), "blacklist", List.of("#minecraft:is_ocean")));
        }
        switch (sp.activeTime()) {
            case DAY -> r.add(m("type", "starcatcher:daytime_restriction", "ranges", List.of(m("max", 12700, "min", 0)), "translation_override", "gui.guide.day"));
            case NIGHT -> r.add(m("type", "starcatcher:daytime_restriction", "ranges", List.of(m("max", 24000, "min", 12700)), "translation_override", "gui.guide.night"));
            default -> { }
        }
        String fluid = sp.lava() ? "lava" : "water";
        r.add(m("type", "starcatcher:fluid", "fluids", List.of("minecraft:" + fluid), "translation_override", "fluid.starcatcher." + fluid));
        return r;
    }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }
}
