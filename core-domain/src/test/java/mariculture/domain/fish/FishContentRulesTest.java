package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.random.ScriptedRng;

/** Shipped species table, live fish data and species effects. */
class FishContentRulesTest {
    @Test
    void shippedSpeciesMatchTheDataFiles() {
        List<Species> species = FishData.species();
        assertEquals(species.size(), ShippedSpecies.ALL.size());
        for (Species s : species) {
            ShippedSpecies.Entry e = ShippedSpecies.byId(s.id());
            assertNotNull(e, s.id());
            if (s.food().edible()) {
                assertEquals(s.food().hunger(), e.hunger(), s.id());
                assertEquals(s.food().saturation(), e.saturation(), 1e-6, s.id());
            } else {
                assertFalse(e.edible(), s.id());
            }
        }
    }

    @Test
    void oreGatedMetalFishNeedTheirIngotTag() {
        assertNull(FishData.registry(Set.of()).species("mariculture:tinfish"));
        assertNotNull(FishData.registry(Set.of("c:ingots/tin")).species("mariculture:tinfish"));
    }

    @Test
    void liveFishDataRoundTrips() {
        FishRegistry r = FishData.registry();
        LiveFish.setRegistry(r);
        Genome g = Breeding.makePure(r.species("mariculture:koi"), new FixedRng(0));
        var stack = LiveFish.stack(g);
        assertEquals("mariculture:koi", stack.item());
        assertEquals(g, LiveFish.genome(stack));
        assertEquals(LiveFish.lifespan(stack), LiveFish.currentLife(stack));
        assertNull(LiveFish.onExpire(stack, true), "alive in water");
        assertEquals("mariculture:raw_koi", LiveFish.onExpire(stack, false).item());
    }

    @Test
    void swappedPotionArgumentsAreFixed() {
        var lung = (FishEffects.Potion) FishEffects.eaten("mariculture:lungfish", true).get(0);
        assertEquals(2000, lung.ticks());
        assertEquals(0, lung.amplifier());
    }

    @Test
    void nightfishOnlyAtNight() {
        assertTrue(FishEffects.eaten("mariculture:nightfish", true).isEmpty());
        assertFalse(FishEffects.eaten("mariculture:nightfish", false).isEmpty());
    }

    @Test
    void clownfishKeepsTheLegacyHorseOrChickenRoll() {
        // nextInt(10)=0 passes, coord 2, cow roll 0 short-circuits, horse roll 0 gives a horse.
        var a = FishEffects.world("mariculture:clownfish", new ScriptedRng().ints(0, 2, 0, 0), 5);
        assertEquals("minecraft:horse", ((FishEffects.SpawnMob) a.get(0)).entity());
        var b = FishEffects.world("mariculture:clownfish", new ScriptedRng().ints(0, 2, 1, 1, 1, 3), 5);
        assertEquals("minecraft:chicken", ((FishEffects.SpawnMob) b.get(0)).entity());
    }
}
