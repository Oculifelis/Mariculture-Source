package mariculture.domain.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Face;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Stack;

class FactoryPart2Test {
    /** Every item is its own block except sticks. */
    static final class SawWorld extends FakeWorld {
        final List<String> filled = new ArrayList<>();
        boolean refuse;

        SawWorld() {
            super(3);
        }

        @Override public String blockOfItem(String item) { return item.equals("minecraft:stick") ? null : item; }
        @Override public boolean fluidExists(String fluid) { return !fluid.startsWith("missing:"); }
        @Override public int fillTo(Face face, String fluid, int amount, boolean execute) {
            if (refuse) return 0;
            if (execute) filled.add(face + " " + fluid + " " + amount);
            return amount;
        }
    }

    @AfterEach
    void reset() {
        Settings.reset();
    }

    private static Sawmill loaded(ListInventory[] holder) {
        Sawmill m = new Sawmill();
        ListInventory inv = new ListInventory(m);
        inv.set(Sawmill.PLAN, Plans.of(Plans.Type.STAIRS));
        inv.set(Sawmill.TOP, Stack.of("minecraft:oak_planks", 2));
        inv.set(Sawmill.BOTTOM, Stack.of("minecraft:stone", 2));
        inv.set(Sawmill.NORTH, Stack.of("minecraft:glass", 2));
        inv.set(Sawmill.SOUTH, Stack.of(Sawmill.FEATHER, 2));
        inv.set(Sawmill.WEST, Stack.of("minecraft:dirt", 2));
        inv.set(Sawmill.EAST, Stack.of("minecraft:sand", 2));
        holder[0] = inv;
        return m;
    }

    @Test
    void sawmillCutsSixFacesIntoThePlansShape() {
        ListInventory[] inv = new ListInventory[1];
        Sawmill m = loaded(inv);
        SawWorld w = new SawWorld();
        for (int i = 0; i <= 6; i++) {
            w.time = i;
            w.tick(m);
        }
        Stack out = inv[0].get(Sawmill.OUT);
        assertEquals("mariculture:custom_stairs", out.item());
        assertEquals(4 * 2, out.count(), "stairs plan 4 x output multiplier 2");
        CustomFaces f = CustomFaces.of(out);
        assertEquals("minecraft:stone", f.block(Face.DOWN));
        assertEquals("minecraft:oak_planks", f.block(Face.UP));
        assertEquals(CustomFaces.AIR, f.block(Face.SOUTH), "a feather leaves the face see-through");
        assertEquals("minecraft:sand", f.block(Face.EAST));
        assertEquals(Face.DOWN, f.side(Face.UP), "the sawmill always shows the source's bottom");
        assertEquals(1, inv[0].get(Sawmill.TOP).count(), "one of each block used");
        assertTrue(Plans.isPlan(inv[0].get(Sawmill.PLAN)), "the plan stays");
    }

    @Test
    void sawmillNeedsEverySideABlockAndAPlan() {
        ListInventory[] inv = new ListInventory[1];
        Sawmill m = loaded(inv);
        SawWorld w = new SawWorld();
        inv[0].set(Sawmill.EAST, Stack.of("minecraft:stick"));
        assertTrue(m.result(w).isEmpty(), "sticks are not blocks");
        inv[0].set(Sawmill.EAST, Stack.EMPTY);
        assertTrue(m.result(w).isEmpty());
        inv[0].set(Sawmill.EAST, Stack.of("minecraft:sand"));
        m.button(Sawmill.BUTTON_PLAN);
        assertEquals(Sawmill.PLAN + 1, m.selected());
        assertTrue(m.result(w).isEmpty(), "the selected plan slot is empty");
        m.button(Sawmill.BUTTON_PLAN);
        m.button(Sawmill.BUTTON_PLAN);
        assertEquals(Sawmill.PLAN, m.selected(), "wraps round the three plan slots");
        assertFalse(m.result(w).isEmpty());
    }

    @Test
    void etherealDoublesAndConfigScales() {
        ListInventory[] inv = new ListInventory[1];
        Sawmill m = loaded(inv);
        SawWorld w = new SawWorld();
        inv[0].set(0, Stack.of("mariculture:ethereal_upgrade"));
        Settings.SAWMILL_MULTIPLIER.set(3);
        w.tick(m);
        assertEquals(4 * 3 * 2, m.result(w).count());
        assertEquals(6, Math.max(1, Settings.SAWMILL_SPEED.get() / 100), "650 / 100");
    }

    @Test
    void sawmillSlotsFillFromTheirOwnSide() {
        ListInventory[] inv = new ListInventory[1];
        Sawmill m = loaded(inv);
        Stack plank = Stack.of("minecraft:oak_planks");
        assertTrue(m.canInsert(Sawmill.TOP, plank, Face.UP));
        assertFalse(m.canInsert(Sawmill.TOP, plank, Face.NORTH));
        assertFalse(m.canInsert(Sawmill.TOP, Stack.of("mariculture:custom_block"), Face.UP), "sawmill blocks cannot go back in");
        assertFalse(m.isValid(Sawmill.PLAN, plank));
        assertTrue(m.canExtract(Sawmill.OUT, Face.DOWN));
        assertFalse(m.canExtract(Sawmill.TOP, Face.UP));
    }

    @Test
    void planSizesAndChalk() {
        assertEquals(9, Plans.Type.FLOOR.size);
        assertEquals(12, Plans.Type.SLABS.size);
        assertEquals(2, Plans.Type.GATE.size);
        assertEquals(1, Plans.Type.RF.size);
        assertEquals(Plans.Type.SLABS, Plans.type(Stack.of("mariculture:plan_slabs")));
        assertNull(Plans.type(Stack.of(Plans.BLANK)), "a blank plan is not a plan");
        assertEquals(Plans.Type.RF_WALL, Plans.forBlock(Plans.Kind.WALL, true, false, false, false, false));
        assertEquals(Plans.Type.WALL, Plans.forBlock(Plans.Kind.WALL, false, false, false, false, false));
        assertEquals(Plans.Type.RF, Plans.forBlock(Plans.Kind.OTHER, false, true, true, true, false));
        assertEquals(Plans.Type.LIGHT, Plans.forBlock(Plans.Kind.OTHER, false, false, true, true, false));
        assertEquals(Plans.Type.BLOCK, Plans.forBlock(Plans.Kind.OTHER, false, false, false, false, false));
        assertNull(Plans.forBlock(Plans.Kind.OTHER, false, false, false, true, false), "a chest gives nothing");
        assertNull(Plans.forBlock(Plans.Kind.OTHER, false, false, false, false, true));
        assertEquals(Plans.Type.FLOOR, Plans.forBlock(Plans.Kind.CARPET, false, false, false, false, false));
    }

    @Test
    void facesRotateRoundTripAndAverage() {
        CustomFaces f = CustomFaces.of("a:d", "a:u", "a:n", "a:s", "a:w", "a:e");
        CustomFaces r = f.rotateHorizontal();
        assertEquals("a:w", r.block(Face.NORTH));
        assertEquals("a:e", r.block(Face.SOUTH));
        assertEquals("a:s", r.block(Face.WEST));
        assertEquals("a:n", r.block(Face.EAST));
        assertEquals(f, r.rotateHorizontal().rotateHorizontal().rotateHorizontal(), "four quarter turns");
        assertEquals("a:u", f.flip().block(Face.DOWN));
        assertEquals(f, CustomFaces.of(f.onto("x:y", 1)));
        assertEquals(CustomFaces.DEFAULT, CustomFaces.of(Stack.of("x:y")));
        assertEquals(1.5f, CustomFaces.average(new float[] {1.5f, 1.5f, 3, 0, 3, -1}), 1e-6);
    }

    @Test
    void paintbrushLoadsAndPaintsOnlyChanges() {
        Stack brush = Stack.of(CustomBlocks.PAINTBRUSH);
        assertNull(CustomBlocks.paint(CustomFaces.DEFAULT, Face.UP, brush), "an empty brush does nothing");
        brush = CustomBlocks.load(brush, "minecraft:oak_log", Face.UP);
        CustomFaces painted = CustomBlocks.paint(CustomFaces.DEFAULT, Face.NORTH, brush);
        assertEquals("minecraft:oak_log", painted.block(Face.NORTH));
        assertEquals(Face.UP, painted.side(Face.NORTH));
        assertNull(CustomBlocks.paint(painted, Face.NORTH, brush), "same face again: no use spent");
        CustomFaces clear = CustomBlocks.paint(painted, Face.NORTH, CustomBlocks.loadAir(brush));
        assertEquals(CustomFaces.AIR, clear.block(Face.NORTH));
    }

    @Test
    void fluidDictionaryConvertsByRatioAndCycles() {
        FluidDictionary d = new FluidDictionary();
        SawWorld w = new SawWorld();
        Settings.FLUID_DICTIONARY.set(List.of("water=minecraft:water=2000", "xp=mariculture:xp=200", "xp=other:juice=1000",
                "xp=missing:essence=666", "bad"));
        w.tick(d);
        assertEquals("minecraft:water", d.target(), "first existing entry");
        d.onUse();
        assertEquals("mariculture:xp", d.target());
        assertEquals(0, d.buffer().fill("minecraft:water", 100, true), "other families are refused");
        assertEquals(1000, d.buffer().fill("other:juice", 5000, false), "the buffer holds one ratio of the biggest");
        w.tick(d);
        assertEquals(List.of("DOWN mariculture:xp 200"), w.filled);
        assertTrue(d.buffer().isEmpty());
        d.onUse();
        assertEquals("other:juice", d.target());
        d.onUse();
        assertEquals("minecraft:water", d.target(), "the missing fluid is skipped and it wraps");
        d.onUse();
        d.buffer().fill("mariculture:xp", 100, false);
        w.tick(d);
        assertEquals(1, w.filled.size(), "less than a ratio waits");
        w.refuse = true;
        d.buffer().fill("mariculture:xp", 100, false);
        w.tick(d);
        assertEquals(200, d.buffer().amount(), "nobody takes it: it stays");
    }
}
