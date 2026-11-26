package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;

class MariTechMachinesTest {
    private static final Stack TITANIUM = Stack.of("mariculture:" + EnergyItem.TITANIUM);

    private static Stack charged(int energy) {
        return EnergyItem.withEnergy(TITANIUM, energy);
    }

    @Test
    void poweredMachinesGrowWithRfUpgradesAndChargeFromTheBatterySlot() {
        Incubator inc = new Incubator();
        ListInventory inv = new ListInventory(inc);
        FakeWorld world = new FakeWorld(1);
        assertEquals(50000, inc.battery().capacity());
        inv.set(0, Stack.of("mariculture:advanced_rf_upgrade"));
        inv.set(PoweredMachine.BATTERY_SLOT, charged(100000));
        world.tick(inc);
        assertEquals(100000, inc.battery().capacity(), "50000 + advanced RF 50000");
        assertEquals(PoweredMachine.BATTERY_PULL, inc.battery().stored(), "1000 a tick from the battery slot");
        assertEquals(100000 - PoweredMachine.BATTERY_PULL, EnergyItem.stored(inv.get(PoweredMachine.BATTERY_SLOT)));
        for (int i = 0; i < 200; i++) world.tick(inc);
        assertEquals(100000, inc.battery().stored(), "fills to the upgraded size");
        inv.set(0, Stack.EMPTY);
        world.time = 20;
        world.tick(inc);
        assertEquals(50000, inc.battery().stored(), "taking the upgrade out cuts what is stored");
    }

    @Test
    void savedEnergyAboveTheBaseSizeSurvivesALoad() {
        Incubator inc = new Incubator();
        ListInventory inv = new ListInventory(inc);
        inv.set(0, Stack.of("mariculture:ultimate_rf_upgrade"));
        new FakeWorld(1).tick(inc);
        inc.battery().set(140000);
        Map<String, Object> saved = new HashMap<>();
        inc.save(saved);
        Incubator again = new Incubator();
        ListInventory inv2 = new ListInventory(again);
        inv2.set(0, Stack.of("mariculture:ultimate_rf_upgrade"));
        again.load(saved);
        new FakeWorld(1).tick(again);
        assertEquals(140000, again.battery().stored());
        assertEquals(150000, again.battery().capacity());
    }

    @Test
    void incubatorUsageFollowsTheLegacyFormula() {
        Incubator inc = new Incubator();
        ListInventory inv = new ListInventory(inc);
        FakeWorld world = new FakeWorld(1);
        world.tick(inc);
        assertEquals(40, inc.usage(), "4 + 36 with no upgrades");
        inv.set(0, Stack.of("mariculture:basic_speed_upgrade"));
        inv.set(1, Stack.of("mariculture:basic_heating_upgrade"));
        world.time = 20;
        world.tick(inc);
        assertEquals(4 + 36 + 40 + 80, inc.usage(), "speed 2, heat 1");
        inv.set(2, Stack.of("mariculture:advanced_rf_upgrade"));
        world.time = 40;
        world.tick(inc);
        assertEquals(4 + (int) ((1 - 50000 / 300000D * 0.75) * 156), inc.usage());
    }

    @Test
    void incubatorTurnsChickenEggsIntoSpawnEggsOneInEight() {
        Incubator inc = new Incubator();
        ListInventory inv = new ListInventory(inc);
        FakeWorld world = new FakeWorld(3);
        inc.battery().set(50000);
        inv.set(Incubator.IN, Stack.of(Incubator.CHICKEN_EGG, 64));
        int ticks = 0;
        while (!inv.get(Incubator.IN).isEmpty() && ticks < 100000) {
            if (inc.battery().stored() < 1000) inc.battery().set(50000);
            world.tick(inc);
            ticks++;
        }
        assertTrue(inv.get(Incubator.IN).isEmpty(), "every egg is used");
        int spawn = 0;
        for (int s = Incubator.OUT; s < Incubator.SLOTS; s++) {
            Stack o = inv.get(s);
            if (!o.isEmpty()) {
                assertEquals("minecraft:chicken_spawn_egg", o.item());
                spawn += o.count();
            }
        }
        assertTrue(spawn > 0 && spawn < 24, "about 1 in 8 of 64: " + spawn);
        // 3 eggs per batch (heat 0) every 40 ticks (speed 1 x 10 against 400).
        assertTrue(ticks >= 21 * 40 && ticks <= 22 * 40 + 1, "ticks " + ticks);
    }

    @Test
    void incubatorStopsWithoutTwoTicksOfEnergy() {
        Incubator inc = new Incubator();
        ListInventory inv = new ListInventory(inc);
        FakeWorld world = new FakeWorld(3);
        inv.set(Incubator.IN, Stack.of(Incubator.CHICKEN_EGG, 4));
        inc.battery().set(79);
        for (int i = 0; i < 100; i++) world.tick(inc);
        assertEquals(4, inv.get(Incubator.IN).count());
        assertEquals(79, inc.battery().stored());
    }

    /** A world whose block below is water and whose casts are recorded; catches go straight to the outputs. */
    static final class FishingWorld extends FakeWorld {
        final List<FishingRules.Catch> caught = new ArrayList<>();
        String below = "minecraft:water";

        FishingWorld() {
            super(5);
        }

        @Override public String fluid(int dx, int dy, int dz) { return dy == -1 ? below : ""; }
        @Override public FishingRules.Cast castBelow(Species.Rod rod, int luck) {
            return new FishingRules.Cast(rod, luck, "overworld", 61, true, mariculture.domain.fish.Salinity.SALINE, 10, null, 0);
        }
        @Override public boolean deliverCatch(FishingRules.Catch c, int rodSlot, int[] outputs, boolean eject) {
            caught.add(c);
            return true;
        }
    }

    @Test
    void autofisherUsageAndSlots() {
        Autofisher f = new Autofisher();
        ListInventory inv = new ListInventory(f);
        new FakeWorld(1).tick(f);
        assertEquals(20 + 4 * 20, f.usage(), "speed 1 x 4");
        assertTrue(f.isValid(Autofisher.ROD, Stack.of("minecraft:fishing_rod")));
        assertTrue(f.isValid(Autofisher.ROD, Stack.of("mariculture:titanium_fishing_rod")));
        assertFalse(f.isValid(Autofisher.ROD, Stack.of("minecraft:stick")));
        assertTrue(f.isValid(Autofisher.BAIT, Stack.of("minecraft:bread")) || f.isValid(Autofisher.BAIT, Stack.of("mariculture:maggot")));
        assertFalse(f.isValid(Autofisher.OUT, Stack.of("minecraft:cod")));
        assertEquals(1, inv.slotLimit(Autofisher.ROD));
    }

    @Test
    void autofisherNeedsWaterRodBaitAndEnergy() {
        Autofisher f = new Autofisher();
        ListInventory inv = new ListInventory(f);
        FishingWorld world = new FishingWorld();
        f.battery().set(20000);
        for (int i = 0; i < 1000; i++) world.tick(f);
        assertEquals(20000, f.battery().stored(), "no rod: idle");
        inv.set(Autofisher.ROD, Stack.of("minecraft:fishing_rod"));
        String bait = null;
        for (String b : mariculture.domain.fishery.Bait.QUALITY.keySet()) {
            if (mariculture.domain.fishery.Bait.canUse(Species.Rod.DIRE, b)) bait = b;
        }
        inv.set(Autofisher.BAIT, Stack.of(bait, 64));
        world.below = "";
        world.time = 0;
        for (int i = 0; i < 1000; i++) world.tick(f);
        assertEquals(20000, f.battery().stored(), "no water below: idle");
        world.below = "minecraft:water";
        world.time = 0;
        for (int i = 0; i < 1000; i++) world.tick(f);
        assertTrue(f.battery().stored() < 20000, "fishing uses energy");
    }

    @Test
    void autofisherCatchesWithSpeedUpgradesAndABatteryInItsSlot() {
        mariculture.domain.fish.LiveFish.setRegistry(mariculture.domain.fish.FishData.registry());
        Autofisher f = new Autofisher();
        ListInventory inv = new ListInventory(f);
        FishingWorld world = new FishingWorld();
        // Speed 1 + 8 + 1 = 10, x 4 = 40 a tick: a cast every 63 ticks for 820 FE a tick, under the battery slot's 1000.
        inv.set(0, Stack.of("mariculture:ultimate_speed_upgrade"));
        inv.set(1, Stack.of("mariculture:basic_speed_upgrade"));
        inv.set(PoweredMachine.BATTERY_SLOT, Stack.of("mariculture:" + EnergyItem.CREATIVE));
        inv.set(Autofisher.ROD, Stack.of("mariculture:titanium_fishing_rod"));
        inv.set(Autofisher.BAIT, Stack.of("mariculture:raw_minnow", 64));
        f.battery().set(20000);
        for (int i = 0; i < 300; i++) world.tick(f);
        assertEquals(820, f.usage());
        assertTrue(world.caught.size() >= 4, "minnows (quality 100) always bite: " + world.caught.size());
        assertEquals(64 - world.caught.size(), inv.get(Autofisher.BAIT).count(), "one bait per catch");
        mariculture.domain.fish.LiveFish.setRegistry(mariculture.domain.fish.FishRegistry.build(java.util.List.of(), java.util.List.of(), java.util.Set.of()));
        assertFalse(world.caught.isEmpty(), "casts: " + world.caught.size() + " energy " + f.battery().stored());
    }

    /** A grid of blocks around the sponge (offsets from it); everything else is water. */
    static final class Pool extends FakeWorld {
        final Map<String, String> blocks = new HashMap<>();
        final Set<String> cleared = new HashSet<>();

        Pool() {
            super(1);
        }

        static String k(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        @Override public String block(int x, int y, int z) { return cleared.contains(k(x, y, z)) ? "minecraft:air" : blocks.getOrDefault(k(x, y, z), "minecraft:water"); }
        @Override public String fluid(int x, int y, int z) { return block(x, y, z).equals("minecraft:water") ? "minecraft:water" : ""; }
        @Override public void setBlock(int x, int y, int z, String block) {
            if (block.equals("minecraft:air")) cleared.add(k(x, y, z));
        }
    }

    @Test
    void spongeClearsTheMarkedBoxForFortyFePerBlock() {
        Sponge s = new Sponge();
        Pool pool = new Pool();
        pool.blocks.put(Pool.k(0, 0, 0), "mariculture:mechanized_sponge");
        pool.blocks.put(Pool.k(3, 0, 0), Sponge.MARKER);
        pool.blocks.put(Pool.k(-2, 0, 0), Sponge.MARKER);
        pool.blocks.put(Pool.k(0, 0, 4), Sponge.MARKER);
        pool.blocks.put(Pool.k(0, 0, -1), Sponge.MARKER);
        pool.blocks.put(Pool.k(0, 2, 0), Sponge.WET);
        for (int i = 0; i < 200; i++) pool.tick(s);
        assertEquals(5 * 5 * 2 * Sponge.PER_BLOCK, s.battery().capacity(), "asks for the box's energy");
        assertTrue(pool.cleared.isEmpty());
        int cost = s.battery().capacity();
        int got = 0;
        while (got < cost) got += s.battery().receive(cost, false);
        for (int i = 0; i < 200; i++) pool.tick(s);
        assertEquals(0, s.battery().stored());
        assertEquals(0, s.battery().capacity());
        assertTrue(pool.cleared.contains(Pool.k(-2, 2, 4)), "the far corner, top level");
        assertTrue(pool.cleared.contains(Pool.k(3, 0, 0)), "the marker sponges go too");
        assertTrue(pool.cleared.contains(Pool.k(0, 2, 0)), "wet ones as well");
        assertFalse(pool.cleared.contains(Pool.k(0, 0, 0)), "not itself");
        assertFalse(pool.cleared.contains(Pool.k(4, 0, 0)), "nothing outside the box");
        assertFalse(pool.cleared.contains(Pool.k(0, -1, 0)), "nothing below its level");
    }

    @Test
    void spongeHoldsNothingWithoutAllFiveMarkers() {
        Sponge s = new Sponge();
        Pool pool = new Pool();
        pool.blocks.put(Pool.k(3, 0, 0), Sponge.MARKER);
        for (int i = 0; i < 400; i++) pool.tick(s);
        assertEquals(0, s.battery().capacity());
        assertEquals(0, s.battery().receive(5000, false));
    }
}
