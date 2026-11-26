package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Breeding;
import mariculture.domain.fish.FishData;
import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.Genes;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Species;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;
import mariculture.domain.random.Rng;

class DnaMachinesTest {
    private FishRegistry registry;

    @BeforeEach
    void load() {
        registry = FishData.registry();
        LiveFish.setRegistry(registry);
    }

    @AfterEach
    void reset() {
        LiveFish.setRegistry(FishRegistry.build(java.util.List.of(), java.util.List.of(), java.util.Set.of()));
    }

    private Genome pure(String id, long seed) {
        Species s = registry.species(id);
        assertNotNull(s, id);
        return Breeding.makePure(s, Rng.of(new Random(seed)));
    }

    private Species any(int n) {
        return registry.allSpecies().stream().skip(n).findFirst().orElseThrow();
    }

    @Test
    void extractionOnlyCopiesGenesBothSidesAgreeOn() {
        Genome fish = pure(any(0).id(), 1).withGene(Genes.LIFESPAN, 100, 200);
        Rng rng = Rng.of(new Random(4));
        boolean species = false;
        for (int i = 0; i < 500; i++) {
            Stack dna = DnaExtractor.extract(fish, rng);
            assertNotNull(dna, "20 rounds always find something");
            assertTrue(Dna.isDna(dna));
            assertFalse("lifespan".equals(dna.stringData(Dna.TYPE, "")), "lifespan differs between the sides (BUG-069)");
            if (dna.stringData(Dna.TYPE, "").equals(Dna.SPECIES_TYPE)) {
                species = true;
                assertEquals(fish.species(), dna.stringData(Dna.SPECIES, ""));
            } else {
                assertEquals(fish.upper(Dna.gene(dna)), dna.intData(Dna.VALUE, -1));
            }
        }
        assertTrue(species, "the species comes first, 1 in 10 a round");
    }

    @Test
    void extractorUsesOilAndKeepsTheFish() {
        DnaExtractor m = new DnaExtractor();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(2);
        inv.set(DnaExtractor.FISH, LiveFish.stack(pure(any(0).id(), 2)));
        m.tanks().get(0).fill(Fluids.FISH_OIL, 10000, false);
        // 120 FE a tick for 1000 ticks is more than the buffer: it needs a steady supply.
        for (int i = 0; i < 1001; i++) {
            m.battery().set(100000);
            world.tick(m);
        }
        assertEquals(0, m.tanks().get(0).amount(), "10000 mB of fish oil per try");
        assertFalse(inv.get(DnaExtractor.FISH).isEmpty(), "the fish stays");
        int dna = 0;
        for (int s = DnaExtractor.OUT; s < DnaExtractor.SLOTS; s++) if (Dna.isDna(inv.get(s))) dna++;
        assertEquals(1, dna);
    }

    @Test
    void extractorKeepsItsProgressWhileOutOfPower() {
        DnaExtractor m = new DnaExtractor();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(2);
        inv.set(DnaExtractor.FISH, LiveFish.stack(pure(any(0).id(), 2)));
        m.tanks().get(0).fill(Fluids.FISH_OIL, 10000, false);
        // One full battery (100000 FE) runs dry before the 120000 FE an extraction takes.
        m.battery().set(100000);
        for (int i = 0; i < 1100; i++) world.tick(m);
        assertEquals(10000, m.tanks().get(0).amount(), "not done on one battery");
        for (int i = 0; i < 100; i++) world.tick(m);
        m.battery().set(100000);
        for (int i = 0; i < 300; i++) world.tick(m);
        assertEquals(0, m.tanks().get(0).amount(), "a second battery finishes it");
    }

    /** Survival client run 4: a second titanium battery in the slot restarted the extraction from zero. */
    @Test
    void extractorKeepsItsProgressWhenABatteryIsSwappedIn() {
        DnaExtractor m = new DnaExtractor();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(2);
        Stack titanium = Stack.of("mariculture:" + EnergyItem.TITANIUM);
        inv.set(DnaExtractor.FISH, LiveFish.stack(pure(any(0).id(), 2)));
        m.tanks().get(0).fill(Fluids.FISH_OIL, 10000, false);
        inv.set(DnaExtractor.BATTERY_SLOT, EnergyItem.withEnergy(titanium, 100000));
        for (int i = 0; i < 1100; i++) world.tick(m);
        int before = m.gauges()[DnaExtractor.G_PROGRESS];
        assertTrue(before > 500, "most of the way on one battery: " + before);
        inv.set(DnaExtractor.BATTERY_SLOT, Stack.EMPTY);
        for (int i = 0; i < 7; i++) world.tick(m);
        inv.set(DnaExtractor.BATTERY_SLOT, EnergyItem.withEnergy(titanium, 100000));
        for (int i = 0; i < 25; i++) {
            world.tick(m);
            assertTrue(m.gauges()[DnaExtractor.G_PROGRESS] >= before, "progress kept while the new battery charges");
        }
        for (int i = 0; i < 300; i++) world.tick(m);
        assertEquals(0, m.tanks().get(0).amount(), "the second battery finishes it");
    }

    @Test
    void extractorNeedsAPureFishOilAndTenThousandFe() {
        DnaExtractor m = new DnaExtractor();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(2);
        Genome hybrid = pure(any(0).id(), 2).withSpecies(any(0).id(), any(1).id());
        inv.set(DnaExtractor.FISH, LiveFish.stack(hybrid));
        m.tanks().get(0).fill(Fluids.FISH_OIL, 10000, false);
        m.battery().set(100000);
        for (int i = 0; i < 1100; i++) world.tick(m);
        assertEquals(10000, m.tanks().get(0).amount(), "hybrids give nothing");
        inv.set(DnaExtractor.FISH, LiveFish.stack(pure(any(0).id(), 2)));
        m.battery().set(10000);
        for (int i = 0; i < 1100; i++) world.tick(m);
        assertEquals(10000, m.tanks().get(0).amount(), "needs more than 10000 FE");
        assertFalse(m.tanks().get(0).accepts(Fluids.WATER), "fish oil only");
    }

    @Test
    void injectorWritesEveryDnaIntoBothSidesAndWearsIt() {
        DnaInjector m = new DnaInjector();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(3);
        Genome fish = pure(any(0).id(), 3);
        String other = any(1).id();
        inv.set(DnaInjector.FISH, LiveFish.stack(fish));
        inv.set(DnaInjector.DNA, Dna.ofSpecies(other));
        inv.set(DnaInjector.DNA + 1, Dna.ofGene(Genes.FERTILITY, 4321));
        m.tanks().get(0).fill(Fluids.BLOOD, 5000, false);
        m.tanks().get(1).fill(Fluids.FLUX, 5000, false);
        for (int i = 0; i < 5001; i++) {
            m.battery().set(250000);
            world.tick(m);
        }
        Stack out = inv.get(DnaInjector.OUT);
        Genome g = LiveFish.genome(out);
        assertNotNull(g);
        assertEquals(other, g.species());
        assertEquals(other, g.lowerSpecies());
        assertEquals(4321, g.upper(Genes.FERTILITY));
        assertEquals(4321, g.lower(Genes.FERTILITY));
        assertEquals(LiveFish.itemFor(other), out.item(), "the item follows the new species");
        assertTrue(inv.get(DnaInjector.FISH).isEmpty());
        assertEquals(1, inv.get(DnaInjector.DNA).intData(Dna.WEAR, 0));
        assertEquals(0, m.tanks().get(0).amount());
        assertEquals(0, m.tanks().get(1).amount());
    }

    @Test
    void dnaIsUsedUpAfterThirtyTwoInjections() {
        Stack dna = Dna.ofGene(Genes.FERTILITY, 10);
        for (int i = 0; i < Dna.USES - 1; i++) dna = Dna.wear(dna);
        assertTrue(Dna.isDna(dna));
        assertTrue(Dna.wear(dna).isEmpty());
    }

    @Test
    void injectorUsageAndTanks() {
        DnaInjector m = new DnaInjector();
        ListInventory inv = new ListInventory(m);
        FakeWorld world = new FakeWorld(3);
        world.tick(m);
        assertEquals(400, m.usage());
        assertFalse(m.tanks().get(0).accepts(Fluids.WATER));
        inv.set(0, Stack.of("mariculture:basic_storage_upgrade"));
        world.time = 20;
        world.tick(m);
        assertEquals(10000, m.tanks().get(0).capacity());
    }

    @Test
    void fluxRodFishesOnEnergy() {
        assertEquals(Species.Rod.FLUX, Autofisher.rod("mariculture:flux_rod"));
        Stack empty = Stack.of("mariculture:" + EnergyItem.FLUX_ROD);
        assertEquals(0, EnergyItem.stored(empty));
        EnergyItem.Transfer t = EnergyItem.extract(EnergyItem.withEnergy(empty, 250000), 1000, false);
        assertEquals(Autofisher.FLUX_CAST, t.amount(), "100 FE a cast");
    }
}
