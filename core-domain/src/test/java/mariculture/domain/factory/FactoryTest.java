package mariculture.domain.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import mariculture.domain.content.Face;
import mariculture.domain.content.MachineModel;
import mariculture.domain.diving.AirPump;
import mariculture.domain.machine.Battery;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.MachineWorld.EntityMotion;
import mariculture.domain.machine.Stack;

class FactoryTest {
    /** A dry world with solid blocks where {@code solid} says so. */
    static class World extends FakeWorld {
        final Map<String, String> blocks = new HashMap<>();
        final List<String> airRefills = new ArrayList<>();
        Face facing = Face.UP;
        int solidAt = Integer.MAX_VALUE;
        List<int[]> moved;

        World() {
            super(1);
            fluidEverywhere = "";
        }

        @Override public Face facing() { return facing; }
        @Override public boolean passable(int dx, int dy, int dz) { return Math.abs(dx + dy + dz) < solidAt; }
        @Override public void moveEntities(List<int[]> offsets, UnaryOperator<EntityMotion> rule) { moved = offsets; }
        @Override public String block(int dx, int dy, int dz) { return blocks.getOrDefault(dx + "," + dy + "," + dz, "minecraft:air"); }
        @Override public void setBlock(int dx, int dy, int dz, String block) { blocks.put(dx + "," + dy + "," + dz, block); }
        @Override public Stack uncraft(Stack one) { return one.is("minecraft:iron_block") ? Stack.of("minecraft:iron_ingot", 9) : Stack.EMPTY; }
        @Override public void refillAir(double rx, double ry, double rz, String helmet, String top) { airRefills.add(helmet + "+" + top); }
        @Override public java.util.Set<String> itemTags(String item) {
            return switch (item) {
                case "minecraft:copper_ingot", "othermod:copper_ingot" -> java.util.Set.of("c:ingots/copper", "c:ingots");
                case "minecraft:iron_ingot" -> java.util.Set.of("c:ingots/iron", "c:ingots");
                case "minecraft:blue_dye", "minecraft:lapis_lazuli" -> java.util.Set.of("c:dyes/blue");
                default -> java.util.Set.of();
            };
        }
        @Override public List<String> itemsInTag(String tag) {
            return tag.equals("c:ingots/copper") ? List.of("minecraft:copper_ingot", "othermod:copper_ingot") : List.of();
        }
    }

    @Test
    void geyserReachDependsOnTheFluidAndStopsAtSolidBlocks() {
        assertEquals(8, Geyser.reach("minecraft:water"));
        assertEquals(16, Geyser.reach("mariculture:pressurised_water"));
        assertEquals(0, Geyser.reach("minecraft:lava"));
        World w = new World();
        assertEquals(8, Geyser.jet(w, Face.UP, 8).size());
        w.solidAt = 3;
        List<int[]> jet = Geyser.jet(w, Face.EAST, 8);
        assertEquals(3, jet.size());
        assertEquals(2, jet.get(2)[0]);
    }

    @Test
    void geyserLiftsUpwardsAndPushesSideways() {
        EntityMotion lifted = Geyser.push(new EntityMotion(0.3, 0, 0.1, false), Face.UP);
        assertEquals(0.25, lifted.y(), 1e-9);
        assertEquals(0.3, lifted.x(), 1e-9);
        assertNull(Geyser.push(new EntityMotion(0, 0.3, 0, false), Face.UP));
        EntityMotion item = Geyser.push(new EntityMotion(0.3, 0, 0.1, true), Face.UP);
        assertEquals(0.265, item.y(), 1e-9);
        assertEquals(0, item.x(), 1e-9);
        EntityMotion side = Geyser.push(new EntityMotion(0, 0, 0, false), Face.WEST);
        assertEquals(-0.1, side.x(), 1e-9);
    }

    @Test
    void geyserWorksWithWaterUnlessPoweredAndUsesOneMillibucketEveryHundredTicks() {
        Geyser g = new Geyser();
        g.tanks().get(0).fill("minecraft:water", 1000, false);
        World w = new World();
        for (int i = 0; i < 100; i++) w.tick(g);
        assertTrue(g.working());
        assertEquals(999, g.tanks().get(0).amount());
        assertEquals(8, w.moved.size());
        w.powered = true;
        for (int i = 0; i < 20; i++) w.tick(g);
        assertFalse(g.working());
        assertFalse(g.tanks().get(0).accepts("minecraft:lava"));
    }

    @Test
    void unpackerUncraftsOneItemAtATimeIntoTheInventoryBelow() {
        Unpacker u = new Unpacker();
        ListInventory inv = new ListInventory(u);
        u.attach(inv);
        World w = new World();
        assertTrue(u.canInsert(Unpacker.INPUT, Stack.of("minecraft:iron_block"), Face.UP));
        assertFalse(u.canInsert(Unpacker.INPUT, Stack.of("minecraft:iron_block"), Face.NORTH));
        w.tick(u);
        assertFalse(u.canInsert(Unpacker.INPUT, Stack.of("minecraft:dirt"), Face.UP));
        inv.set(Unpacker.INPUT, Stack.of("minecraft:iron_block", 2));
        w.tick(u);
        w.tick(u);
        assertTrue(inv.get(Unpacker.INPUT).isEmpty());
        assertEquals(2, w.ejected.get(Face.DOWN).size());
        assertEquals(9, w.ejected.get(Face.DOWN).get(0).count());
    }

    @Test
    void airPumpNeedsEnergyRefillsDiversAndSucksUpGas() {
        AirPump p = new AirPump();
        World w = new World();
        w.blocks.put("3,2,-1", "mariculture:natural_gas");
        for (int i = 0; i < 300; i++) w.tick(p);
        assertTrue(w.airRefills.isEmpty());
        assertEquals("mariculture:natural_gas", w.block(3, 2, -1));
        Battery b = p.battery();
        assertEquals(2000, b.receive(5000, false));
        for (int i = 0; i < 20; i++) w.tick(p);
        assertEquals("minecraft:air", w.block(3, 2, -1));
        assertEquals(1000, p.tanks().get(0).amount());
        assertEquals(1900, b.stored());
        for (int i = 0; i < 280; i++) w.tick(p);
        assertEquals(List.of("mariculture:diving_helmet+mariculture:diving_top"), w.airRefills);
        assertEquals(2000 - 15 * 100 - 100, b.stored());
        Map<String, Object> saved = new HashMap<>();
        p.save(saved);
        AirPump q = new AirPump();
        q.load(saved);
        assertEquals(b.stored(), q.battery().stored());
        assertEquals(1000, q.tanks().get(0).amount());
    }

    @Test
    void airPumpWindsByHandOncePerTurnOfTheWheel() {
        AirPump p = new AirPump();
        World w = new World();
        p.onUse();
        assertEquals(100, p.battery().stored());
        p.onUse();
        assertEquals(100, p.battery().stored());
        for (int i = 0; i < AirPump.WHEEL_TICKS; i++) w.tick(p);
        assertFalse(p.turning());
        assertEquals(0, p.battery().stored(), "the first gas cycle used the wound energy");
        p.onUse();
        assertEquals(100, p.battery().stored());
    }

    @Test
    void dictionaryMatchesSharedMaterialTagsOnly() {
        World w = new World();
        assertTrue(Dictionary.same("minecraft:copper_ingot", w.itemTags("minecraft:copper_ingot"), "othermod:copper_ingot", w.itemTags("othermod:copper_ingot")));
        assertFalse(Dictionary.same("minecraft:copper_ingot", w.itemTags("minecraft:copper_ingot"), "minecraft:iron_ingot", w.itemTags("minecraft:iron_ingot")),
                "c:ingots alone is a group, not a material");
        assertFalse(Dictionary.same("minecraft:blue_dye", w.itemTags("minecraft:blue_dye"), "minecraft:lapis_lazuli", w.itemTags("minecraft:lapis_lazuli")),
                "lapis is blacklisted");
        assertEquals("othermod:copper_ingot", Dictionary.next("minecraft:copper_ingot", w.itemsInTag("c:ingots/copper")));
        assertEquals("minecraft:copper_ingot", Dictionary.next("othermod:copper_ingot", w.itemsInTag("c:ingots/copper")));
    }

    @Test
    void converterTurnsItemsIntoTheFilterItemAndCyclesFilters() {
        DictionaryConverter d = new DictionaryConverter();
        ListInventory inv = new ListInventory(d);
        d.attach(inv);
        World w = new World();
        inv.set(0, Stack.of("minecraft:copper_ingot"));
        inv.set(DictionaryConverter.INPUT, Stack.of("othermod:copper_ingot", 5));
        w.tick(d);
        assertTrue(w.ejected.isEmpty(), "FakeWorld.ejectToNeighbours keeps everything, so the stack lands in the outputs");
        assertTrue(inv.get(DictionaryConverter.INPUT).isEmpty());
        assertEquals(Stack.of("minecraft:copper_ingot", 5), inv.get(25));
        inv.set(DictionaryConverter.INPUT, Stack.of("minecraft:iron_ingot", 2));
        w.tick(d);
        assertEquals(Stack.of("minecraft:iron_ingot", 2), inv.get(26), "items without a match stay themselves");
        d.button(DictionaryConverter.CYCLE_BUTTON);
        assertEquals("othermod:copper_ingot", inv.get(0).item());
    }

    @Test
    void filterHoldsNineDistinctItems() {
        Stack f = Stack.of("mariculture:item_filter");
        for (int i = 0; i < 9; i++) f = ItemFilter.add(f, "minecraft:item" + i);
        assertEquals(9, ItemFilter.entries(f).size());
        assertNull(ItemFilter.add(f, "minecraft:more"));
        assertNull(ItemFilter.add(ItemFilter.clear(f), "minecraft:air"));
        assertTrue(ItemFilter.entries(ItemFilter.clear(f)).isEmpty());
        DictionaryConverter d = new DictionaryConverter();
        ListInventory inv = new ListInventory(d);
        d.attach(inv);
        inv.set(3, ItemFilter.add(Stack.of("mariculture:item_filter"), "othermod:copper_ingot"));
        assertEquals("othermod:copper_ingot", d.convert(new World(), Stack.of("minecraft:copper_ingot")).item());
    }

    @Test
    void batteryHonoursItsLimits() {
        Battery b = new Battery(100, 30, 10);
        assertEquals(30, b.receive(50, false));
        assertEquals(30, b.receive(50, true));
        assertEquals(10, b.extract(50, false));
        assertTrue(b.use(20));
        assertFalse(b.use(20));
    }

    @Test
    void machineModelOutlinesTurnWithTheFacing() {
        mariculture.domain.content.Catalog.blocks();
        double[] up = MachineModel.of("geyser").bounds(Face.UP);
        assertEquals(1.6, up[0], 1e-9);
        assertEquals(0, up[1], 1e-9);
        assertEquals(4, up[4], 1e-9);
        double[] down = MachineModel.of("geyser").bounds(Face.DOWN);
        assertEquals(12, down[1], 1e-9);
        assertEquals(16, down[4], 1e-9);
        double[] north = MachineModel.of("geyser").bounds(Face.NORTH);
        assertEquals(12, north[2], 1e-9);
        assertEquals(16, north[5], 1e-9);
        double[] east = MachineModel.of("geyser").bounds(Face.EAST);
        assertEquals(0, east[0], 1e-9);
        assertEquals(4, east[3], 1e-9);
        double[] pump = MachineModel.of("air_pump").bounds(Face.SOUTH);
        assertEquals(0, pump[1], 1e-9);
        assertTrue(pump[4] <= 14.4 + 1e-9 && pump[0] >= 3.2 - 1e-9 && pump[3] <= 12.8 + 1e-9);
    }
}
