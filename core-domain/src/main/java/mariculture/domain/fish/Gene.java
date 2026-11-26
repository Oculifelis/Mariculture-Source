package mariculture.domain.fish;

import java.util.List;
import java.util.Objects;

/**
 * One numeric gene. Values are stored raw on the fish; the configured booster is added and the
 * result clamped only when the gene is read ({@link #express}). Dominance is decided by which
 * display range the (boosted) value falls in; a value in no range counts as dominant.
 *
 * <p>Legacy: mariculture.api.fishery.fish.FishDNABase / FishDNA and fishery/fish/dna/*.
 */
public final class Gene {
    public record Range(String name, int min, int max, boolean dominant) {}

    private final String key;
    private final String legacyKey;
    private final int min;
    private final int max;
    private final int extractorOneIn;
    private final boolean hidden;
    private final List<Range> ranges;

    Gene(String key, String legacyKey, int min, int max, int extractorOneIn, boolean hidden, List<Range> ranges) {
        this.key = Objects.requireNonNull(key);
        this.legacyKey = legacyKey;
        this.min = min;
        this.max = max;
        this.extractorOneIn = extractorOneIn;
        this.hidden = hidden;
        this.ranges = List.copyOf(ranges);
    }

    public String key() { return key; }

    /** Legacy NBT key of the expressed allele; the recessive one is "lower" + this. */
    public String legacyKey() { return legacyKey; }

    public int min() { return min; }

    public int max() { return max; }

    /** DNA Extractor picks this gene with probability 1/n per pass (1.3.0 values). */
    public int extractorOneIn() { return extractorOneIn; }

    public boolean hidden() { return hidden; }

    public List<Range> ranges() { return ranges; }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }

    /** Read value: stored + booster, clamped (legacy getDNA/sanitize). Long math avoids overflow. */
    public int express(int stored, int booster) {
        long v = (long) stored + booster;
        return (int) Math.max(min, Math.min(max, v));
    }

    /** True when the value counts as dominant (last matching range wins, as in legacy). */
    public boolean isDominant(int value) {
        boolean dominant = true;
        for (Range r : ranges) {
            if (value >= r.min() && value <= r.max()) dominant = r.dominant();
        }
        return dominant;
    }

    /** Display range name for tooltips, or null when the value is in no range. */
    public String rangeName(int value) {
        String name = null;
        for (Range r : ranges) {
            if (value >= r.min() && value <= r.max()) name = r.name();
        }
        return name;
    }

    @Override
    public String toString() {
        return key;
    }
}
