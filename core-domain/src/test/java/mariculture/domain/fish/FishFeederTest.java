package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.fishery.FishFeeder;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;
import mariculture.domain.random.Rng;

/** TileFeeder behaviour through the domain machine. */
class FishFeederTest {
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

    private Stack fish(String species, int gender) {
        Genome g = Breeding.makePure(registry.species(species), Rng.of(new Random(1)));
        return LiveFish.stack(g.withGene(Genes.GENDER, gender, gender));
    }

    @Test
    void fishFoodItemsBecomeFishFoodFluid() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(1);
        inv.set(FishFeeder.MALE, fish("mariculture:cod", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:cod", Fish.FEMALE));
        world.tick(feeder); // first tick scans the water and sizes the tank
        inv.set(FishFeeder.FLUID_IN, Stack.of("mariculture:fish_meal", 3));
        world.tick(feeder);
        assertEquals(Fluids.FISH_FOOD, feeder.tanks().get(0).fluid());
        assertEquals(12, feeder.tanks().get(0).amount());
        assertEquals(2, inv.get(FishFeeder.FLUID_IN).count());
    }

    @Test
    void workingPairAgesAndTheFemaleLeavesAnEgg() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(7);
        world.temperature = 11; // cod: base 11, saline
        inv.set(FishFeeder.MALE, fish("mariculture:cod", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:cod", Fish.FEMALE));
        world.tick(feeder);
        feeder.tanks().get(0).fill(Fluids.FISH_FOOD, 100000, false);
        int startLife = LiveFish.currentLife(inv.get(FishFeeder.MALE));
        for (int i = 0; i < 400; i++) world.tick(feeder);
        assertTrue(LiveFish.currentLife(inv.get(FishFeeder.MALE)) < startLife, "fish age while working");
        for (int i = 0; i < 30000 && !inv.get(FishFeeder.FEMALE).isEmpty(); i++) world.tick(feeder);
        assertTrue(inv.get(FishFeeder.FEMALE).isEmpty(), "female died");
        boolean egg = false, raw = false;
        for (int s = 7; s < 13; s++) {
            if (LiveFish.isEgg(inv.get(s))) egg = true;
            if (inv.get(s).is("minecraft:cod")) raw = true;
        }
        for (Stack d : world.drops) {
            if (LiveFish.isEgg(d)) egg = true;
            if (d.is("minecraft:cod")) raw = true;
        }
        assertTrue(egg, "a dead female leaves an egg");
        assertTrue(raw, "a dead fish leaves its raw form");
    }

    @Test
    void noFoodNoWork() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(3);
        world.temperature = 11;
        inv.set(FishFeeder.MALE, fish("mariculture:cod", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:cod", Fish.FEMALE));
        int life = LiveFish.currentLife(inv.get(FishFeeder.MALE));
        for (int i = 0; i < 1000; i++) world.tick(feeder);
        assertEquals(life, LiveFish.currentLife(inv.get(FishFeeder.MALE)));
    }

    @Test
    void wrongWaterGivesNoTank() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(3);
        world.fluidEverywhere = "minecraft:lava";
        inv.set(FishFeeder.MALE, fish("mariculture:cod", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:cod", Fish.FEMALE));
        world.tick(feeder);
        assertEquals(0, feeder.tanks().get(0).capacity(), "no cod-friendly water, no tank");
    }

    @Test
    void aStargazerPairGuardsItsTankUntilRemovedOrConfiguredOff() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(3);
        inv.set(FishFeeder.MALE, fish("mariculture:stargazer", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:stargazer", Fish.FEMALE));
        world.tick(feeder);
        assertNotNull(world.guard, "BUG-004: a Stargazer feeder guards its tank");
        assertEquals(-5 - 16, world.guard[0], "tank water from -5, widened by 16");
        mariculture.domain.config.Settings.STARGAZER_BLOCKS_SPAWNS.set(false);
        try {
            world.time = 20;
            world.tick(feeder);
            assertNull(world.guard, "configured off");
        } finally {
            mariculture.domain.config.Settings.reset();
        }
        world.time = 40;
        world.tick(feeder);
        assertNotNull(world.guard);
        feeder.removed(world);
        assertNull(world.guard, "breaking the feeder lifts the guard");
    }

    @Test
    void feedersWithoutAStargazerGuardNothing() {
        FishFeeder feeder = new FishFeeder();
        ListInventory inv = new ListInventory(feeder);
        FakeWorld world = new FakeWorld(3);
        inv.set(FishFeeder.MALE, fish("mariculture:cod", Fish.MALE));
        inv.set(FishFeeder.FEMALE, fish("mariculture:cod", Fish.FEMALE));
        world.tick(feeder);
        assertNull(world.guard);
    }
}
