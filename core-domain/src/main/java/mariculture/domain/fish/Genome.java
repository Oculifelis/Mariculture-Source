package mariculture.domain.fish;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable pair of alleles (expressed "upper", recessive "lower") for species and every numeric
 * gene. Values are stored raw; boosters apply only on read. Two fish stack only if their genomes
 * are equal, so equality covers every allele.
 *
 * <p>Species are stored by string id (legacy stored numeric ids from a per-side config file,
 * which could silently change a saved fish's species: BUG-025). An id that is not loaded stays
 * as-is so the fish survives a removed data pack and comes back when the pack returns.
 */
public final class Genome {
    public static final int FORMAT = 1;

    private final String upperSpecies;
    private final String lowerSpecies;
    private final int[] upper;
    private final int[] lower;

    Genome(String upperSpecies, String lowerSpecies, int[] upper, int[] lower) {
        if (upper.length != Genes.ALL.size() || lower.length != Genes.ALL.size()) throw new IllegalArgumentException("Wrong allele count");
        this.upperSpecies = Objects.requireNonNull(upperSpecies);
        this.lowerSpecies = Objects.requireNonNull(lowerSpecies);
        this.upper = upper.clone();
        this.lower = lower.clone();
    }

    public String species() { return upperSpecies; }

    public String lowerSpecies() { return lowerSpecies; }

    /** Legacy FishyHelper.isPure: same species in both slots. */
    public boolean isPure() { return upperSpecies.equals(lowerSpecies); }

    public int upper(Gene g) { return upper[Genes.index(g)]; }

    public int lower(Gene g) { return lower[Genes.index(g)]; }

    public int expressed(Gene g, Boosters b) { return g.express(upper(g), b.of(g)); }

    public int expressedLower(Gene g, Boosters b) { return g.express(lower(g), b.of(g)); }

    public boolean isMale() { return upper(Genes.GENDER) == Fish.MALE; }

    public boolean isFemale() { return upper(Genes.GENDER) == Fish.FEMALE; }

    public Genome withGene(Gene g, int upperValue, int lowerValue) {
        int[] u = upper.clone(), l = lower.clone();
        u[Genes.index(g)] = upperValue;
        l[Genes.index(g)] = lowerValue;
        return new Genome(upperSpecies, lowerSpecies, u, l);
    }

    public Genome withSpecies(String upperValue, String lowerValue) {
        return new Genome(upperValue, lowerValue, upper, lower);
    }

    int[] upperArray() { return upper.clone(); }

    int[] lowerArray() { return lower.clone(); }

    /** Flat, versioned key/value form for item data (legacy-compatible key names). */
    public Map<String, Object> toData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("format", FORMAT);
        m.put("species", upperSpecies);
        m.put("lower_species", lowerSpecies);
        for (Gene g : Genes.ALL) {
            m.put(g.key(), upper(g));
            m.put("lower_" + g.key(), lower(g));
        }
        return m;
    }

    /**
     * Reads {@link #toData()} output. Missing genes take the species default when the species is
     * known (legacy lazy default, FishDNABase.getDNA), otherwise the gene minimum; anything else
     * malformed is an error the caller reports instead of guessing a different fish.
     */
    public static Genome fromData(Map<String, ?> m, java.util.function.Function<String, Species> lookup) {
        Object format = m.get("format");
        if (format != null && ((Number) format).intValue() > FORMAT) {
            throw new IllegalArgumentException("Fish data format " + format + " is newer than this version supports (" + FORMAT + ")");
        }
        Object s = m.get("species");
        if (!(s instanceof String upperS)) throw new IllegalArgumentException("Fish data has no species");
        String lowerS = m.get("lower_species") instanceof String ls ? ls : upperS;
        Species up = lookup.apply(upperS), low = lookup.apply(lowerS);
        int[] u = new int[Genes.ALL.size()], l = new int[Genes.ALL.size()];
        for (int i = 0; i < Genes.ALL.size(); i++) {
            Gene g = Genes.ALL.get(i);
            u[i] = read(m.get(g.key()), g, up, i);
            l[i] = read(m.get("lower_" + g.key()), g, low, i);
        }
        return new Genome(upperS, lowerS, u, l);
    }

    private static int read(Object v, Gene g, Species species, int index) {
        if (v instanceof Number n) return n.intValue();
        if (species == null) return g == Genes.GENDER ? Fish.MALE : g.min();
        // Deterministic: a missing gender reads as male rather than drawing randomness on load.
        return g == Genes.GENDER ? Fish.MALE : species.defaultValue(g, null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Genome other)) return false;
        return upperSpecies.equals(other.upperSpecies) && lowerSpecies.equals(other.lowerSpecies)
                && Arrays.equals(upper, other.upper) && Arrays.equals(lower, other.lower);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upperSpecies, lowerSpecies, Arrays.hashCode(upper), Arrays.hashCode(lower));
    }

    @Override
    public String toString() {
        return "Genome[" + upperSpecies + "/" + lowerSpecies + " " + Arrays.toString(upper) + " / " + Arrays.toString(lower) + "]";
    }
}
