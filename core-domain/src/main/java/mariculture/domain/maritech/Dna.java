package mariculture.domain.maritech;

import java.util.HashMap;
import java.util.Map;

import mariculture.domain.fish.Gene;
import mariculture.domain.fish.Genes;
import mariculture.domain.fish.Genome;
import mariculture.domain.machine.Stack;

/**
 * Fish DNA (maritech/items/ItemFishDNA.java, 1.3.0): one gene and its value, copied from a pure fish by the DNA
 * extractor and written into both sides of a fish by the DNA injector. It wears by one each injection and is used up
 * after 32 (ItemMCDamageable 32).
 */
public final class Dna {
    public static final String ITEM = "mariculture:fish_dna";
    /** The gene key ("species" for the species, otherwise a {@link Gene#key()}), its value, and the wear. */
    public static final String TYPE = "DNAType", VALUE = "DNAValue", SPECIES = "DNASpecies", WEAR = "Wear";
    public static final String SPECIES_TYPE = "species";
    public static final int USES = 32;

    private Dna() {}

    public static Stack ofGene(Gene gene, int value) {
        Map<String, Object> d = new HashMap<>();
        d.put(TYPE, gene.key());
        d.put(VALUE, value);
        return Stack.of(ITEM, 1, d);
    }

    public static Stack ofSpecies(String species) {
        Map<String, Object> d = new HashMap<>();
        d.put(TYPE, SPECIES_TYPE);
        d.put(SPECIES, species);
        return Stack.of(ITEM, 1, d);
    }

    /** Whether a stack is DNA with a gene on it (an empty DNA item does nothing, legacy hasTagCompound). */
    public static boolean isDna(Stack s) {
        return !s.isEmpty() && s.item().equals(ITEM) && !s.stringData(TYPE, "").isEmpty();
    }

    public static Gene gene(Stack s) {
        return Genes.byKey(s.stringData(TYPE, ""));
    }

    /** ItemFishDNA.add: the fish with the DNA's value on both sides, or null when the DNA is unknown. */
    public static Genome apply(Stack dna, Genome fish) {
        String type = dna.stringData(TYPE, "");
        if (type.equals(SPECIES_TYPE)) {
            String sp = dna.stringData(SPECIES, "");
            return sp.isEmpty() ? null : fish.withSpecies(sp, sp);
        }
        Gene g = Genes.byKey(type);
        if (g == null) return null;
        int v = g.clamp(dna.intData(VALUE, g.min()));
        return fish.withGene(g, v, v);
    }

    /** One injection's wear (attemptDamageItem(1)): the DNA afterwards, or EMPTY when used up. */
    public static Stack wear(Stack dna) {
        int wear = dna.intData(WEAR, 0) + 1;
        if (wear >= USES) return Stack.EMPTY;
        Map<String, Object> d = new HashMap<>(dna.data());
        d.put(WEAR, wear);
        return dna.withData(d);
    }

    /** Tooltip (ItemFishDNA.addInformation): the gene and its value in the fish tooltip's words, then the uses left. */
    public static java.util.List<mariculture.domain.fish.FishText.Line> lines(Stack dna) {
        java.util.List<mariculture.domain.fish.FishText.Line> out = new java.util.ArrayList<>();
        if (!isDna(dna)) return out;
        if (dna.stringData(TYPE, "").equals(SPECIES_TYPE)) {
            out.add(new mariculture.domain.fish.FishText.Line("dna.mariculture.species",
                    new Object[] {"lang:" + mariculture.domain.fish.FishText.speciesKey(dna.stringData(SPECIES, ""))}));
        } else {
            Gene g = gene(dna);
            if (g == null) return out;
            int value = dna.intData(VALUE, g.min());
            String range = g.rangeName(value);
            Object shown = g == Genes.LIFESPAN ? (Object) (value / 1200) : value;
            out.add(new mariculture.domain.fish.FishText.Line("gene.mariculture." + g.key(),
                    new Object[] {range == null ? String.valueOf(shown) : "lang:gene.mariculture." + g.key() + "." + range, shown}));
        }
        out.add(new mariculture.domain.fish.FishText.Line("dna.mariculture.uses", new Object[] {USES - dna.intData(WEAR, 0)}));
        return out;
    }

    /** Remaining uses as a 0-13 bar. */
    public static int bar(Stack dna) {
        return Math.round(13f * (USES - dna.intData(WEAR, 0)) / USES);
    }
}
