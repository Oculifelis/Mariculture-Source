package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Catalog;
import mariculture.domain.content.Face;
import mariculture.domain.content.MachineModel;
import mariculture.domain.content.Recipe;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.Machines;
import mariculture.domain.machine.Stack;

class MariTechPowerTest {
    /** A small block grid round the machine at 0,0,0: blocks, fluids, machines with facings, energy receivers. */
    static class Grid extends FakeWorld {
        final Map<String, String> blocks = new HashMap<>();
        final Map<String, String> fluids = new HashMap<>();
        final Set<String> flowing = new HashSet<>();
        final Map<String, Face> facings = new HashMap<>();
        final Map<String, Integer> energy = new HashMap<>();
        final Set<String> solid = new HashSet<>();
        Face facing = Face.NORTH;
        FluidAmount behind;
        int frontCapacity, frontAmount;
        String frontFluid;

        Grid() {
            super(1);
            fluidEverywhere = "";
        }

        static String k(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        void put(int x, int y, int z, Machine m, Face f) {
            machines.put(k(x, y, z), m);
            facings.put(k(x, y, z), f);
        }

        @Override public Face facing() { return facing; }
        @Override public Face facingAt(int x, int y, int z) { return facings.getOrDefault(k(x, y, z), Face.NORTH); }
        @Override public String fluid(int x, int y, int z) { return fluids.getOrDefault(k(x, y, z), ""); }
        @Override public boolean isFluidSource(int x, int y, int z) { return fluids.containsKey(k(x, y, z)) && !flowing.contains(k(x, y, z)); }
        @Override public boolean isAir(int x, int y, int z) {
            String key = k(x, y, z);
            return !key.equals("0,0,0") && !blocks.containsKey(key) && !fluids.containsKey(key) && !machines.containsKey(key) && !solid.contains(key);
        }
        @Override public boolean isSolidCube(int x, int y, int z) { return solid.contains(k(x, y, z)); }
        @Override public void setBlock(int x, int y, int z, String block) {
            String key = k(x, y, z);
            fluids.remove(key);
            machines.remove(key);
            if (block.equals("minecraft:air")) blocks.remove(key);
            else blocks.put(key, block);
        }
        @Override public boolean placeFluid(int x, int y, int z, String fluid) {
            fluids.put(k(x, y, z), fluid);
            flowing.remove(k(x, y, z));
            return true;
        }
        @Override public int insertEnergy(int x, int y, int z, Face side, int amount) {
            String key = k(x, y, z);
            if (!energy.containsKey(key)) return 0;
            int take = Math.min(amount, 1000);
            energy.merge(key, take, Integer::sum);
            return take;
        }
        @Override public FluidAmount drainFrom(Face face, int max, boolean execute) {
            if (face != mariculture.domain.factory.Geyser.opposite(facing) || behind == null || behind.isEmpty()) return null;
            int n = Math.min(max, behind.amount());
            FluidAmount out = new FluidAmount(behind.fluid(), n);
            if (execute) behind = new FluidAmount(behind.fluid(), behind.amount() - n);
            return out;
        }
        @Override public int fillTo(Face face, String fluid, int amount, boolean execute) {
            if (face != facing || frontCapacity == 0 || frontFluid != null && !frontFluid.equals(fluid)) return 0;
            int n = Math.min(amount, frontCapacity - frontAmount);
            if (execute && n > 0) {
                frontAmount += n;
                frontFluid = fluid;
            }
            return n;
        }

        void run(Machine m, int ticks) {
            for (int i = 0; i < ticks; i++) tick(m);
        }
    }

    private static Machine rotor(Rotor.Tier tier) {
        return Machines.byId(tier.block()).create();
    }

    @Test
    void rotorsFeedTheGeneratorAtTheEndOfTheirLineTimesTheirTier() {
        Grid w = new Grid();
        // Rotors at x 2 and 3 (z 0), generator at x 4 facing east, two receivers beside it.
        w.put(2, 0, 0, rotor(Rotor.TITANIUM), Face.EAST);
        w.put(3, 0, 0, rotor(Rotor.COPPER), Face.EAST);
        w.put(4, 0, 0, new Generator(), Face.EAST);
        w.energy.put(Grid.k(5, 0, 0), 0);
        w.energy.put(Grid.k(4, 1, 0), 0);
        w.energy.put(Grid.k(3, 0, 0), 0); // behind the generator: never fed (it is a rotor spot anyway)
        int given = Rotor.power(w, 2, 0, 0, 300, 50);
        assertEquals(1500, given, "titanium x5 = 1500, shared, not given twice (BUG-067)");
        assertEquals(1500, w.energy.get(Grid.k(5, 0, 0)) + w.energy.get(Grid.k(4, 1, 0)));
        assertEquals(50, ((Rotor) w.machine(2, 0, 0)).wear());
        assertTrue(((Rotor) w.machine(2, 0, 0)).spinning());

        // A generator facing the rotors is not theirs.
        w.facings.put(Grid.k(4, 0, 0), Face.WEST);
        assertNull(Rotor.generator(w, 2, 0, 0));
        assertEquals(0, Rotor.power(w, 2, 0, 0, 300, 0));
        assertEquals(-1, Rotor.power(w, 9, 0, 0, 300, 0), "no rotor there");
    }

    @Test
    void wornOutRotorsTurnBackIntoBaseIronAndBrokenOnesDropTheirWear() {
        Grid w = new Grid();
        Rotor r = (Rotor) rotor(Rotor.COPPER);
        r.placedFrom(Rotor.item(Rotor.COPPER, Rotor.COPPER.maxWear() - 10));
        w.put(1, 0, 0, r, Face.EAST);
        Rotor.power(w, 1, 0, 0, 10, 50);
        assertEquals(Rotor.BASE, w.blocks.get(Grid.k(1, 0, 0)));
        r.removed(w);
        assertTrue(w.drops.isEmpty(), "a worn-out rotor drops nothing");

        Rotor kept = (Rotor) rotor(Rotor.ALUMINUM);
        kept.placedFrom(Rotor.item(Rotor.ALUMINUM, 1234));
        assertEquals(1234, kept.wear(), "B-005: the placed rotor keeps the item's wear");
        kept.removed(w);
        assertEquals(List.of(Rotor.item(Rotor.ALUMINUM, 1234), Stack.of(Rotor.BASE)), w.drops);
        assertEquals(13, Rotor.bar(Stack.of("mariculture:copper_rotor")));
        assertEquals(7, Rotor.bar(Rotor.item(Rotor.COPPER, Rotor.COPPER.maxWear() / 2)));
    }

    @Test
    void sluicePressurisesWaterAndPushesTheRotorAtTheEndOfTheRun() {
        Grid w = new Grid();
        w.facing = Face.EAST;
        Sluice s = (Sluice) Machines.byId("sluice").create();
        // A water column of 6 behind (west), pressurised water running 3 blocks east, rotor at x 4.
        for (int y = 0; y < 6; y++) w.fluids.put(Grid.k(-1, y, 0), Sluice.WATER);
        w.put(4, 0, 0, rotor(Rotor.COPPER), Face.EAST);
        w.put(5, 0, 0, new Generator(), Face.EAST);
        w.energy.put(Grid.k(6, 0, 0), 0);
        w.time = 200;
        w.tick(s);
        assertEquals(Sluice.PRESSURISED, w.fluid(1, 0, 0), "pressurised water put in front");
        w.fluids.put(Grid.k(2, 0, 0), Sluice.PRESSURISED);
        w.fluids.put(Grid.k(3, 0, 0), Sluice.PRESSURISED);
        w.time = 400;
        w.tick(s);
        assertEquals(6, s.height());
        assertEquals(4, s.distance());
        assertEquals((6 * 4 * 25) >> 6, s.energyPerPush());
        w.time = 405;
        w.tick(s);
        assertEquals(s.energyPerPush(), w.energy.get(Grid.k(6, 0, 0)));

        // No water behind: the pressurised block goes and the turbine stops.
        for (int y = 0; y < 6; y++) w.fluids.remove(Grid.k(-1, y, 0));
        w.time = 600;
        w.tick(s);
        assertEquals("", w.fluid(1, 0, 0));
        assertEquals(0, s.distance());

        Sluice adv = (Sluice) Machines.byId("advanced_sluice").create();
        Map<String, Object> saved = new HashMap<>();
        saved.put("Height", 6);
        saved.put("Distance", 4);
        adv.load(saved);
        assertEquals((6 * 4 * 25 * 4) >> 6, adv.energyPerPush(), "advanced x4");
    }

    @Test
    void sluiceMovesFluidSourcesTanksAndBlocks() {
        Grid w = new Grid();
        w.facing = Face.UP;
        Sluice s = (Sluice) Machines.byId("sluice").create();
        // A lava source below (behind), a tank above (in front).
        w.fluids.put(Grid.k(0, -1, 0), "minecraft:lava");
        w.frontCapacity = 4000;
        w.time = 60;
        w.tick(s);
        assertEquals(1000, w.frontAmount);
        assertEquals("", w.fluid(0, -1, 0), "source taken");

        // Flowing fluid is not taken (legacy took it and made 1000 mB from nothing).
        w.fluids.put(Grid.k(0, -1, 0), "minecraft:water");
        w.flowing.add(Grid.k(0, -1, 0));
        w.time = 120;
        w.tick(s);
        assertEquals(1000, w.frontAmount);

        // A tank behind: a block placed in front and 1000 mB moved on to the tank in front.
        Grid p = new Grid();
        p.facing = Face.EAST;
        p.behind = new FluidAmount("minecraft:water", 3000);
        p.time = 60;
        p.tick(s);
        assertEquals("minecraft:water", p.fluid(1, 0, 0), "block placed in front");
        assertEquals(2000, p.behind.amount());
    }

    @Test
    void advancedSluiceFillsTheAreaInFrontWhenPowered() {
        Grid w = new Grid();
        w.facing = Face.EAST;
        w.powered = true;
        w.behind = new FluidAmount("minecraft:water", 100000);
        // A 3-wide trench one deep in front: everything else solid.
        for (int x = -40; x <= 40; x++) for (int z = -40; z <= 40; z++) for (int y = -3; y <= 0; y++) {
            boolean trench = x >= 1 && x <= 3 && z >= -1 && z <= 1 && y >= -1;
            if (!trench && (x | y | z) != 0) w.solid.add(Grid.k(x, y, z));
        }
        Sluice adv = (Sluice) Machines.byId("advanced_sluice").create();
        for (int i = 1; i <= 18; i++) {
            w.time = 60L * i;
            w.tick(adv);
        }
        long filled = w.fluids.values().stream().filter("minecraft:water"::equals).count();
        assertEquals(18, filled, "the 3x3x2 trench, one block every 60 ticks");
        assertEquals(100000 - 18000, w.behind.amount());
    }

    @Test
    void gasTurbineNeedsAFramedRotorAndBurnsOneMillibucketAPush() {
        Grid w = new Grid();
        GasTurbine t = new GasTurbine();
        t.tanks().get(0).fill(Fluids.NATURAL_GAS, 3000, false);
        assertEquals(0, t.tanks().get(0).fill("minecraft:water", 1000, true), "natural gas only");
        // Rotor north of the tank (z -1), generator further north facing north.
        w.put(0, 0, -1, rotor(Rotor.COPPER), Face.NORTH);
        w.put(0, 0, -2, new Generator(), Face.NORTH);
        w.energy.put(Grid.k(0, 0, -3), 0);
        w.time = 200;
        w.tick(t);
        assertFalse(t.hasRotor(), "no frame yet");
        // Frame in the x-y plane at z -1: 8 air round the rotor, 16 solid round those.
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            if (Math.abs(x) == 2 || Math.abs(y) == 2) w.solid.add(Grid.k(x, y, -1));
        }
        w.time = 400;
        w.tick(t);
        assertTrue(t.hasRotor());
        w.time = 405;
        w.tick(t);
        assertEquals(2999, t.tanks().get(0).amount());
        assertEquals(1000, w.energy.get(Grid.k(0, 0, -3)), "5000 offered, the receiver takes 1000 a push");
        assertEquals(GasTurbine.DAMAGE, ((Rotor) w.machine(0, 0, -1)).wear());
    }

    @Test
    void powerContentIsRegistered() {
        Catalog.blocks();
        for (String id : List.of("generator", "sluice", "advanced_sluice", "gas_turbine_tank", "copper_rotor_block", "aluminum_rotor_block", "titanium_rotor_block")) {
            assertEquals(id, Machines.byId(id).id());
        }
        Set<String> recipes = new HashSet<>();
        for (Recipe r : Catalog.recipes()) recipes.add(r.id());
        assertTrue(recipes.containsAll(List.of("generator", "sluice", "advanced_sluice", "gas_turbine_tank", "copper_rotor", "aluminum_rotor", "titanium_rotor")));
        double[] b = MachineModel.of("titanium_rotor_block").bounds(Face.SOUTH);
        for (double v : b) assertTrue(v >= 0 && v <= 16, "rotor outline stays in the block");
        // A six-way model with a "front" slot would also get the horizontal facing and fail to register.
        for (var spec : Catalog.blocks()) {
            if (spec.shape() == mariculture.domain.content.Shape.MACHINE_MODEL && MachineModel.of(spec.id()).facing() == MachineModel.Facing.ALL) {
                assertFalse(spec.facing(), spec.id());
            }
        }
    }
}
