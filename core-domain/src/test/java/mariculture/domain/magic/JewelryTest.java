package mariculture.domain.magic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Anvil;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.ScriptedRng;

class JewelryTest {
    private static List<String> grid(String[] rows, String a, String b) {
        List<String> g = new ArrayList<>();
        for (String r : rows) for (char c : r.toCharArray()) g.add(c == 'A' ? a : c == 'B' ? b : "");
        return g;
    }

    @Test
    void partsSetDurabilityHitsAndLimits() {
        // Gold binding necklace (450) with a black pearl (x2), red ring (basic 50 x5).
        assertEquals(900, Jewelry.durability(JewelryType.NECKLACE, Binding.GOLD, JewelryMaterial.BLACK));
        assertEquals(250, Jewelry.durability(JewelryType.RING, Binding.BASIC, JewelryMaterial.RED));
        assertEquals(75, Jewelry.hits(JewelryType.NECKLACE, Binding.GOLD, JewelryMaterial.BLACK), "100 x 0.75");
        assertEquals(4, Jewelry.maxEnchantments(JewelryType.BRACELET, JewelryMaterial.BLACK), "3 + 1");
        assertEquals(5, Jewelry.maxEnchantments(JewelryType.NECKLACE, JewelryMaterial.RED), "7 - 2");
        assertEquals(2, Jewelry.maxLevel(JewelryType.RING, Binding.BASIC, JewelryMaterial.BROWN), "basic ring caps at 2");
        assertEquals(1, Jewelry.maxLevel(JewelryType.BRACELET, Binding.GOLD, JewelryMaterial.RED), "red caps at 1");
    }

    @Test
    void gridsMatchTheThreePatternsAndTheSwap() {
        Jewelry.Match ring = Jewelry.match(grid(Jewelry.pattern(JewelryType.RING), "minecraft:gold_ingot", "mariculture:blue_pearl"), i -> null);
        assertEquals(new Jewelry.Match(JewelryType.RING, Binding.GOLD, JewelryMaterial.BLUE, 35, false), ring);
        Jewelry.Match necklace = Jewelry.match(grid(Jewelry.pattern(JewelryType.NECKLACE), "minecraft:white_wool", "mariculture:pink_pearl"), i -> null);
        assertEquals(JewelryType.NECKLACE, necklace.type());
        assertEquals(225, necklace.hits(), "75 x 3");
        assertNull(Jewelry.match(grid(Jewelry.pattern(JewelryType.RING), "minecraft:gold_ingot", "minecraft:diamond"), i -> null));
        List<String> mixed = grid(Jewelry.pattern(JewelryType.NECKLACE), "minecraft:white_wool", "mariculture:pink_pearl");
        mixed.set(8, "mariculture:blue_pearl");
        assertNull(Jewelry.match(mixed, i -> null), "one material per piece");

        List<String> swap = new ArrayList<>(Collections.nCopies(9, ""));
        swap.set(0, "mariculture:bracelet");
        swap.set(4, "mariculture:gold_pearl");
        Jewelry.Match m = Jewelry.match(swap, i -> i == 0 ? Binding.GOLD : null);
        assertEquals(new Jewelry.Match(JewelryType.BRACELET, Binding.GOLD, JewelryMaterial.GOLD, 21, true), m, "the piece keeps its binding; 65 / 3 hits");
    }

    @Test
    void workedPieceCarriesIngredientEnchantmentsToTheAnvil() {
        Jewelry.Match m = new Jewelry.Match(JewelryType.RING, Binding.BASIC, JewelryMaterial.BLUE, 25, false);
        Stack worked = Jewelry.worked(m, List.of(new Jewelry.Enchant("mariculture:speed", 2), new Jewelry.Enchant("mariculture:jump", 1)),
                new ScriptedRng().ints(0));
        assertTrue(worked.is(Anvil.WORKED));
        assertEquals(25, worked.data().get(Anvil.K_REQUIRED));
        @SuppressWarnings("unchecked")
        Stack piece = Stack.of("mariculture:ring", 1, (Map<String, Object>) worked.data().get(Anvil.K_RESULT_DATA));
        assertEquals(Binding.BASIC, Jewelry.binding(piece));
        assertEquals(List.of(new Jewelry.Enchant("mariculture:jump", 1), new Jewelry.Enchant("mariculture:speed", 2)), Jewelry.pending(piece),
                "shuffled");
        assertTrue(Jewelry.pending(Jewelry.withoutPending(piece)).isEmpty());
    }

    @Test
    void finishingKeepsCapsAndReplacesWhenFull() {
        // A basic ring holds one enchantment at level 2 at most; both kept rolls (<50) fit only one.
        Map<String, Integer> out = Jewelry.finish(JewelryType.RING, Binding.BASIC, JewelryMaterial.BLUE,
                List.of(new Jewelry.Enchant("mariculture:speed", 5), new Jewelry.Enchant("mariculture:jump", 1)), Map.of(), id -> 5,
                new ScriptedRng().ints(10, 10, 0));
        assertEquals(Map.of("mariculture:jump", 1), out, "speed (capped to 2) was replaced by jump; legacy crashed here (BUG-059)");
        Map<String, Integer> lost = Jewelry.finish(JewelryType.RING, Binding.BASIC, JewelryMaterial.BLUE,
                List.of(new Jewelry.Enchant("mariculture:speed", 1)), Map.of(), id -> 5, new ScriptedRng().ints(50));
        assertTrue(lost.isEmpty(), "50 or more loses it with the basic binding");
    }

    @Test
    void enchantmentWindowsAndMirrorLevels() {
        // Speed level 1: 1*1/3*2 + 1*5/35 = 0, so 0 + 1 + 1 = 2 to 0 + 35 + 1 = 36.
        assertEquals(2, JewelryEnchantment.SPEED.minCost(1));
        assertEquals(36, JewelryEnchantment.SPEED.maxCost(1));
        // Flight level 3: 9/3*2 = 6, 3*3/60 = 0 -> 6 + 55 + 3 = 64.
        assertEquals(64, JewelryEnchantment.FLIGHT.minCost(3));
        assertArrayEquals(new int[] {4, 5, 6}, MirrorEnchanting.nextLevels(MirrorEnchanting.MAGIC, new int[] {1, 2, 3}));
        assertArrayEquals(new int[] {1, 2, 3}, MirrorEnchanting.nextLevels(MirrorEnchanting.MAGIC, new int[] {28, 29, 30}));
        assertEquals("mariculture:speed", JewelryEnchantment.biased(PearlColor.BLUE, 10, new ScriptedRng().ints(0)));
        assertNull(JewelryEnchantment.biased(PearlColor.PINK, 50, new ScriptedRng()), "resurrection needs more than 50 levels");
    }

    @Test
    void mirrorRollsJewelryEnchantmentsInsideTheirWindow() {
        // Pearl enchantability 2: j = 1 + 0 + 0 = 1, k = 1 + 30, f = 0 -> 31. Only windows holding 31 qualify.
        List<Jewelry.Enchant> rolled = MirrorEnchanting.roll(new ScriptedRng().ints(0, 0).floats(0.5f, 0.5f).ints(0, 44), 2, 30,
                MirrorEnchanting.jewelryCandidates(), (a, b) -> false);
        assertEquals(1, rolled.size());
        JewelryEnchantment e = JewelryEnchantment.byId(rolled.get(0).id());
        int lv = rolled.get(0).level();
        assertTrue(e.minCost(lv) <= 31 && 31 <= e.maxCost(lv), e + " " + lv);
        assertTrue(MirrorEnchanting.roll(new ScriptedRng(), 0, 30, MirrorEnchanting.jewelryCandidates(), (a, b) -> true).isEmpty());
    }

    @Test
    void wornStrengthAndElementalMaterials() {
        List<WornJewelry.Piece> worn = List.of(
                new WornJewelry.Piece(0, JewelryMaterial.RED, Map.of("mariculture:speed", 2, "mariculture:elemental", 1), false),
                new WornJewelry.Piece(1, JewelryMaterial.BLUE, Map.of("mariculture:speed", 3), false),
                new WornJewelry.Piece(2, JewelryMaterial.GOLD, Map.of("mariculture:speed", 5), true));
        assertEquals(5, WornJewelry.strength(worn, "mariculture:speed"), "broken pieces do not count");
        List<WornJewelry.Piece> twoRings = List.of(
                new WornJewelry.Piece(0, JewelryMaterial.RED, Map.of("mariculture:speed", 2), false, JewelryType.RING),
                new WornJewelry.Piece(1, JewelryMaterial.RED, Map.of("mariculture:speed", 3), false, JewelryType.RING),
                new WornJewelry.Piece(2, JewelryMaterial.RED, Map.of("mariculture:speed", 1), false, JewelryType.NECKLACE));
        assertEquals(4, WornJewelry.strength(twoRings, "mariculture:speed", false), "only the strongest ring counts");
        assertEquals(6, WornJewelry.strength(twoRings, "mariculture:speed", true), "extra slots stack when the setting is on");
        assertEquals(List.of(JewelryMaterial.RED, JewelryMaterial.BLUE, JewelryMaterial.GOLD), Elemental.active(worn));
        assertEquals(List.of(0), WornJewelry.wearSlots(worn, "mariculture:elemental", JewelryMaterial.RED));
        assertTrue(Elemental.defend(JewelryMaterial.RED, Elemental.Damage.FIRE, false).cancel());
        assertEquals(80, Elemental.defend(JewelryMaterial.PURPLE, Elemental.Damage.MAGIC, true).wearOneIn());
        assertEquals(0, Elemental.breakWear(JewelryMaterial.BROWN, false, 0), "only dirt and grass (BUG-060)");
        assertTrue(WornJewelry.accepts(1, "mariculture:bracelet"));
        assertFalse(WornJewelry.accepts(0, "mariculture:bracelet"));
    }

    @Test
    void effectsFollowLegacyNumbers() {
        assertEquals(new JewelryEffects.Fall(true, 0, 3), JewelryEffects.fall(20, 2), "17 of 20 absorbed, wear 17/5");
        assertEquals(new JewelryEffects.Fall(false, 13, 2), JewelryEffects.fall(23, 1));
        assertEquals(10, JewelryEffects.oneUpHealth(4, 6, 2));
        assertEquals(-1, JewelryEffects.oneUpHealth(10, 6, 2));
        assertEquals(new JewelryEffects.Repair(true, true, 2), JewelryEffects.repair(200, 3));
        assertEquals(new JewelryEffects.Resurrection(true, false, 150, 4), JewelryEffects.resurrection(3, true));
        assertEquals(1.5, JewelryEffects.stepHeight(2));
        assertFalse(JewelryEffects.canFly(1));
    }

    @Test
    void anvilFinishesASwapKeepingTheOldEnchantments() {
        Jewelry.Match swap = new Jewelry.Match(JewelryType.BRACELET, Binding.GOLD, JewelryMaterial.GOLD, 21, true);
        Anvil a = new Anvil();
        mariculture.domain.machine.ListInventory inv = new mariculture.domain.machine.ListInventory(a);
        inv.set(0, Jewelry.worked(swap, List.of(new Jewelry.Enchant("mariculture:jump", 1)),
                List.of(new Jewelry.Enchant("mariculture:speed", 2)), new ScriptedRng()));
        ScriptedRng rng = new ScriptedRng().ints(0);
        for (int i = 0; i < 20; i++) assertEquals(Anvil.WORKED_HIT, a.work(n -> true, rng));
        assertEquals(Anvil.FINISHED, a.work(n -> true, rng));
        assertEquals(Jewelry.create(JewelryType.BRACELET, Binding.GOLD, JewelryMaterial.GOLD), inv.get(0), "new material, no leftover lists");
        assertEquals(Map.of("mariculture:speed", 2, "mariculture:jump", 1), inv.applied.get(0));
        assertNull(Jewelry.finishOnAnvil(Stack.of("mariculture:aluminum_sheet"), rng), "only jewelry is finished");
    }

    @Test
    void craftingReadsTheGridAndItsEnchantments() {
        List<Stack> g = new ArrayList<>();
        for (String id : grid(Jewelry.pattern(JewelryType.RING), "minecraft:iron_ingot", "mariculture:green_pearl")) {
            g.add(id.isEmpty() ? Stack.EMPTY : Stack.of(id));
        }
        Stack worked = Jewelry.craft(g, i -> i == 1 ? List.of(new Jewelry.Enchant("mariculture:jump", 2)) : List.of(), new ScriptedRng());
        assertTrue(worked.is(Anvil.WORKED));
        assertNull(((Map<?, ?>) worked.data().get(Anvil.K_RESULT_DATA)).get(Jewelry.K_KEPT));
        g.set(0, Stack.of("minecraft:diamond"));
        assertTrue(Jewelry.craft(g, i -> List.of(), new ScriptedRng()).isEmpty());
    }

    @Test
    void tickPlanAppliesBonusesAndWearsOnItsIntervals() {
        Map<String, Integer> worn = Map.of("mariculture:speed", 2, "mariculture:step_up", 1, "mariculture:jump", 2,
                "mariculture:hungry", 1, "mariculture:flight", 2);
        JewelryTick.Plan p = JewelryTick.plan(new JewelryTick.State(1200, true, true, false, false, true, false, 19),
                id -> worn.getOrDefault(id, 0));
        assertEquals(0.1, p.speed(), 1e-9, "0.05 per level while sprinting on the ground");
        assertEquals(1.0 - JewelryTick.VANILLA_STEP, p.stepBonus(), 1e-9, "step height 0.5 * (1 + 1)");
        assertEquals(2, p.jumpAmplifier(), "jump boost III is 0.3, legacy 0.15 x 2");
        assertTrue(p.mayFly());
        assertEquals(0.05f, p.feedSaturation(), 1e-6f);
        assertEquals(Map.of("mariculture:speed", 1, "mariculture:flight", 1, "mariculture:hungry", 1), p.wear(),
                "tick 1200: speed (1200) and flight (300) intervals; step-up waits for 900s; feeding wears");
        JewelryTick.Plan walking = JewelryTick.plan(new JewelryTick.State(1201, false, true, false, false, false, false, 20),
                id -> worn.getOrDefault(id, 0));
        assertEquals(0, walking.speed());
        assertTrue(walking.wear().isEmpty(), "no interval reached, full food");
        assertEquals(-1f, walking.feedSaturation());
        assertFalse(JewelryTick.jumpWears(9));
        assertTrue(JewelryTick.jumpWears(10));
    }

    @Test
    void blinkRangeIsSixteenBlocksPerLevel() {
        assertTrue(Blink.inRange(16, 0, 0, 1));
        assertFalse(Blink.inRange(12, 12, 0, 1), "17 blocks away (legacy's cube root let this through, BUG-061)");
        assertTrue(Blink.inRange(30, 10, 0, 2));
        assertFalse(Blink.inRange(1, 0, 0, 0));
    }

    @Test
    void mirrorsCycleTheirLevelsAndRepairWithTheirMaterial() {
        Stack magic = Stack.of("mariculture:magic_mirror");
        assertArrayEquals(new int[] {1, 2, 3}, MirrorEnchanting.levels(magic));
        Stack cycled = MirrorEnchanting.cycle(magic);
        assertArrayEquals(new int[] {4, 5, 6}, MirrorEnchanting.levels(cycled));
        Stack top = Stack.of("mariculture:celestial_mirror", 1, Map.of(MirrorEnchanting.K_LEVELS, List.of(58, 59, 60)));
        assertArrayEquals(new int[] {31, 32, 33}, MirrorEnchanting.levels(MirrorEnchanting.cycle(top)), "past 60 back to 31");
        assertArrayEquals(new int[3], MirrorEnchanting.levels(Stack.of("mariculture:mirror")), "the plain mirror does not enchant");
        assertEquals("#mariculture:pearls", MirrorEnchanting.repairItem("mariculture:magic_mirror"));
        assertFalse(MirrorEnchanting.jewelryCompatible("mariculture:speed", "mariculture:jump"));
        assertTrue(MirrorEnchanting.jewelryCompatible("mariculture:speed", "minecraft:unbreaking"));
    }
}
