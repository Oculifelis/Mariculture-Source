package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The loaded species and mutations. Built by the target's data reload listener from the merged
 * data packs; immutable once built so the server thread can share it safely.
 */
public final class FishRegistry {
    private final Map<String, Species> species;
    private final List<Mutation> mutations;
    private final List<String> problems;

    private FishRegistry(Map<String, Species> species, List<Mutation> mutations, List<String> problems) {
        this.species = Collections.unmodifiableMap(species);
        this.mutations = List.copyOf(mutations);
        this.problems = List.copyOf(problems);
    }

    /**
     * Validates cross references. Species that require an absent item tag (legacy optional
     * ore-dictionary fish) are left out, and so are mutations whose parents or child are missing;
     * each omission is reported in {@link #problems()} rather than failing the whole reload.
     *
     * @param presentItemTags item tags with at least one entry, used for {@code requires_item_tag}
     */
    public static FishRegistry build(Collection<Species> allSpecies, Collection<Mutation> allMutations, Set<String> presentItemTags) {
        Map<String, Species> byId = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (Species s : allSpecies) {
            if (s.requiresItemTag() != null && !presentItemTags.contains(s.requiresItemTag())) {
                problems.add("Species " + s.id() + " disabled: no items in #" + s.requiresItemTag());
                continue;
            }
            byId.put(s.id(), s);
        }
        List<Mutation> ok = new ArrayList<>();
        for (Mutation m : allMutations) {
            String missing = !byId.containsKey(m.parentA()) ? m.parentA() : !byId.containsKey(m.parentB()) ? m.parentB()
                    : !byId.containsKey(m.child()) ? m.child() : null;
            if (missing != null) {
                problems.add("Mutation " + m.id() + " disabled: unknown species " + missing);
            } else {
                ok.add(m);
            }
        }
        return new FishRegistry(byId, ok, problems);
    }

    public Species species(String id) {
        return species.get(id);
    }

    public Collection<Species> allSpecies() {
        return species.values();
    }

    public List<Mutation> mutations() {
        return mutations;
    }

    /** Registration order is preserved, which decides which mutation wins (later overwrite earlier). */
    public List<Mutation> mutationsFor(String a, String b) {
        List<Mutation> out = new ArrayList<>();
        for (Mutation m : mutations) if (m.matches(a, b)) out.add(m);
        return out;
    }

    /** One-line summary for the /mariculture info command. */
    public String summary() {
        return species.size() + " fish species, " + mutations.size() + " mutations loaded (" + problems.size() + " disabled entries)";
    }

    public List<String> problems() {
        return problems;
    }
}
