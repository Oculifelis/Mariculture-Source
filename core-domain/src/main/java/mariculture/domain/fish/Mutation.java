package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mariculture.domain.json.JsonObject;

/**
 * A species mutation from {@code data/<ns>/mariculture/fish_mutation/*.json} (schema 1). Parent
 * order does not matter (legacy FishMutationHandler.getMutations is symmetric).
 *
 * <p>Conditions: {@code egg_tag} (a block in the tag touched the feeder's water when the egg was
 * laid; legacy RequirementHasTag with ore-dictionary names) and {@code config} (a named server
 * option must be on, e.g. enable_metal_fish, extra_breeding_recipes). Any {@link BreedingCondition} type may be listed
 * too; the feeder checks those when the egg is laid and tags the egg with {@link #layTag()}.
 */
public record Mutation(String id, String parentA, String parentB, String child, double chancePercent,
                       List<String> eggTags, List<String> configOptions, List<BreedingCondition> breeding) {
    public static final int SCHEMA = 1;

    public Mutation {
        eggTags = List.copyOf(eggTags);
        configOptions = List.copyOf(configOptions);
        breeding = breeding == null ? List.of() : List.copyOf(breeding);
        if (chancePercent < 0 || chancePercent > 100) throw new IllegalArgumentException(id + ": chance_percent must be 0-100");
    }

    public boolean matches(String a, String b) {
        return (parentA.equals(a) && parentB.equals(b)) || (parentA.equals(b) && parentB.equals(a));
    }

    public boolean conditionsMet(Egg egg, Set<String> enabledOptions) {
        for (String tag : eggTags) if (!egg.hasTag(tag)) return false;
        if (!breeding.isEmpty() && !egg.hasTag(layTag())) return false;
        return enabledOptions.containsAll(configOptions);
    }

    /** The egg tag a feeder adds when this mutation's breeding conditions held as the egg was laid. */
    public String layTag() {
        return "mariculture:bred/" + id;
    }

    public static Mutation parse(String id, String json, String source) {
        JsonObject o = JsonObject.parse(json, source);
        int schema = o.integer("schema");
        if (schema != SCHEMA) throw new IllegalArgumentException(source + ": unsupported schema " + schema);
        List<String> parents = o.strings("parents");
        if (parents.size() != 2) throw new IllegalArgumentException(source + ": parents must list exactly two species");
        List<String> tags = new ArrayList<>(), options = new ArrayList<>();
        List<BreedingCondition> breeding = new ArrayList<>();
        for (JsonObject c : o.objects("conditions")) {
            switch (c.string("type")) {
                case "egg_tag" -> tags.add(c.string("tag"));
                case "config" -> options.add(c.string("option"));
                default -> {
                    BreedingCondition b = BreedingCondition.parse(c);
                    if (b == null) throw new IllegalArgumentException(c.path() + ": unknown condition type " + c.string("type"));
                    breeding.add(b);
                }
            }
        }
        return new Mutation(id, qualify(parents.get(0)), qualify(parents.get(1)), qualify(o.string("child")), o.number("chance_percent"), tags, options, breeding);
    }

    /** Species references without a namespace mean Mariculture's own species. */
    static String qualify(String id) {
        return id.indexOf(':') >= 0 ? id : "mariculture:" + id;
    }
}
