package mariculture.domain.fish;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A fertilised egg: for every gene, the four parental alleles [father upper, father lower,
 * mother upper, mother lower] (legacy FishyHelper.generateEgg), the remaining fertility, how many
 * of each sex have hatched, and environment tags counted when it was laid (legacy egg NBT keys
 * such as "blockCopper", now common block tags such as "c:storage_blocks/copper").
 */
public final class Egg {
    public static final int FORMAT = 1;

    private final String[] species;
    private final int[][] alleles;
    private final int fertility;
    private final int males;
    private final int females;
    private final Map<String, Integer> tags;

    Egg(String[] species, int[][] alleles, int fertility, int males, int females, Map<String, Integer> tags) {
        if (species.length != 4 || alleles.length != Genes.ALL.size()) throw new IllegalArgumentException("Malformed egg");
        this.species = species.clone();
        this.alleles = new int[alleles.length][];
        for (int i = 0; i < alleles.length; i++) {
            if (alleles[i].length != 4) throw new IllegalArgumentException("Malformed egg gene " + i);
            this.alleles[i] = alleles[i].clone();
        }
        this.fertility = fertility;
        this.males = males;
        this.females = females;
        this.tags = Map.copyOf(new TreeMap<>(tags));
    }

    public String species(int slot) { return species[slot]; }

    public int allele(Gene g, int slot) { return alleles[Genes.index(g)][slot]; }

    /** Remaining hatch attempts. Legacy stopped only at exactly 0 (BUG-031); here any value <= 0 is spent. */
    public int fertility() { return fertility; }

    public int males() { return males; }

    public int females() { return females; }

    public Map<String, Integer> tags() { return tags; }

    public boolean hasTag(String tag) { return tags.getOrDefault(tag, 0) > 0; }

    public Egg withTag(String tag) {
        Map<String, Integer> t = new TreeMap<>(tags);
        t.merge(tag, 1, Integer::sum);
        return new Egg(species, alleles, fertility, males, females, t);
    }

    Egg withCounts(int fertility, int males, int females) {
        return new Egg(species, alleles, fertility, males, females, tags);
    }

    /** Versioned item data: species_0..3, each gene as a 4-int list, fertility, males, females, tags. */
    public Map<String, Object> toData() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("format", FORMAT);
        for (int i = 0; i < 4; i++) m.put("species_" + i, species[i]);
        for (Gene g : Genes.ALL) {
            int[] a = alleles[Genes.index(g)];
            m.put(g.key(), java.util.List.of(a[0], a[1], a[2], a[3]));
        }
        m.put("eggs_left", fertility); // not "fertility": that is the gene key
        m.put("males", males);
        m.put("females", females);
        if (!tags.isEmpty()) m.put("tags", new java.util.LinkedHashMap<String, Object>(tags));
        return m;
    }

    /** Reads {@link #toData()}; throws IllegalArgumentException for malformed data. */
    public static Egg fromData(Map<String, ?> m) {
        Object format = m.get("format");
        if (format instanceof Number n && n.intValue() > FORMAT) throw new IllegalArgumentException("Egg data format " + n + " is too new");
        String[] sp = new String[4];
        for (int i = 0; i < 4; i++) {
            if (!(m.get("species_" + i) instanceof String s)) throw new IllegalArgumentException("Egg data has no species_" + i);
            sp[i] = s;
        }
        int[][] al = new int[Genes.ALL.size()][];
        for (Gene g : Genes.ALL) {
            if (!(m.get(g.key()) instanceof java.util.List<?> l) || l.size() != 4) throw new IllegalArgumentException("Egg data has no " + g.key());
            int[] a = new int[4];
            for (int i = 0; i < 4; i++) a[i] = ((Number) l.get(i)).intValue();
            al[Genes.index(g)] = a;
        }
        Map<String, Integer> t = new TreeMap<>();
        if (m.get("tags") instanceof Map<?, ?> tm) tm.forEach((k, v) -> t.put((String) k, ((Number) v).intValue()));
        return new Egg(sp, al, num(m.get("eggs_left")), num(m.get("males")), num(m.get("females")), t);
    }

    private static int num(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Egg e)) return false;
        return Arrays.equals(species, e.species) && Arrays.deepEquals(alleles, e.alleles) && fertility == e.fertility
                && males == e.males && females == e.females && tags.equals(e.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(species), Arrays.deepHashCode(alleles), fertility, males, females, tags);
    }
}
