package mariculture.domain.fish;

import java.util.List;

import mariculture.domain.fish.Gene.Range;

/**
 * The numeric genes in legacy registration order (fishery/Fish.java:187-263). Order matters: it is
 * the order of random draws during breeding and of DNA Extractor passes. Species is handled
 * separately by {@link Genome} because it is an id, not a number.
 *
 * <p>Ranges and limits: content-source/fish/genes.json (traced from fishery/fish/dna/*.java).
 */
public final class Genes {
    private Genes() {}

    public static final Gene GENDER = new Gene("gender", "Gender", 0, 1, 30, false,
            List.of(new Range("male", 0, 0, true), new Range("female", 1, 1, true)));

    public static final Gene LIFESPAN = new Gene("lifespan", "Lifespan", 1, 360000, 12, false, List.of(
            new Range("miniscule", 0, 3600, true), new Range("tiny", 3601, 6001, false), new Range("short", 6002, 12002, true),
            new Range("medium", 12003, 18003, true), new Range("long", 18004, 24004, false), new Range("epic", 24005, 36005, true),
            new Range("marathon", 36006, 54006, false), new Range("insane", 54007, 100000, true)));

    public static final Gene FERTILITY = new Gene("fertility", "Fertility", 1, Integer.MAX_VALUE, 20, false, List.of(
            new Range("extremehigh", 3000, 5000, false), new Range("veryhigh", 2000, 2999, true), new Range("high", 1000, 1999, false),
            new Range("normal", 500, 999, true), new Range("low", 250, 499, false), new Range("verylow", 100, 249, false),
            new Range("extremelow", 25, 99, true), new Range("pathetic", 0, 24, true)));

    public static final Gene PRODUCTIVITY = new Gene("productivity", "WorkEthic", 0, 2, 30, false, List.of(
            new Range("lazy", 0, 0, false), new Range("normal", 1, 1, true), new Range("hardworker", 2, 2, false),
            new Range("overclocker", 3, 3, false)));

    public static final Gene WATER_REQUIRED = new Gene("water_required", "WaterRequired", 1, 1000, 35, true, List.of(
            new Range("preschool", 0, 14, false), new Range("beginner", 15, 24, false), new Range("basic", 25, 44, false),
            new Range("standard", 45, 63, true), new Range("intermediate", 64, 124, true), new Range("advanced", 125, 199, true),
            new Range("ultimate", 200, 319, false), new Range("draconic", 320, 10000, true)));

    public static final Gene FOOD_USAGE = new Gene("food_usage", "FoodUsage", 0, 10, 20, true, List.of(
            new Range("none", 0, 0, false), new Range("normal", 1, 1, false), new Range("hungry", 2, 2, true),
            new Range("greedy", 3, 3, true)));

    private static Gene area(String dir, String legacyDir) {
        return new Gene("area_" + dir, "AreaOfEffect" + legacyDir, -4, 32, 40, true, List.of(
                new Range("negative", -100, -1, false), new Range("neutral", 0, 0, true), new Range("positive", 1, 100, false)));
    }

    public static final Gene AREA_UP = area("up", "UP");
    public static final Gene AREA_DOWN = area("down", "DOWN");
    public static final Gene AREA_NORTH = area("north", "NORTH");
    public static final Gene AREA_EAST = area("east", "EAST");
    public static final Gene AREA_SOUTH = area("south", "SOUTH");
    public static final Gene AREA_WEST = area("west", "WEST");

    public static final Gene SALINITY_TOLERANCE = new Gene("salinity_tolerance", "SalinityTolerance", 0, 2, 40, true, List.of(
            new Range("intolerant", 0, 0, true), new Range("tolerant", 1, 1, false), new Range("hightolerant", 2, 2, false)));

    public static final Gene TEMPERATURE_TOLERANCE = new Gene("temperature_tolerance", "TemperatureTolerance", 0, 100, 35, true, List.of(
            new Range("fussy", 0, 3, true), new Range("normal", 4, 9, true), new Range("sltolerant", 10, 13, false),
            new Range("tolerant", 14, 17, false), new Range("vtolerant", 18, 25, false), new Range("itolerant", 26, 39, false),
            new Range("etolerant", 40, 100, false)));

    /** Numeric genes in legacy order (species, which comes first in legacy, is separate). */
    public static final List<Gene> ALL = List.of(GENDER, LIFESPAN, FERTILITY, PRODUCTIVITY, WATER_REQUIRED, FOOD_USAGE,
            AREA_UP, AREA_DOWN, AREA_NORTH, AREA_EAST, AREA_SOUTH, AREA_WEST, SALINITY_TOLERANCE, TEMPERATURE_TOLERANCE);

    public static int index(Gene gene) {
        int i = ALL.indexOf(gene);
        if (i < 0) throw new IllegalArgumentException("Unknown gene " + gene);
        return i;
    }

    public static Gene byKey(String key) {
        for (Gene g : ALL) if (g.key().equals(key)) return g;
        return null;
    }

    /** DNA Extractor copy chance for the species "gene" (1 in 10, both versions). */
    public static final int SPECIES_EXTRACTOR_ONE_IN = 10;
}
