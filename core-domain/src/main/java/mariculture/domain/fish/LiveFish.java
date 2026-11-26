package mariculture.domain.fish;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mariculture.domain.machine.Stack;

/**
 * Live fish and fish egg stacks (fishery/items/ItemFishy, ItemEgg; FishyHelper). A live fish's item data is
 * its genome plus "total_life" and "current_life" in ticks (legacy "Lifespan"/"CurrentLife"); the item is the
 * species' own live item, or live_fish for species added by other data packs.
 */
public final class LiveFish {
    public static final String EGGS = "mariculture:fish_eggs";
    public static final String GENERIC = "mariculture:live_fish";
    public static final String LIFESPAN = "total_life"; // not "lifespan": that is the gene key
    public static final String CURRENT_LIFE = "current_life";

    private static volatile FishRegistry registry = FishRegistry.build(List.of(), List.of(), Set.of());

    private LiveFish() {}

    /** The species and mutations of the running server, replaced on every data reload. */
    public static FishRegistry registry() {
        return registry;
    }

    public static void setRegistry(FishRegistry r) {
        registry = r;
    }

    public static String itemFor(String species) {
        ShippedSpecies.Entry e = ShippedSpecies.byId(species);
        return e != null && species.startsWith("mariculture:") ? "mariculture:" + e.id() : GENERIC;
    }

    /** A live fish stack for a genome, at full life (FishyHelper.makePureFish / makeBredFish). */
    public static Stack stack(Genome genome) {
        Map<String, Object> data = new LinkedHashMap<>(genome.toData());
        int life = genome.expressed(Genes.LIFESPAN, Boosters.NONE);
        data.put(LIFESPAN, life);
        data.put(CURRENT_LIFE, life);
        return Stack.of(itemFor(genome.species()), 1, data);
    }

    public static boolean isLiveFish(Stack s) {
        return !s.isEmpty() && !s.is(EGGS) && s.data().get("species") instanceof String;
    }

    public static boolean isEgg(Stack s) {
        return s.is(EGGS) && s.data().get("species_0") instanceof String;
    }

    /** The genome, or null when the stack is not a readable live fish. */
    public static Genome genome(Stack s) {
        if (!isLiveFish(s)) return null;
        try {
            return Genome.fromData(s.data(), registry::species);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Egg egg(Stack s) {
        if (!isEgg(s)) return null;
        try {
            return Egg.fromData(s.data());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Stack eggStack(Egg egg) {
        return Stack.of(EGGS, 1, egg.toData());
    }

    public static Species species(Stack s) {
        Genome g = genome(s);
        return g == null ? null : registry.species(g.species());
    }

    public static boolean isMale(Stack s) {
        Genome g = genome(s);
        return g != null && g.isMale();
    }

    public static boolean isFemale(Stack s) {
        Genome g = genome(s);
        return g != null && g.isFemale();
    }

    public static int currentLife(Stack s) {
        return s.intData(CURRENT_LIFE, s.intData(LIFESPAN, 0));
    }

    public static int lifespan(Stack s) {
        return s.intData(LIFESPAN, 0);
    }

    public static Stack withLife(Stack s, int life) {
        Map<String, Object> d = new LinkedHashMap<>(s.data());
        d.put(CURRENT_LIFE, life);
        return s.withData(d);
    }

    /** Sets both gender alleles' expressed value (FishDNAGender.addDNA on upper only, as legacy). */
    public static Stack withGender(Stack s, int gender) {
        Map<String, Object> d = new LinkedHashMap<>(s.data());
        d.put(Genes.GENDER.key(), gender);
        return s.withData(d);
    }

    /** Ticks a raw fish left over from a dead live fish lies on land (FisheryEventHandler.onFishDeath: 6000). */
    public static final int DEAD_FISH_LIFESPAN = 6000;

    /**
     * FisheryEventHandler.onFishDeath: a live fish item about to despawn stays alive in water (unless it is a lava
     * fish); otherwise it becomes its raw form. Returns null to keep the live fish, or the raw stack.
     */
    public static Stack onExpire(Stack s, boolean inWater) {
        Species sp = species(s);
        if (sp == null) return null;
        if (!sp.lava() && inWater) return null;
        return Stack.of(mariculture.domain.fishing.FishingRules.rawForm(sp.id()), s.count());
    }
}
