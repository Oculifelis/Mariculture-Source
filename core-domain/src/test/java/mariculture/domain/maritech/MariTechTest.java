package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Catalog;
import mariculture.domain.content.Recipe;
import mariculture.domain.diving.AirCompressor;
import mariculture.domain.diving.Scuba;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.metal.VatRecipes;

class MariTechTest {
    private static final Stack COPPER = Stack.of("mariculture:" + EnergyItem.COPPER);
    private static final Stack TITANIUM = Stack.of("mariculture:" + EnergyItem.TITANIUM);
    private static final Stack CREATIVE = Stack.of("mariculture:" + EnergyItem.CREATIVE);
    private static final Stack TANK = Stack.of("mariculture:" + Scuba.TANK);
    private static final Stack MASK = Stack.of("mariculture:" + Scuba.MASK);

    @Test
    void batteriesHoldAndMoveEnergyWithinTheirLimits() {
        var in = EnergyItem.receive(COPPER, 5000, false);
        assertEquals(100, in.amount(), "copper takes 100 a tick");
        assertEquals(100, EnergyItem.stored(in.stack()));
        Stack full = EnergyItem.withEnergy(TITANIUM, 100000);
        var out = EnergyItem.extract(full, 9000, false);
        assertEquals(2500, out.amount(), "titanium gives 2500 a tick");
        assertEquals(97500, EnergyItem.stored(out.stack()));
        assertEquals(0, EnergyItem.receive(EnergyItem.withEnergy(COPPER, 10000), 50, false).amount());
        assertEquals(13, EnergyItem.bar(full));
        assertEquals(0, EnergyItem.bar(COPPER));
        assertEquals(0, EnergyItem.receive(COPPER.withCount(2), 50, false).amount(), "a stack of batteries is not charged");
    }

    @Test
    void creativeBatteryGivesExactlyWhatIsAskedAndTakesNothing() {
        assertEquals(1234, EnergyItem.extract(CREATIVE, 1234, false).amount());
        assertEquals(CREATIVE, EnergyItem.extract(CREATIVE, 1234, false).stack());
        assertEquals(0, EnergyItem.receive(CREATIVE, 100, false).amount());
    }

    @Test
    void scubaTankGivesBreathAndLosesAirEvery150Ticks() {
        Stack tank = Scuba.withAir(TANK, 2);
        var dry = Scuba.check(150, MASK, tank, false, false);
        assertFalse(dry.fullBreath());
        var wet = Scuba.check(150, MASK, tank, true, false);
        assertTrue(wet.fullBreath());
        assertEquals(1, Scuba.air(wet.tank()));
        var between = Scuba.check(155, MASK, tank, true, false);
        assertEquals(2, Scuba.air(between.tank()), "air is only used on the 150th tick");
        var empty = Scuba.check(150, MASK, TANK, true, false);
        assertFalse(empty.fullBreath(), "a new tank is empty");
        assertFalse(Scuba.check(150, MASK, Stack.of("minecraft:elytra"), true, false).fullBreath());
    }

    @Test
    void maskLandLightsWearItOutOfWater() {
        Stack lit = Scuba.toggleLandLights(MASK);
        assertTrue(Scuba.landLights(lit));
        assertEquals(1, Scuba.check(750, lit, Stack.EMPTY, false, false).maskWear());
        assertEquals(0, Scuba.check(745, lit, Stack.EMPTY, false, false).maskWear());
        assertEquals(0, Scuba.check(750, lit, Stack.EMPTY, true, false).maskWear(), "no wear in water");
        assertEquals(0, Scuba.check(750, MASK, Stack.EMPTY, false, false).maskWear(), "no wear with the lights off");
        assertFalse(Scuba.landLights(Scuba.toggleLandLights(lit)));
        assertTrue(Scuba.lights(false, lit));
        assertFalse(Scuba.lights(false, MASK));
    }

    @Test
    void suitAndFinsSpeedUpBreakingUnderWater() {
        assertEquals(4f, Scuba.breakSpeed(1f, true, false, true, true, false, true));
        assertEquals(128f, Scuba.breakSpeed(1f, true, false, true, true, true, true));
        assertEquals(20f, Scuba.breakSpeed(1f, true, true, true, true, false, false));
        assertEquals(1f, Scuba.breakSpeed(1f, true, true, false, true, false, false), "dry");
        assertTrue(Scuba.active(false, true, true), "lava with Fire Protection");
        assertFalse(Scuba.active(false, true, false));
        assertEquals(2.0, Scuba.finsSwim(true, true));
        assertEquals(1.0, Scuba.finsSwim(true, false));
        assertEquals(0.0, Scuba.finsSwim(false, true));
        assertEquals(-0.75, Scuba.finsLand(false, false));
        assertEquals(0.0, Scuba.finsLand(true, false));
    }

    @Test
    void fireProtectedTankTakesFireForAir() {
        Stack tank = Scuba.withAir(TANK, 5);
        assertEquals(4, Scuba.air(Scuba.fireHit(true, true, tank, true)));
        assertNull(Scuba.fireHit(true, true, tank, false));
        assertNull(Scuba.fireHit(true, false, tank, true));
        assertNull(Scuba.fireHit(true, true, TANK, true), "an empty tank does not help");
        assertNull(Scuba.fireHit(false, true, tank, true));
    }

    @Test
    void airCompressorTurnsEnergyIntoAirAndFillsTanks() {
        AirCompressor c = new AirCompressor();
        FakeWorld world = new FakeWorld(1);
        assertEquals(10000, c.battery().receive(20000, false));
        for (int i = 0; i < 200; i++) c.tick(world);
        assertEquals(10, c.air(), "one air per 1000 every 20 ticks");
        assertEquals(0, c.battery().stored());
        Stack tank = c.useHeld(TANK, true);
        assertEquals(1, Scuba.air(tank), "sneaking moves one");
        tank = c.useHeld(tank, false);
        assertEquals(10, Scuba.air(tank));
        assertEquals(0, c.air());
        Map<String, Object> saved = new HashMap<>();
        c.save(saved);
        AirCompressor d = new AirCompressor();
        d.load(saved);
        assertEquals(0, d.air());
        assertNull(c.useHeld(Stack.of("minecraft:stick"), false));
    }

    @Test
    void airCompressorChargesFromBatteriesAndRefusesEnergyWhenFull() {
        AirCompressor c = new AirCompressor();
        Stack battery = EnergyItem.withEnergy(TITANIUM, 100000);
        Stack after = c.useHeld(battery, false);
        assertEquals(2500, c.battery().stored(), "a click takes up to 5000, within the battery's 2500 a tick");
        assertEquals(97500, EnergyItem.stored(after));
        assertEquals(CREATIVE, c.useHeld(CREATIVE, false));
        assertEquals(7500, c.battery().stored(), "the creative battery gives the full 5000");
        Map<String, Object> full = new HashMap<>();
        full.put("StoredAir", AirCompressor.MAX_AIR);
        full.put("Energy", 0);
        AirCompressor d = new AirCompressor();
        d.load(full);
        assertEquals(0, d.battery().receive(1000, false), "no room for air, no energy taken");
    }

    @Test
    void catalogHasTheGearRecipesAndChargedBatteries() {
        for (String id : new String[] {"copper_battery", "titanium_battery", "creative_battery", "scuba_mask", "scuba_tank", "scuba_suit", "swimfin"}) {
            assertTrue(Catalog.items().stream().anyMatch(s -> s.id().equals(id)), id);
        }
        assertTrue(Catalog.blocks().stream().anyMatch(b -> b.id().equals("air_compressor")));
        Recipe charged = Catalog.recipes().stream().filter(r -> r.id().equals("titanium_battery_charged")).findFirst().orElseThrow();
        assertEquals(100000, ((Recipe.Shapeless) charged).resultData().get("Energy"));
        for (String id : new String[] {"basic_rf_upgrade", "standard_rf_upgrade", "advanced_rf_upgrade", "ultimate_rf_upgrade", "lens", "air_compressor"}) {
            assertTrue(Catalog.recipes().stream().anyMatch(r -> r.id().equals(id)), id);
        }
        assertFalse(Catalog.recipes().stream().anyMatch(r -> r.id().equals("creative_battery")), "creative only");
        assertNotNull(VatRecipes.all().stream().filter(r -> r.id().equals("plastic_from_limestone")).findFirst().orElse(null));
        assertNotNull(VatRecipes.all().stream().filter(r -> r.id().equals("neoprene")).findFirst().orElse(null));
    }
}
