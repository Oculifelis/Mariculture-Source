package mariculture.domain.fish;

import java.util.HashMap;
import java.util.Map;

/**
 * Config boosters added to a gene whenever it is read (legacy FishMechanics *_BOOSTER options).
 * Legacy defaults: all 0 except temperature tolerance 3 (FishMechanics.java:28-54).
 */
public final class Boosters {
    public static final Boosters LEGACY_DEFAULTS = new Boosters(Map.of(Genes.TEMPERATURE_TOLERANCE, 3));
    public static final Boosters NONE = new Boosters(Map.of());

    private final Map<Gene, Integer> values;

    public Boosters(Map<Gene, Integer> values) {
        this.values = Map.copyOf(new HashMap<>(values));
    }

    public int of(Gene gene) {
        return values.getOrDefault(gene, 0);
    }
}
