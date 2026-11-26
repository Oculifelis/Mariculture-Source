package mariculture.domain.metal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.Stack;
import mariculture.domain.random.ScriptedRng;

class MetallurgyTest {
    private static void run(FakeWorld w, Machine m, int ticks) {
        for (int i = 0; i < ticks; i++) w.tick(m);
    }

    @Test
    void crucibleBurnsCoalAndMeltsIceIntoWater() {
        Crucible c = new Crucible();
        ListInventory inv = new ListInventory(c);
        inv.set(Crucible.FUEL, Stack.of("minecraft:coal"));
        inv.set(Crucible.IN_A, Stack.of("minecraft:ice", 2));
        FakeWorld w = new FakeWorld(1);
        run(w, c, 1200);
        assertTrue(inv.get(Crucible.FUEL).isEmpty(), "coal burnt");
        assertTrue(inv.get(Crucible.IN_A).isEmpty(), "both ice melted");
        assertEquals("minecraft:water", c.tanks().get(0).fluid());
        assertEquals(2000, c.tanks().get(0).amount());
        assertTrue(c.temperature() > 0);
    }

    @Test
    void crucibleWaitsForTheMeltingPoint() {
        Crucible c = new Crucible();
        ListInventory inv = new ListInventory(c);
        inv.set(Crucible.FUEL, Stack.of("minecraft:coal"));
        inv.set(Crucible.IN_A, Stack.of("minecraft:iron_bars"));
        run(new FakeWorld(1), c, 600);
        assertEquals(1, inv.get(Crucible.IN_A).count(), "1538 degrees are far off without heating upgrades");
    }

    @Test
    void etherealUpgradeMeltsEnchantmentsIntoExperience() {
        // Sharpness III (weight 10): 3 * (11 - 10) / 10 * 25 = 7.5, so value 7 and 700 mB.
        assertEquals(7, Ethereal.value(List.of(new int[] {3, 10}), 10));
        // Mending (weight 2) counts more: 1 * 9 / 10 * 25 = 22.5, so 22.
        assertEquals(2200, Ethereal.experience(List.of(new int[] {1, 2}), 10));
        assertEquals(1, Ethereal.value(List.of(), 10), "at least 1");

        Crucible c = new Crucible();
        ListInventory inv = new ListInventory(c);
        inv.set(0, Stack.of("mariculture:ethereal_upgrade"));
        inv.set(Crucible.IN_A, Stack.of("minecraft:diamond_sword"));
        inv.enchanted.put(Crucible.IN_A, List.of(new int[] {3, 10}));
        c.load(Map.of("temperature", Crucible.MAX_TEMP));
        run(new FakeWorld(1), c, 400);
        assertTrue(inv.get(Crucible.IN_A).isEmpty(), "melted");
        assertTrue(inv.get(Crucible.OUT).is("minecraft:diamond_sword"), "returned without its enchantments");
        assertEquals("mariculture:xp", c.tanks().get(0).fluid());
        assertEquals(700, c.tanks().get(0).amount());
    }

    @Test
    void enchantedItemsWaitWithoutTheUpgradeOrBelow1500() {
        Crucible c = new Crucible();
        ListInventory inv = new ListInventory(c);
        inv.set(Crucible.IN_A, Stack.of("minecraft:diamond_sword"));
        inv.enchanted.put(Crucible.IN_A, List.of(new int[] {3, 10}));
        c.load(Map.of("temperature", Crucible.MAX_TEMP));
        run(new FakeWorld(1), c, 400);
        assertEquals(1, inv.get(Crucible.IN_A).count(), "no ethereal upgrade");

        Crucible cold = new Crucible();
        ListInventory ci = new ListInventory(cold);
        ci.set(0, Stack.of("mariculture:ethereal_upgrade"));
        ci.set(Crucible.IN_A, Stack.of("minecraft:diamond_sword"));
        ci.enchanted.put(Crucible.IN_A, List.of(new int[] {3, 10}));
        cold.load(Map.of("temperature", Crucible.MAX_TEMP * 1400 / 2000));
        run(new FakeWorld(1), cold, 400);
        assertEquals(1, ci.get(Crucible.IN_A).count(), "below 1500 degrees");
    }

    @Test
    void meltingTableAndRandomDustFluids() {
        Melting.Recipe r = Melting.find("mariculture:aluminum_ingot", 2000, t -> t.equals("c:ingots/aluminum"), null);
        assertEquals("mariculture:molten_aluminum", r.fluid());
        assertEquals(MetalRates.INGOT, r.amount());
        assertNull(Melting.find("mariculture:aluminum_ingot", 659, t -> t.equals("c:ingots/aluminum"), null), "below 660");
        assertEquals(MetalRates.ORE, Melting.find("minecraft:raw_iron", 2000, t -> t.equals("c:raw_materials/iron"), null).amount());
        Melting.Recipe dust = Melting.find("mariculture:ironic_dust", 2000, t -> false, null);
        // Only tin's ingots exist: aluminium and copper are skipped; tin rolls 1 in 8 and is also the fallback.
        assertEquals("mariculture:molten_tin", Melting.fluid(dust, new ScriptedRng().ints(5), t -> t.equals("c:ingots/tin")).fluid());
        assertNull(Melting.fluid(dust, new ScriptedRng(), t -> false), "no linked metal present");
    }

    @Test
    void fuelsByItemTagAndFluidPath() {
        assertEquals(1680, CrucibleFuels.item("minecraft:coal_block", t -> t.equals("c:storage_blocks/coal")).maxTempPer());
        assertEquals(1500, CrucibleFuels.fluid("minecraft:lava").maxTemp());
        assertEquals(1750, CrucibleFuels.fluid("othermod:oil").maxTemp());
        assertTrue(CrucibleFuels.item("minecraft:tnt", t -> false).explosive());
    }

    @Test
    void ingotCasterFillsEverySlotItCan() {
        Machine m = Caster.INGOT.create();
        ListInventory inv = new ListInventory(m);
        m.load(Map.of("tank0_fluid", "mariculture:molten_iron", "tank0_amount", MetalRates.INGOT * 3 + 10));
        run(new FakeWorld(1), m, 100);
        int ingots = 0;
        for (int i = 0; i < 4; i++) if (inv.get(i).is("minecraft:iron_ingot")) ingots++;
        assertEquals(3, ingots);
        assertEquals(10, m.tanks().get(0).amount());
        assertEquals(3, m.onUse().size());
    }

    @Test
    void waterCastsSaltAndOtherModsMetalsNeedTheirTag() {
        assertEquals("mariculture:salt", Casting.result(Casting.Kind.NUGGET, "minecraft:water", 250, t -> null).item());
        assertNull(Casting.result(Casting.Kind.INGOT, "mariculture:molten_tin", 1000, t -> null));
        assertEquals("othermod:tin_ingot", Casting.result(Casting.Kind.INGOT, "mariculture:molten_tin", 1000, t -> "othermod:tin_ingot").item());
    }

    @Test
    void vatMakesBurntBricksAndObsidian() {
        Vat v = new Vat();
        ListInventory inv = new ListInventory(v);
        v.load(Map.of("tank0_fluid", "minecraft:lava", "tank0_amount", 600));
        inv.set(Vat.IN, Stack.of("minecraft:brick", 3));
        run(new FakeWorld(1), v, 200);
        assertEquals("mariculture:burnt_brick", inv.get(Vat.OUT).item());
        assertEquals(2, inv.get(Vat.OUT).count(), "600 mB of lava covers two bricks");
        assertEquals(1, inv.get(Vat.IN).count());

        Vat o = new Vat();
        ListInventory oi = new ListInventory(o);
        o.load(Map.of("tank0_fluid", "minecraft:lava", "tank0_amount", 1000, "tank1_fluid", "minecraft:water", "tank1_amount", 1000));
        run(new FakeWorld(1), o, 400);
        assertEquals("minecraft:obsidian", oi.get(Vat.OUT).item(), "water/lava either way round");
        assertTrue(o.tanks().get(0).isEmpty() && o.tanks().get(1).isEmpty());
    }

    @Test
    void anvilWorksARecipeHitByHit() {
        Anvil a = new Anvil();
        ListInventory inv = new ListInventory(a);
        inv.set(0, Stack.of("mariculture:aluminum_block"));
        assertEquals(0, a.work(n -> false), "no experience, no work");
        assertEquals(Anvil.STARTED, a.work(n -> true));
        assertTrue(inv.get(0).is(Anvil.WORKED));
        for (int i = 0; i < 29; i++) assertEquals(Anvil.WORKED_HIT, a.work(n -> true));
        assertEquals(Anvil.FINISHED, a.work(n -> true));
        assertEquals(Stack.of("mariculture:aluminum_sheet", 8), inv.get(0));
        assertNull(a.work(n -> true), "a sheet is not workable: the target tries a repair");
        assertTrue(Anvil.hasXp(0, 1, 2) && !Anvil.hasXp(0, 0, 2) && !Anvil.hasXp(1, 1, 0));
        assertEquals(1f / 7 / 4, Anvil.repairCost(7, java.util.List.of(), 0), 1e-6);
    }

    @Test
    void autohammerWorksTheAnvilBelowItsSideWithTankExperience() {
        Autohammer h = new Autohammer();
        ListInventory hi = new ListInventory(h);
        hi.set(0, Stack.of(Autohammer.HAMMER));
        Anvil a = new Anvil();
        ListInventory ai = new ListInventory(a);
        ai.set(0, Stack.of("minecraft:bone"));
        FakeWorld w = new FakeWorld(1);
        w.machines.put("0,-1,-1", a);
        w.below = new mariculture.domain.machine.FluidAmount("mariculture:xp", 1000);
        run(w, h, 50 * 8);
        assertEquals(Stack.of("minecraft:bone_meal", 7), ai.get(0));
        assertTrue(w.below.amount() < 1000, "experience used");
    }
}

