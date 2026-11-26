package mariculture.domain.metal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.plugin.Plugins;

/**
 * Anvil (core/tile/TileAnvil, 1.3.0). Holds one stack; hitting it with the hammer works it. A recipe input first
 * becomes a worked item ("required" hits, holding the result), and each further hit adds one until the result
 * replaces it. Anything else damaged and repairable is repaired one point per hit (plus 1 in 3 per fortune level)
 * at a cost of experience.
 *
 * <p>Experience: legacy only checked that the player had some (hasXP) for working, and drained a share of a level
 * for repairs. The return codes are kept for the autohammer: 1 started, -1 worked, 5000 finished, repair cost
 * (+5000 when fully repaired), 0 nothing happened.
 *
 * <p>Changes: right-click takes the stack or puts one held item on (BlockRenderedMachine.onBlockActivated); the
 * enchantment modifier sums every enchantment (legacy re-read the first one each time, BUG-057) and the hammer's
 * efficiency counts in 25ths (legacy divided integers, so it never did).
 */
public final class Anvil extends Machine {
    public static final String WORKED = "mariculture:worked_item";
    public static final String K_WORKED = "worked", K_REQUIRED = "required", K_RESULT = "result", K_RESULT_COUNT = "result_count",
            K_RESULT_DATA = "result_data";
    public static final int STARTED = 1, WORKED_HIT = -1, FINISHED = 5000;
    private static final int[] SLOT = {0};

    public record Recipe(String input, String output, int count, int hits) {}

    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    static {
        // core/Recipes.java:165-182 (1.3.0 hits)
        add("minecraft:gold_block", "mariculture:gold_sheet", 8, 50);
        add("mariculture:aluminum_block", "mariculture:aluminum_sheet", 8, 30);
        add("mariculture:titanium_block", "mariculture:titanium_sheet", 8, 75);
        add("minecraft:bone", "minecraft:bone_meal", 7, 5);
        add("minecraft:poppy", "minecraft:red_dye", 2, 5);
        add("minecraft:blue_orchid", "minecraft:light_blue_dye", 2, 5);
        add("minecraft:allium", "minecraft:magenta_dye", 2, 5);
        add("minecraft:azure_bluet", "minecraft:light_gray_dye", 2, 5);
        add("minecraft:red_tulip", "minecraft:red_dye", 2, 5);
        add("minecraft:orange_tulip", "minecraft:orange_dye", 2, 5);
        add("minecraft:white_tulip", "minecraft:light_gray_dye", 2, 5);
        add("minecraft:pink_tulip", "minecraft:pink_dye", 2, 5);
        add("minecraft:oxeye_daisy", "minecraft:light_gray_dye", 2, 5);
        add("minecraft:dandelion", "minecraft:yellow_dye", 3, 5);
        add("minecraft:sunflower", "minecraft:yellow_dye", 4, 5);
        add("minecraft:lilac", "minecraft:magenta_dye", 4, 5);
        add("minecraft:rose_bush", "minecraft:red_dye", 4, 5);
        add("minecraft:peony", "minecraft:pink_dye", 4, 5);
        // WorldPlus.java:87-98 addCrushRecipe: two dye from dried coral on the anvil, one green dye from (vanilla) kelp.
        for (mariculture.domain.worldgen.Coral.Colour c : mariculture.domain.worldgen.Coral.Colour.values()) {
            add("mariculture:" + c.dried(), c.dye, 2, 5);
        }
        add("minecraft:kelp", "minecraft:green_dye", 1, 5);
    }

    public static final MachineType TYPE = new MachineType("anvil", Anvil::new, null);

    private static void add(String in, String out, int count, int hits) {
        RECIPES.put(in, new Recipe(in, out, count, hits));
    }

    public static Recipe recipe(String item) {
        return mariculture.domain.plugin.Tweaks.anvil(item, () -> builtIn(item));
    }

    private static Recipe builtIn(String item) {
        Recipe r = RECIPES.get(item);
        if (r != null || !Plugins.active(Plugins.BIOMES_O_PLENTY)) return r;
        String dye = Plugins.BOP_CRUSHING.get(item);
        return dye == null ? null : new Recipe(item, dye, 2, 5);
    }

    /** The table, then the crushing of active plugins. */
    public static List<Recipe> recipes() {
        List<Recipe> out = new ArrayList<>(RECIPES.values());
        if (Plugins.active(Plugins.BIOMES_O_PLENTY)) Plugins.BOP_CRUSHING.forEach((in, dye) -> out.add(new Recipe(in, dye, 2, 5)));
        return mariculture.domain.plugin.Tweaks.anvil(out);
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 1;
    }

    @Override
    public int[] slots(Face side) {
        return SLOT;
    }

    /** One item: legacy allowed a stack (via automation) and replaced all of it with one worked item (BUG-058). */
    @Override
    public int slotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return side == Face.UP && inv.get(0).isEmpty();
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return !inv.get(0).is(WORKED) || side == Face.UP;
    }

    /** Right-click: take what is on the anvil, or put one of the held item down. Returns what the player gets. */
    public List<Stack> use(Stack held) {
        Stack on = inv.get(0);
        if (!on.isEmpty()) {
            inv.set(0, Stack.EMPTY);
            return List.of(on);
        }
        if (!held.isEmpty()) inv.set(0, held.withCount(1));
        return List.of();
    }

    public static Stack worked(Recipe r) {
        return worked(r, Stack.of(r.output(), r.count()));
    }

    /** A worked item finishing into {@code result} (with its item data: jewelry) after the recipe's hits. */
    public static Stack worked(Recipe r, Stack result) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put(K_WORKED, 0);
        d.put(K_REQUIRED, r.hits());
        d.put(K_RESULT, result.item());
        d.put(K_RESULT_COUNT, result.count());
        if (!result.data().isEmpty()) d.put(K_RESULT_DATA, result.data());
        return Stack.of(WORKED, 1, d);
    }

    /**
     * TileAnvil.workItem for recipe inputs and worked items. {@code hasXp} answers legacy hasXP(player, n): some
     * experience and either n/17 of a level's progress or a whole level. Returns the code; the slot is updated.
     * {@code null} means the stack is not workable (the target tries a repair instead).
     */
    public Integer work(java.util.function.IntPredicate hasXp) {
        return work(hasXp, null);
    }

    /** As {@link #work(java.util.function.IntPredicate)}; a finished jewelry piece rolls its enchantments with {@code rng}. */
    public Integer work(java.util.function.IntPredicate hasXp, mariculture.domain.random.Rng rng) {
        Stack s = inv.get(0);
        if (s.isEmpty()) return 0;
        if (!s.is(WORKED)) {
            Recipe r = RECIPES.get(s.item());
            if (r == null) return null;
            if (!hasXp.test(1)) return 0;
            inv.set(0, worked(r));
            return STARTED;
        }
        if (!hasXp.test(2)) return 0;
        int worked = integer(s.data().get(K_WORKED), 0) + 1;
        int required = integer(s.data().get(K_REQUIRED), 1);
        if (worked >= required) {
            Object result = s.data().get(K_RESULT);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = s.data().get(K_RESULT_DATA) instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
            Stack done = result instanceof String id ? Stack.of(id, integer(s.data().get(K_RESULT_COUNT), 1), data) : Stack.EMPTY;
            mariculture.domain.magic.Jewelry.Finished piece = rng == null ? null : mariculture.domain.magic.Jewelry.finishOnAnvil(done, rng);
            inv.set(0, piece == null ? done : piece.piece());
            if (piece != null && !piece.enchantments().isEmpty()) inv.enchant(0, piece.enchantments());
            return FINISHED;
        }
        Map<String, Object> d = new LinkedHashMap<>(s.data());
        d.put(K_WORKED, worked);
        inv.set(0, s.withData(d));
        return WORKED_HIT;
    }

    /**
     * Repair cost as a share of a level (TileAnvil.workItem): 1 / xpBarCap / 4 * modifier * efficiency, with the
     * enchantment modifier max(1, sum(level + enchantability range) / 3).
     */
    public static float repairCost(int xpBarCap, List<int[]> enchantLevelAndRange, int efficiencyLevel) {
        int modifier = 1;
        if (!enchantLevelAndRange.isEmpty()) {
            for (int[] e : enchantLevelAndRange) modifier += e[0] + e[1];
            modifier = Math.max(1, modifier / 3);
        }
        float efficiency = 1.0f - efficiencyLevel / 25f;
        return 1.0f / xpBarCap / 4 * modifier * efficiency;
    }

    /** Legacy hasXP: some experience and either n/17 of the bar or at least one level. */
    public static boolean hasXp(float progress, int level, float xp) {
        return xp > 0 && (progress >= xp * 0.05882353f || level > 0);
    }

    /** Damage removed by one repair hit: one plus 1 in 3 per fortune level. */
    public static int repairAmount(int fortune, mariculture.domain.random.Rng rng) {
        int n = 1;
        for (int i = 0; i < fortune; i++) if (rng.nextInt(3) == 0) n++;
        return n;
    }

    /** The autohammer's experience use for a repair code. */
    public static int repairCode(float cost, boolean fullyRepaired) {
        int ret = Math.max(1, (int) (cost / 0.05882353f));
        return fullyRepaired ? ret + FINISHED : ret;
    }

    @Override
    public void tick(MachineWorld world) {}
}
