package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.random.Rng;

class BreedingTest {
    private final FishRegistry registry = FishData.registry();
    private final Species cod = registry.species("mariculture:cod");
    private final Species tetra = registry.species("mariculture:neon_tetra");
    private final Species nether = registry.species("mariculture:netherfish");

    private static Genome sexed(Genome g, int sex) {
        return g.withGene(Genes.GENDER, sex, sex);
    }

    @Test
    void pureParentsBreedTrue() {
        Rng rng = Rng.of(new Random(7));
        Genome male = sexed(Breeding.makePure(cod, rng), Fish.MALE);
        Genome female = sexed(Breeding.makePure(cod, rng), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.LEGACY_DEFAULTS);
        assertEquals(5000, egg.fertility());
        for (int i = 0; i < 50; i++) {
            Genome child = Breeding.breed(egg, rng, 1.0, registry, Boosters.LEGACY_DEFAULTS, Set.of());
            assertTrue(child.isPure());
            assertEquals("mariculture:cod", child.species());
            assertEquals(cod.defaultValue(Genes.LIFESPAN, null), child.upper(Genes.LIFESPAN));
        }
    }

    @Test
    void mutationOverwritesBothSlotsWhenBothRollsHit() {
        // netherfish x neon_tetra -> anglerfish at 25%: draws of 0 always pass both rolls.
        Genome male = sexed(Breeding.makePure(nether, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(tetra, new FixedRng(0)), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.NONE);
        Genome child = Breeding.breed(egg, new FixedRng(0), 1.0, registry, Boosters.NONE, Set.of());
        assertEquals("mariculture:anglerfish", child.species());
        assertTrue(child.isPure());
    }

    @Test
    void mutationRollAtThresholdFails() {
        // 25% -> threshold 250. A draw of 250 fails, so the child keeps a parent species.
        Genome male = sexed(Breeding.makePure(nether, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(tetra, new FixedRng(0)), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.NONE);
        Genome child = Breeding.breed(egg, new FixedRng(250), 1.0, registry, Boosters.NONE, Set.of());
        assertNotEquals("mariculture:anglerfish", child.species());
        assertNotEquals("mariculture:anglerfish", child.lowerSpecies());
    }

    @Test
    void boostersDoNotCompoundAcrossGenerations() {
        // Legacy stored boosted reads, gaining +6 temperature tolerance per generation by default.
        Rng rng = Rng.of(new Random(99));
        Genome male = sexed(Breeding.makePure(cod, rng), Fish.MALE);
        Genome female = sexed(Breeding.makePure(cod, rng), Fish.FEMALE);
        for (int gen = 0; gen < 10; gen++) {
            Egg egg = Breeding.layEgg(male, female, Boosters.LEGACY_DEFAULTS);
            male = sexed(Breeding.breed(egg, rng, 1.0, registry, Boosters.LEGACY_DEFAULTS, Set.of()), Fish.MALE);
            female = sexed(Breeding.breed(egg, rng, 1.0, registry, Boosters.LEGACY_DEFAULTS, Set.of()), Fish.FEMALE);
        }
        assertEquals(cod.temperatureTolerance(), male.upper(Genes.TEMPERATURE_TOLERANCE));
        assertEquals(cod.temperatureTolerance() + 3, male.expressed(Genes.TEMPERATURE_TOLERANCE, Boosters.LEGACY_DEFAULTS));
    }

    @Test
    void eggGuaranteesBothSexesWhenItRunsOut() {
        Genome male = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.NONE).withCounts(1, 0, 0);
        // Draw 999 fails the hatch roll; the egg is then spent and must yield one male and one female.
        Breeding.HatchResult r = Breeding.hatch(egg, new FixedRng(999), 19, 1.0, registry, Boosters.NONE, Set.of());
        assertNull(r.egg());
        assertEquals(2, r.fish().size());
        assertTrue(r.fish().get(0).isMale());
        assertTrue(r.fish().get(1).isFemale());
    }

    @Test
    void spentEggNeverHatchesAgain() {
        Genome male = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE);
        for (int stored : new int[] {0, -5}) {
            Egg egg = Breeding.layEgg(male, female, Boosters.NONE).withCounts(stored, 0, 0);
            Breeding.HatchResult r = Breeding.hatch(egg, new FixedRng(0), 19, 1.0, registry, Boosters.NONE, Set.of());
            assertNull(r.egg());
            assertTrue(r.fish().isEmpty());
        }
    }

    @Test
    void hatchChanceIsOnePlusBoostPerThousand() {
        Genome male = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.NONE);
        assertEquals(1, Breeding.hatch(egg, new FixedRng(19), 19, 1.0, registry, Boosters.NONE, Set.of()).fish().size());
        Breeding.HatchResult miss = Breeding.hatch(egg, new FixedRng(20), 19, 1.0, registry, Boosters.NONE, Set.of());
        assertTrue(miss.fish().isEmpty());
        assertEquals(4999, miss.egg().fertility());
    }

    @Test
    void metalMutationNeedsBlockTagAndOption() {
        Species shroom = registry.species("mariculture:brown_shroom_fish");
        Species bowfin = registry.species("mariculture:bowfin");
        Genome male = sexed(Breeding.makePure(shroom, new FixedRng(0)), Fish.MALE);
        Genome female = sexed(Breeding.makePure(bowfin, new FixedRng(0)), Fish.FEMALE);
        Egg plain = Breeding.layEgg(male, female, Boosters.NONE);
        Egg tagged = plain.withTag("c:storage_blocks/copper");
        assertNotEquals("mariculture:copperfish", Breeding.breed(tagged, new FixedRng(0), 1, registry, Boosters.NONE, Set.of()).species());
        assertNotEquals("mariculture:copperfish", Breeding.breed(plain, new FixedRng(0), 1, registry, Boosters.NONE, Set.of("enable_metal_fish")).species());
        assertEquals("mariculture:copperfish", Breeding.breed(tagged, new FixedRng(0), 1, registry, Boosters.NONE, Set.of("enable_metal_fish")).species());
    }

    @Test
    void unknownSpeciesFailsBreedInsteadOfInventingAFish() {
        Genome male = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.MALE).withSpecies("other:gone", "other:gone");
        Genome female = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE);
        Egg egg = Breeding.layEgg(male, female, Boosters.NONE);
        assertNull(Breeding.breed(egg, new FixedRng(0), 1, registry, Boosters.NONE, Set.of()));
    }

    @Test
    void resetRecipeOnlyForPureFishAndKeepsSex() {
        Genome f = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE).withGene(Genes.FERTILITY, 3, 3);
        Genome reset = Breeding.resetToSpecies(f, cod);
        assertEquals(cod.fertility(), reset.upper(Genes.FERTILITY));
        assertTrue(reset.isFemale());
        assertNull(Breeding.resetToSpecies(f.withSpecies("mariculture:cod", "mariculture:salmon"), cod));
    }

    @Test
    void modeTiesGoToEarliest() {
        assertEquals(5, Breeding.mode(new int[] {5, 7, 7, 5}));
        assertEquals(7, Breeding.mode(new int[] {5, 7, 7, 1}));
    }

    @Test
    void genomeDataRoundTripsAndKeepsUnknownSpecies() {
        Genome g = sexed(Breeding.makePure(cod, new FixedRng(0)), Fish.FEMALE).withSpecies("mariculture:cod", "other:gone");
        Map<String, Object> data = g.toData();
        assertEquals(g, Genome.fromData(data, registry::species));
        data.put("format", Genome.FORMAT + 1);
        assertThrows(IllegalArgumentException.class, () -> Genome.fromData(data, registry::species));
    }

    @Test
    void missingGeneTakesSpeciesDefault() {
        Genome g = Genome.fromData(Map.of("species", "mariculture:cod"), registry::species);
        assertEquals(cod.fertility(), g.upper(Genes.FERTILITY));
        assertTrue(g.isMale());
    }
}
