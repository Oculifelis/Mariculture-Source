package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Face;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

class FluddTest {
    /** Open air to the east, a stone wall at {@code wall}, bedrock at {@code bedrock}. */
    static final class JetWorld extends FakeWorld {
        int wall = 99;
        int bedrock = -1;
        final Set<String> broken = new HashSet<>();
        final List<int[]> jet = new ArrayList<>();
        int pushes;
        float hurt;

        JetWorld() {
            super(5);
            fluidEverywhere = "";
        }

        private boolean solid(int dx) {
            return dx == wall || dx == bedrock;
        }

        @Override public Face facing() { return Face.EAST; }
        @Override public boolean isAir(int dx, int dy, int dz) { return !solid(dx) || broken.contains(dx + "," + dy + "," + dz); }
        @Override public boolean passable(int dx, int dy, int dz) { return isAir(dx, dy, dz); }
        @Override public float hardness(int dx, int dy, int dz) { return dx == bedrock ? -1f : 1.5f; }
        @Override public void breakBlock(int dx, int dy, int dz) { broken.add(dx + "," + dy + "," + dz); }
        @Override public void hurtLiving(double[] box, float amount) { hurt += amount; }

        @Override
        public void moveEntities(List<int[]> offsets, java.util.function.UnaryOperator<EntityMotion> rule) {
            jet.clear();
            jet.addAll(offsets);
            EntityMotion m = rule.apply(new EntityMotion(0, 0, 0, false));
            if (m != null && m.x() != 0) pushes++;
        }
    }

    @Test
    void modesCycleAndEachUseCostsOneUnit() {
        Stack f = Fludd.of(2);
        assertEquals(Fludd.Mode.HOVER, Fludd.mode(f));
        for (int i = 0; i < 3; i++) f = Fludd.nextMode(f);
        assertEquals(Fludd.Mode.SQUIRT, Fludd.mode(f));
        assertEquals(Fludd.Mode.HOVER, Fludd.mode(Fludd.nextMode(f)), "wraps after squirt");
        f = Fludd.use(f, Fludd.Mode.TURBO);
        assertEquals(1, Fludd.water(f));
        assertEquals(Fludd.Mode.TURBO, Fludd.mode(f), "using a mode records it (damageFLUDD)");
        f = Fludd.use(f, Fludd.Mode.TURBO);
        assertEquals(Fludd.Mode.NONE, Fludd.mode(f), "no water, no mode");
        assertEquals(0, Fludd.water(Fludd.use(f, Fludd.Mode.TURBO)));
    }

    @Test
    void fillsFromPressurisedWaterOnly() {
        Tank tank = new Tank(100000);
        tank.fill(Fluids.WATER, 50000, false);
        assertEquals(0, Fludd.water(Fludd.fill(Fludd.of(0), tank)));
        tank = new Tank(100000);
        tank.fill(Fluids.PRESSURISED_WATER, 50000, false);
        assertEquals(Fludd.STORAGE, Fludd.water(Fludd.fill(Fludd.of(5000), tank)));
        assertEquals(50000 - 15000, tank.amount());
    }

    @Test
    void rocketLiftsForTenTicksUntilItLands() {
        Fludd.Pilot p = new Fludd.Pilot();
        int boosts = 0;
        for (int i = 0; i < 40; i++) {
            Fludd.Step s = p.tick(Fludd.Mode.ROCKET, new Fludd.Input(true, i == 0, false, false, false));
            if (s.addY() == Fludd.ROCKET_LIFT) boosts++;
        }
        assertEquals(Fludd.BOOST_TICKS, boosts);
        p.tick(Fludd.Mode.ROCKET, new Fludd.Input(true, true, false, false, false));
        assertEquals(Fludd.ROCKET_LIFT, p.tick(Fludd.Mode.ROCKET, new Fludd.Input(true, false, false, false, false)).addY(),
                "landing resets it from the next tick (legacy checks before resetting)");
    }

    @Test
    void hoverHoldsForThreeHundredTicksAndOnlyTheStartCostsWater() {
        Fludd.Pilot p = new Fludd.Pilot();
        int held = 0, cost = 0;
        for (int i = 0; i < 400; i++) {
            Fludd.Step s = p.tick(Fludd.Mode.HOVER, new Fludd.Input(true, i == 0, false, false, false));
            if (s.setY() == Fludd.HOVER_RISE) held++;
            if (s.cost()) cost++;
        }
        assertEquals(Fludd.HOVER_TICKS - 1, held);
        assertEquals(Fludd.BOOST_TICKS - 1, cost);
        assertTrue(p.tick(Fludd.Mode.HOVER, new Fludd.Input(false, false, false, false, false)).landed(), "letting go forgives the fall");
    }

    @Test
    void turboPushesHardestOnTheGround() {
        Fludd.Pilot p = new Fludd.Pilot();
        assertEquals(Fludd.TURBO_GROUND, p.tick(Fludd.Mode.TURBO, new Fludd.Input(true, true, false, true, false)).forward());
        assertEquals(Fludd.TURBO_AIR, p.tick(Fludd.Mode.TURBO, new Fludd.Input(true, false, false, true, false)).forward());
        assertEquals(Fludd.TURBO_WATER, p.tick(Fludd.Mode.TURBO, new Fludd.Input(true, false, true, true, false)).forward());
        assertTrue(p.tick(Fludd.Mode.TURBO, new Fludd.Input(true, true, false, true, false)).cost());
    }

    private static FluddStand stand(JetWorld world, ListInventory[] holder, String... upgrades) {
        FluddStand s = new FluddStand();
        ListInventory inv = new ListInventory(s);
        for (int i = 0; i < upgrades.length; i++) inv.set(i, Stack.of("mariculture:" + upgrades[i]));
        s.placedFrom(Fludd.of(20000));
        if (holder != null) holder[0] = inv;
        return s;
    }

    @Test
    void standJetsOnlyWhenPoweredAndReachesEightBlocksPerPurity() {
        JetWorld world = new JetWorld();
        FluddStand s = stand(world, null);
        world.tick(s);
        assertTrue(world.jet.isEmpty(), "redstone HIGH: needs power");
        world.powered = true;
        world.time = 20;
        world.tick(s);
        assertEquals(8, world.jet.size());
        assertTrue(world.pushes > 0);

        JetWorld w2 = new JetWorld();
        w2.powered = true;
        FluddStand pure = stand(w2, null, "basic_purity_upgrade");
        w2.tick(pure);
        assertEquals(16, w2.jet.size());
        w2.wall = 5;
        w2.time = 20;
        w2.tick(pure);
        assertEquals(5, w2.jet.size(), "stops at a solid block");
    }

    @Test
    void standUsesWaterEveryHundredTicksAndDropsItsFludd() {
        JetWorld world = new JetWorld();
        world.powered = true;
        FluddStand s = stand(world, null);
        for (int i = 0; i <= 100; i++) {
            world.time = i;
            world.tick(s);
        }
        // distance 8 / 8 + heat 0 + speed 1, at ticks 0 and 100.
        assertEquals(20000 - 4, s.tanks().get(0).amount());
        assertEquals(2, s.usage());
        s.removed(world);
        assertEquals(Fludd.ITEM, world.drops.get(0).item());
        assertEquals(20000 - 4, Fludd.water(world.drops.get(0)));
        assertFalse(s.tanks().get(0).accepts(Fluids.WATER), "pressurised water only");
    }

    @Test
    void coolingWearsDownBlocksButNeverBedrock() {
        JetWorld world = new JetWorld();
        world.powered = true;
        world.wall = 3;
        world.bedrock = 3;
        FluddStand s = stand(world, null, "ultimate_cooling_upgrade");
        for (int i = 0; i < 400; i++) {
            world.time = i;
            world.tick(s);
        }
        assertTrue(world.broken.isEmpty(), "BUG-070: bedrock (hardness -1) stays");
        JetWorld w2 = new JetWorld();
        w2.powered = true;
        w2.wall = 3;
        FluddStand s2 = stand(w2, null, "ultimate_cooling_upgrade");
        for (int i = 0; i < 400; i++) {
            w2.time = i;
            w2.tick(s2);
        }
        assertTrue(w2.broken.contains("3,0,0"), "hardness 1.5 <= 28 breaks, 1 in 15 a tick");
    }

    @Test
    void heatingScalds() {
        JetWorld world = new JetWorld();
        world.powered = true;
        FluddStand s = stand(world, null, "ultimate_heating_upgrade");
        world.tick(s);
        assertEquals(8 * 7, world.hurt, 0.001, "heat 14 / 2 in each of 8 blocks");
    }

    @Test
    void sidewaysJetsPushAndEtherealReverses() {
        var m = new mariculture.domain.machine.MachineWorld.EntityMotion(0, 0, 0, false);
        assertEquals(0.025, FluddStand.push(m, Face.EAST, 1, false).x(), 1e-9);
        assertEquals(-0.025, FluddStand.push(m, Face.EAST, 1, true).x(), 1e-9);
        assertEquals(0.15, FluddStand.push(m, Face.UP, 1, false).y(), 1e-9);
        assertEquals(null, FluddStand.push(new mariculture.domain.machine.MachineWorld.EntityMotion(0, 0.1, 0, false), Face.UP, 1, false));
    }

    @Test
    void pressureVesselHoldsTwoHundredBucketsAndChargesTheFludd() {
        PressureVessel v = new PressureVessel();
        ListInventory inv = new ListInventory(v);
        FakeWorld world = new FakeWorld(1);
        world.tick(v);
        assertEquals(200000, v.tanks().get(0).capacity());
        inv.set(0, Stack.of("mariculture:basic_storage_upgrade"));
        world.time = 20;
        world.tick(v);
        assertEquals(208000, v.tanks().get(0).capacity());
        v.tanks().get(0).fill(Fluids.PRESSURISED_WATER, 30000, false);
        inv.set(PressureVessel.SPECIAL, Fludd.of(0));
        world.time = 30;
        world.tick(v);
        assertEquals(Fludd.STORAGE, Fludd.water(inv.get(PressureVessel.SPECIAL)));
        assertEquals(10000, v.tanks().get(0).amount());
    }

    @Test
    void touchingVesselsShareTheirFluidLikeOneTank() {
        PressureVessel a = new PressureVessel(), b = new PressureVessel();
        new ListInventory(a);
        new ListInventory(b);
        FakeWorld world = new FakeWorld(1);
        world.machines.put("1,0,0", b);
        a.tanks().get(0).fill(Fluids.PRESSURISED_WATER, 100000, false);
        world.tick(a);
        assertEquals(50000, a.tanks().get(0).amount());
        assertEquals(50000, b.tanks().get(0).amount());
        assertEquals(Fluids.PRESSURISED_WATER, b.tanks().get(0).fluid());
        b.tanks().get(0).set(Fluids.WATER, 10);
        world.time = 20;
        world.tick(a);
        assertEquals(10, b.tanks().get(0).amount(), "different fluids stay apart");
    }
}
