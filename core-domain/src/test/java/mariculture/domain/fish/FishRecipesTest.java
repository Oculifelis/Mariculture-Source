package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.fishery.FishRecipes;
import mariculture.domain.machine.Stack;
import mariculture.domain.random.Rng;

class FishRecipesTest {
    private final FishRegistry registry = FishData.registry();
    private final Species cod = registry.species("mariculture:cod");
    private final Species tetra = registry.species("mariculture:neon_tetra");
    private FishRegistry saved;

    @BeforeEach
    void install() {
        saved = LiveFish.registry();
        LiveFish.setRegistry(registry);
    }

    @AfterEach
    void restore() {
        LiveFish.setRegistry(saved);
    }

    private Egg codEgg() {
        Rng rng = Rng.of(new Random(3));
        Genome male = Breeding.makePure(cod, rng).withGene(Genes.GENDER, Fish.MALE, Fish.MALE);
        Genome female = Breeding.makePure(cod, rng).withGene(Genes.GENDER, Fish.FEMALE, Fish.FEMALE);
        return Breeding.layEgg(male, female, Boosters.LEGACY_DEFAULTS);
    }

    @Test
    void caviarRoundsUpEggsOver650() {
        Egg egg = codEgg();
        assertEquals(5000, egg.fertility());
        assertEquals(egg, LiveFish.egg(LiveFish.eggStack(egg)), "egg data round-trips (BUG-054)");
        Stack out = FishRecipes.caviar(List.of(Stack.EMPTY, LiveFish.eggStack(egg), Stack.EMPTY));
        assertEquals(8, out.count(), "ceil(5000 / 650) = 8; legacy integer division gave 7 (BUG-039)");
        assertEquals(1, FishRecipes.caviar(List.of(LiveFish.eggStack(egg.withCounts(10, 0, 0)))).count());
        assertTrue(FishRecipes.caviar(List.of(LiveFish.eggStack(egg), LiveFish.eggStack(egg))).isEmpty());
    }

    @Test
    void resetOnlyTakesPureFishAndKeepsSex() {
        Genome female = Breeding.makePure(cod, Rng.of(new Random(5))).withGene(Genes.GENDER, Fish.FEMALE, Fish.FEMALE)
                .withGene(Genes.LIFESPAN, 1, 1);
        Stack out = FishRecipes.reset(List.of(LiveFish.stack(female)));
        Genome g = LiveFish.genome(out);
        assertEquals(Fish.FEMALE, g.upper(Genes.GENDER));
        assertEquals(cod.defaultValue(Genes.LIFESPAN, null), g.upper(Genes.LIFESPAN));
        assertEquals(female.withGene(Genes.LIFESPAN, 7, 9), LiveFish.genome(LiveFish.stack(female.withGene(Genes.LIFESPAN, 7, 9))), "fish data round-trips (BUG-054)");

        Genome hybrid = new Genome(cod.id(), tetra.id(), new int[Genes.ALL.size()], new int[Genes.ALL.size()]);
        assertTrue(FishRecipes.reset(List.of(LiveFish.stack(hybrid))).isEmpty());
    }

    @Test
    void scanReportListsActiveAndInactiveGenesAndMarksTheFish() {
        Genome g = Breeding.makePure(cod, Rng.of(new Random(1))).withGene(Genes.LIFESPAN, 24000, 12000);
        Stack fish = LiveFish.stack(g);
        List<FishText.Line> report = FishText.scanReport(fish.data());
        assertEquals("fish.mariculture.scan_species", report.get(0).key());
        FishText.Line life = report.stream().filter(l -> "lang:gene.mariculture.lifespan.title".equals(l.args()[0])).findFirst().orElseThrow();
        assertEquals("20", life.args()[1]);
        assertEquals("10", life.args()[3]);
        assertTrue(FishText.lines(FishText.scanned(fish).data()).stream().noneMatch(l -> l.key().equals("fish.mariculture.unscanned")));
    }
}

