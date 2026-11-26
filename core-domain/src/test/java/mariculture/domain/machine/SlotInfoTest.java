package mariculture.domain.machine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mariculture.domain.fishery.Hatchery;
import mariculture.domain.maritech.Autofisher;
import mariculture.domain.maritech.DnaExtractor;
import mariculture.domain.maritech.EnergyItem;
import mariculture.domain.maritech.PoweredMachine;
import mariculture.domain.metal.Crucible;
import mariculture.domain.metal.Vat;

/** Menu slot roles, faint items and shift-click order (new in the port). */
class SlotInfoTest {

    @Test
    void everyMenuSlotHasARoleAndGhostsComeFromTheTables() {
        for (MachineType t : Machines.all()) {
            Machine m = t.create();
            for (int s = 0; s < m.slotCount(); s++) {
                SlotInfo info = m.slotInfo(s);
                assertTrue(info != null && info.role() != null && !info.what().isEmpty(), t.id() + " slot " + s);
            }
        }
        Autofisher f = new Autofisher();
        assertEquals(SlotInfo.Role.UPGRADE, f.slotInfo(0).role());
        assertEquals(SlotInfo.Role.FUEL, f.slotInfo(PoweredMachine.BATTERY_SLOT).role());
        assertEquals(SlotInfo.Role.CATALYST, f.slotInfo(Autofisher.ROD).role());
        assertTrue(f.slotInfo(Autofisher.ROD).ghosts().contains("minecraft:fishing_rod"));
        assertTrue(f.slotInfo(Autofisher.ROD).ghosts().contains("mariculture:titanium_fishing_rod"));
        assertEquals(SlotInfo.Role.INPUT, f.slotInfo(Autofisher.BAIT).role());
        assertTrue(f.slotInfo(Autofisher.BAIT).ghosts().contains("minecraft:bread"));
        assertEquals(SlotInfo.Role.OUTPUT, f.slotInfo(Autofisher.OUT).role());
        assertTrue(new Crucible().slotInfo(Crucible.FUEL).ghosts().contains("minecraft:coal"));
        assertEquals(SlotInfo.Role.OUTPUT, new Hatchery().slotInfo(1).role());
        assertEquals(SlotInfo.Role.OUTPUT, new Vat().slotInfo(Vat.OUT).role());
        assertFalse(new Vat().slotInfo(Vat.IN).ghosts().isEmpty(), "vat inputs from the vat recipes");
    }

    @Test
    void shiftClickGoesToTheSlotMadeForTheItem() {
        Autofisher f = new Autofisher();
        new ListInventory(f);
        assertArrayEquals(new int[] {PoweredMachine.BATTERY_SLOT}, f.quickMoveOrder(Stack.of("mariculture:" + EnergyItem.COPPER)));
        assertArrayEquals(new int[] {Autofisher.ROD}, f.quickMoveOrder(Stack.of("minecraft:fishing_rod")));
        assertArrayEquals(new int[] {5, 6, 7, 8, 9, 10}, f.quickMoveOrder(Stack.of("minecraft:bread")));
        assertArrayEquals(new int[] {0, 1, 2}, f.quickMoveOrder(Stack.of("mariculture:basic_speed_upgrade")));
        assertArrayEquals(new int[0], f.quickMoveOrder(Stack.of("minecraft:stick")), "never into an output");

        Crucible c = new Crucible();
        new ListInventory(c);
        assertEquals(Crucible.FUEL, c.quickMoveOrder(Stack.of("minecraft:coal"))[0], "fuel to the fuel slot");
        assertEquals(Crucible.IN_A, c.quickMoveOrder(Stack.of("minecraft:iron_door"))[0], "metal to a melting slot");
        assertEquals(Crucible.FLUID_IN, c.quickMoveOrder(Stack.of("minecraft:water_bucket"))[0]);

        DnaExtractor d = new DnaExtractor();
        new ListInventory(d);
        assertEquals(DnaExtractor.FLUID_IN, d.quickMoveOrder(Stack.of("mariculture:fish_oil_bucket"))[0]);
    }
}
