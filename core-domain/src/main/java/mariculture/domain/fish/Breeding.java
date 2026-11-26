package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mariculture.domain.random.Rng;

/**
 * Fish creation, egg laying and hatching. Legacy: mariculture.fishery.FishyHelper (1.3.0)
 * makePureFish 139-151, makeBredFish 154-211, generateEgg 291-310, attemptToHatchEgg 312-361.
 * Random draws happen in the legacy order so seeded runs are comparable.
 *
 * <p>Intentional fixes (docs/bug_ledger.csv):
 * <ul>
 *   <li>Boosters no longer compound: eggs and children store raw alleles. Legacy stored boosted
 *   reads, so each generation gained twice the booster (+6 temperature tolerance per generation
 *   by default) until the clamp.</li>
 *   <li>An egg with fertility at or below zero is finished (legacy only stopped at exactly 0,
 *   so a stored 0 or negative count hatched forever; BUG-031).</li>
 * </ul>
 */
public final class Breeding {
    /** Hatch chance per attempt is (1 + boost) / 1000 (FishyHelper.attemptToHatchEgg). */
    public static final int HATCH_ROLL = 1000;

    private Breeding() {}

    /** A wild-type fish: both alleles of every gene are the species default; sex drawn per allele. */
    public static Genome makePure(Species species, Rng rng) {
        int n = Genes.ALL.size();
        int[] upper = new int[n], lower = new int[n];
        for (int i = 0; i < n; i++) {
            Gene g = Genes.ALL.get(i);
            upper[i] = species.defaultValue(g, rng);
            lower[i] = species.defaultValue(g, rng);
        }
        return new Genome(species.id(), species.id(), upper, lower);
    }

    /** 1.3.0 crafting reset (ShapelessResetFishRecipe): pure fish only, keeps its sex. */
    public static Genome resetToSpecies(Genome fish, Species species) {
        if (!fish.isPure() || !fish.species().equals(species.id())) return null;
        Genome pure = makePure(species, new Rng() {
            @Override public float nextFloat() { return 0f; }
            @Override public int nextInt(int bound) { return 0; }
        });
        int sex = fish.upper(Genes.GENDER);
        return pure.withGene(Genes.GENDER, sex, sex);
    }

    /** Lays an egg from the feeder's male and female. Fertility is the mode of the four alleles, read with boosters. */
    public static Egg layEgg(Genome male, Genome female, Boosters boosters) {
        int n = Genes.ALL.size();
        int[][] alleles = new int[n][];
        for (int i = 0; i < n; i++) {
            Gene g = Genes.ALL.get(i);
            alleles[i] = new int[] {male.upper(g), male.lower(g), female.upper(g), female.lower(g)};
        }
        String[] species = {male.species(), male.lowerSpecies(), female.species(), female.lowerSpecies()};
        int[] fert = alleles[Genes.index(Genes.FERTILITY)];
        int[] boosted = new int[4];
        for (int i = 0; i < 4; i++) boosted[i] = Genes.FERTILITY.express(fert[i], boosters.of(Genes.FERTILITY));
        return new Egg(species, alleles, mode(boosted), 0, 0, Map.of());
    }

    /** Most frequent value; ties go to the earliest (lib/helpers/AverageHelper.getMode, strict >). */
    static int mode(int[] values) {
        int best = values[0], bestCount = 0;
        for (int v : values) {
            int count = 0;
            for (int w : values) if (w == v) count++;
            if (count > bestCount) { best = v; bestCount = count; }
        }
        return best;
    }

    /**
     * Builds one child from an egg (legacy makeBredFish). Returns null if a parent species is not
     * loaded, in which case the caller treats the attempt like legacy's failed breed (raw fish).
     */
    public static Genome breed(Egg egg, Rng rng, double mutationModifier, FishRegistry registry, Boosters boosters, Set<String> options) {
        int n = Genes.ALL.size();
        // Species first, then numeric genes, matching legacy DNAParts order.
        String s1 = egg.species(rng.nextInt(2));
        String s2 = egg.species(2 + rng.nextInt(2));
        int[] upper = new int[n], lower = new int[n];
        for (int i = 0; i < n; i++) {
            Gene g = Genes.ALL.get(i);
            upper[i] = egg.allele(g, rng.nextInt(2));
            lower[i] = egg.allele(g, 2 + rng.nextInt(2));
        }
        Species sp1 = registry.species(s1), sp2 = registry.species(s2);
        if (sp1 == null || sp2 == null) return null;

        if (!s1.equals(s2)) {
            for (Mutation m : registry.mutationsFor(s1, s2)) {
                if (!m.conditionsMet(egg, options)) continue;
                Species child = registry.species(m.child());
                if (child == null) continue;
                double threshold = m.chancePercent() * 10 * mutationModifier;
                if (rng.nextInt(1000) < threshold) {
                    s1 = child.id();
                    sp1 = child;
                    for (int i = 0; i < n; i++) upper[i] = child.defaultValue(Genes.ALL.get(i), rng);
                }
                if (rng.nextInt(1000) < threshold) {
                    s2 = child.id();
                    sp2 = child;
                    for (int i = 0; i < n; i++) lower[i] = child.defaultValue(Genes.ALL.get(i), rng);
                }
            }
        }

        // Dominance: dominant species is expressed; ties are a coin flip (fishery/fish/dna/FishDNASpecies:30-51).
        if (sp1.dominant() == sp2.dominant() ? rng.nextInt(2) == 1 : sp2.dominant()) {
            String t = s1; s1 = s2; s2 = t;
        }
        for (int i = 0; i < n; i++) {
            Gene g = Genes.ALL.get(i);
            if (g == Genes.GENDER) continue; // re-rolled below; legacy sorting of gender was a coin flip anyway
            boolean d1 = g.isDominant(g.express(upper[i], boosters.of(g)));
            boolean d2 = g.isDominant(g.express(lower[i], boosters.of(g)));
            boolean swap = d1 == d2 ? rng.nextInt(2) == 1 : d2;
            if (swap) { int t = upper[i]; upper[i] = lower[i]; lower[i] = t; }
        }
        upper[Genes.index(Genes.GENDER)] = rng.nextInt(2);
        return new Genome(s1, s2, upper, lower);
    }

    /** Result of one hatch attempt. {@code egg} is null once the egg is used up. */
    public record HatchResult(Egg egg, List<Genome> fish, int failedBreeds) {}

    /**
     * One hatch attempt (legacy attemptToHatchEgg). Costs one fertility; succeeds with probability
     * (1 + birthChanceBoost) / 1000. When the egg runs out it guarantees a male and a female if
     * none hatched. {@code failedBreeds} counts children that could not be built (unknown
     * species); legacy dropped raw cod for those.
     */
    public static HatchResult hatch(Egg egg, Rng rng, int birthChanceBoost, double mutationModifier,
                                    FishRegistry registry, Boosters boosters, Set<String> options) {
        if (egg.fertility() <= 0) return new HatchResult(null, List.of(), 0);
        List<Genome> out = new ArrayList<>();
        int failed = 0;
        int males = egg.males(), females = egg.females();
        int fertility = egg.fertility() - 1;
        if (rng.nextInt(HATCH_ROLL) < 1 + birthChanceBoost) {
            Genome fish = breed(egg, rng, mutationModifier, registry, boosters, options);
            if (fish == null) {
                failed += 2;
            } else {
                out.add(fish);
                if (fish.isMale()) males++; else females++;
            }
        }
        if (fertility <= 0) {
            Genome f = breed(egg, rng, mutationModifier, registry, boosters, options);
            if (f == null) {
                failed++;
            } else {
                if (males <= 0) out.add(f.withGene(Genes.GENDER, Fish.MALE, f.lower(Genes.GENDER)));
                Genome f2 = breed(egg, rng, mutationModifier, registry, boosters, options);
                if (f2 != null && females <= 0) out.add(f2.withGene(Genes.GENDER, Fish.FEMALE, f2.lower(Genes.GENDER)));
            }
            return new HatchResult(null, out, failed);
        }
        return new HatchResult(egg.withCounts(fertility, males, females), out, failed);
    }
}
