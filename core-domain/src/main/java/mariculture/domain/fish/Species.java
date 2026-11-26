package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mariculture.domain.content.Ids;
import mariculture.domain.json.JsonObject;
import mariculture.domain.random.Rng;

/**
 * A fish species as loaded from {@code data/<ns>/mariculture/fish_species/<id>.json} (schema 1).
 * The shipped files are produced from the traced legacy classes by tools/convert_fish_research.py;
 * data packs may add or override species without Java changes.
 *
 * <p>{@code catch.biomes} (biome ids or {@code #tags}) limits where the species bites; empty means anywhere its
 * water suits it. {@code catch.fluid} (a fluid id or {@code #tag}) makes it bite only in that fluid, such as Alex's
 * Caves acid; without it the species bites in water (or lava for lava fish), never in another mod's fluid.
 *
 * <p>{@code abilities} lists typed tank abilities ({@link FishAbility}); {@code breeding} lists needs a pair must meet
 * to lay an egg ({@link BreedingCondition}).
 */
public record Species(
        String id, int legacyId, boolean dominant,
        int temperatureBase, int temperatureTolerance, Salinity salinityBase, int salinityTolerance,
        int lifespanMinutes, int fertility, int productivity, int foodConsumption, boolean requiresFood,
        int waterRequired, List<String> validFluids, boolean ignoresSalinity, boolean lava, ActiveTime activeTime,
        int light, Map<String, Integer> areaOfEffect,
        Rod rod, String catchDimension, CatchChance catchChance, CatchChance aliveChance,
        int fishOilMb, int fishMeal, Food food, List<Product> products, Liquified liquifiedProduct,
        List<String> effects, String requiresItemTag, List<String> catchBiomes, String catchFluid,
        List<FishAbility> abilities, List<BreedingCondition> breeding) {

    public static final int SCHEMA = 1;

    public enum ActiveTime { ANY, DAY, NIGHT;
        public boolean allows(boolean daytime) {
            return this == ANY || (this == DAY) == daytime;
        }
    }

    /**
     * Rod tiers in legacy order (mariculture.api.fishery.RodType).
     * MANA is Botania's living rod (PluginBotania.java:37), BLOOD Blood Magic's bound rod (PluginBloodMagic.java:44),
     * INFUSED Redstone Arsenal's flux-infused rod (PluginRedstoneArsenal.java:29).
     */
    public enum Rod { OLD, GOOD, DIRE, SUPER, FLUX, MANA, BLOOD, INFUSED }

    public record Food(boolean edible, int hunger, float saturation) {}

    /**
     * {@code chancePercent} is used as legacy did: success if nextInt(1000) < (int)(chance * 10). {@code data} is item
     * data put on the made stack; a metal droplet product carries its fluid there ({@code fluid}, {@code amount},
     * {@code color} in the JSON, see {@link mariculture.domain.fishery.Droplets#metal}).
     */
    public record Product(String item, double chancePercent, int count, Map<String, Object> data) {
        public Product {
            data = data == null ? Map.of() : Map.copyOf(data);
        }

        public Product(String item, double chancePercent, int count) {
            this(item, chancePercent, count, Map.of());
        }

        public int rollPerMille() {
            return (int) (chancePercent * 10);
        }
    }

    /** Crucible melting by-product, produced when nextInt(oneIn) == 0 (TileCrucible:279). */
    public record Liquified(String item, int oneIn) {}

    public Species {
        Ids.full(id);
        validFluids = List.copyOf(validFluids);
        areaOfEffect = Map.copyOf(areaOfEffect);
        products = List.copyOf(products);
        effects = List.copyOf(effects);
        catchBiomes = List.copyOf(catchBiomes);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        breeding = breeding == null ? List.of() : List.copyOf(breeding);
        if (products.size() > 15) throw new IllegalArgumentException(id + ": at most 15 products (legacy FishSpecies:174)");
        if (lifespanMinutes < 1 || fertility < 1 || waterRequired < 1) throw new IllegalArgumentException(id + ": lifespan, fertility and water must be positive");
    }

    /** Default allele value for a numeric gene (legacy getDNAFromSpecies). Gender draws from rng. */
    public int defaultValue(Gene gene, Rng rng) {
        if (gene == Genes.GENDER) return rng.nextInt(2);
        if (gene == Genes.LIFESPAN) return lifespanMinutes * 20 * 60;
        if (gene == Genes.FERTILITY) return fertility;
        if (gene == Genes.PRODUCTIVITY) return productivity;
        if (gene == Genes.WATER_REQUIRED) return waterRequired;
        if (gene == Genes.FOOD_USAGE) return foodConsumption;
        if (gene == Genes.SALINITY_TOLERANCE) return Math.max(0, salinityTolerance);
        if (gene == Genes.TEMPERATURE_TOLERANCE) return Math.max(0, temperatureTolerance);
        if (gene.key().startsWith("area_")) return areaOfEffect.getOrDefault(gene.key().substring(5), 0);
        throw new IllegalArgumentException("No default for gene " + gene);
    }

    public static Species parse(String id, String json, String source) {
        JsonObject o = JsonObject.parse(json, source);
        int schema = o.integer("schema");
        if (schema != SCHEMA) throw new IllegalArgumentException(source + ": unsupported schema " + schema + " (expected " + SCHEMA + ")");
        JsonObject temp = o.object("temperature");
        JsonObject sal = o.object("salinity");
        JsonObject aoe = o.object("area_of_effect");
        Map<String, Integer> area = new java.util.LinkedHashMap<>();
        for (String dir : List.of("up", "down", "north", "east", "south", "west")) area.put(dir, aoe.integer(dir, 0));
        JsonObject c = o.object("catch");
        JsonObject food = o.object("food");
        List<Product> products = new ArrayList<>();
        for (JsonObject p : o.objects("products")) products.add(product(p));
        return new Species(id, o.integer("legacy_id", -1), o.bool("dominant", false),
                temp.integer("base"), temp.integer("tolerance"), Salinity.byName(sal.string("base")), sal.integer("tolerance"),
                o.integer("lifespan_minutes"), o.integer("fertility"), o.integer("productivity", 1), o.integer("food_consumption", 1),
                o.bool("requires_food", true), o.integer("water_required", 15), o.strings("valid_fluids"), o.bool("ignores_salinity", false),
                o.bool("lava", false), ActiveTime.valueOf(o.string("active_time", "any").toUpperCase(Locale.ROOT)), o.integer("light", 0), area,
                Rod.valueOf(c.string("rod").toUpperCase(Locale.ROOT)), c.string("dimension", "overworld_like"),
                CatchChance.parse(c, "chance_percent"), CatchChance.parse(c, "alive_percent"),
                o.integer("fish_oil_mb", 0), o.integer("fish_meal", 1),
                new Food(food.bool("edible", true), food.integer("hunger", 0), (float) food.number("saturation")),
                products, o.has("liquified_product") ? liquified(o.object("liquified_product")) : null,
                o.strings("effects"), o.string("requires_item_tag", null), c.strings("biomes"), c.string("fluid", null),
                FishAbility.parseAll(o.objects("abilities")), BreedingCondition.parseAll(o.objects("breeding")));
    }

    private static Product product(JsonObject p) {
        double chance = p.number("chance_percent");
        if (chance < 0 || chance > 100) throw new IllegalArgumentException(p.path() + ": chance_percent must be 0-100");
        String item = Ids.full(p.string("item"));
        if (p.has("components")) {
            // Other mods' data components by full id (Thaumaturge's crystal aspect); the loader decodes them.
            JsonObject c = p.object("components");
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            for (String k : c.keys()) {
                if (k.indexOf(':') < 0) throw new IllegalArgumentException(c.path() + ": component ids need a namespace: " + k);
                data.put(k, c.raw(k));
            }
            return new Product(item, chance, p.integer("count", 1), data);
        }
        if (!p.has("fluid")) return new Product(item, chance, p.integer("count", 1));
        if (!item.equals(mariculture.domain.fishery.Droplets.METAL)) {
            throw new IllegalArgumentException(p.path() + ": fluid is only for " + mariculture.domain.fishery.Droplets.METAL);
        }
        int amount = p.integer("amount", mariculture.domain.fishery.Droplets.NUGGET_MB);
        if (amount < 1) throw new IllegalArgumentException(p.path() + ": amount must be at least 1");
        int color = Integer.parseInt(p.string("color", "#FFFFFF").replace("#", ""), 16);
        return new Product(item, chance, p.integer("count", 1),
                mariculture.domain.fishery.Droplets.metal(Ids.full(p.string("fluid")), amount, color).data());
    }

    private static Liquified liquified(JsonObject p) {
        int oneIn = p.integer("one_in");
        if (oneIn < 1) throw new IllegalArgumentException(p.path() + ": one_in must be at least 1");
        return new Liquified(Ids.full(p.string("item")), oneIn);
    }
}
