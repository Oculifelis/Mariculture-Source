package mariculture.domain.magic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Anvil;
import mariculture.domain.random.Rng;

/**
 * Jewelry pieces (magic/JewelryHandler.java, magic/jewelry/ItemJewelry.java, ShapedJewelryRecipe.java,
 * ShapelessJewelryRecipe.java, 1.3.0). A piece is a ring, bracelet or necklace item whose data names its binding and
 * pearl material. It is crafted as a worked item that the anvil finishes; the enchantments on the ingredients
 * (enchanted pearls) travel with it and are rolled onto the finished piece.
 *
 * <p>Changes: legacy matched ingredients by item and metadata only, so the material-swap recipe (a piece plus a
 * pearl) gave whichever binding its HashMap listed first; here the piece keeps its own binding. Legacy finishing
 * removed a random enchantment from the piece's original list once it was full, and crashed when that list was
 * empty (a new piece with more kept enchantments than it holds, BUG-059); here a random enchantment already on
 * the piece is replaced. Legacy's swap stored the old piece itself as the anvil result, so the material never
 * changed; here the swap makes the new material and the old piece's enchantments are kept as its existing ones.
 */
public final class Jewelry {
    public static final String K_BINDING = "binding", K_MATERIAL = "material", K_PENDING = "pending_enchantments",
            K_KEPT = "kept_enchantments", K_SPECIAL = "special";
    /** The One Ring (Magic.java:249-256): a ring with no binding or material, found in dungeon chests. */
    public static final String ONE_RING = "one_ring";

    /** An enchantment and level (ids are registry ids, for example "mariculture:speed"). */
    public record Enchant(String id, int level) {}

    /** A crafting grid that makes a worked piece: the type, parts, anvil hits and whether it swaps a material. */
    public record Match(JewelryType type, Binding binding, JewelryMaterial material, int hits, boolean swap) {}

    private static final Map<JewelryType, String[]> PATTERNS = Map.of(
            JewelryType.RING, new String[] {"ABA", "A A", "AAA"},
            JewelryType.BRACELET, new String[] {"A A", "B B", " B "},
            JewelryType.NECKLACE, new String[] {"BAB", "B B", "BBB"});

    private Jewelry() {}

    /** The 3x3 pattern for a type: A is the binding item, B the pearl (JewelryHandler.java:22-41). */
    public static String[] pattern(JewelryType t) {
        return PATTERNS.get(t).clone();
    }

    public static Map<String, Object> data(Binding b, JewelryMaterial m) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put(K_BINDING, b.id());
        d.put(K_MATERIAL, m.id());
        return d;
    }

    public static Stack create(JewelryType t, Binding b, JewelryMaterial m) {
        return Stack.of(t.item(), 1, data(b, m));
    }

    public static Stack oneRing() {
        return Stack.of(JewelryType.RING.item(), 1, Map.of(K_SPECIAL, ONE_RING));
    }

    public static Binding binding(Stack s) {
        return s.data().get(K_BINDING) instanceof String id ? Binding.byId(id) : null;
    }

    public static JewelryMaterial material(Stack s) {
        return s.data().get(K_MATERIAL) instanceof String id ? JewelryMaterial.byId(id) : null;
    }

    public static boolean isOneRing(Stack s) {
        return ONE_RING.equals(s.data().get(K_SPECIAL));
    }

    /** The model a piece shows: "one_ring", "<binding>_<material>", or "" for a plain item (26.1 select key). */
    public static String modelKey(Stack s) {
        if (isOneRing(s)) return ONE_RING;
        Binding b = binding(s);
        JewelryMaterial m = material(s);
        return b == null || m == null ? "" : b.id() + "_" + m.id();
    }

    /** The same as an integer for custom_model_data overrides (1.20.1, 1.21.1): 1000, binding * 100 + material + 1, or 0. */
    public static int modelIndex(Stack s) {
        if (isOneRing(s)) return 1000;
        Binding b = binding(s);
        JewelryMaterial m = material(s);
        return b == null || m == null ? 0 : b.ordinal() * 100 + m.ordinal() + 1;
    }

    /** Durability of a stack's piece. */
    public static int durability(Stack s) {
        JewelryType t = JewelryType.byItem(s.item());
        return t == null ? 0 : durability(t, binding(s), material(s));
    }

    /** ItemJewelry.getMaxDamage: the binding's durability times the material's modifier (100 without parts). */
    public static int durability(JewelryType t, Binding b, JewelryMaterial m) {
        if (b == null || m == null) return 100;
        return (int) (b.durability(t) * m.durabilityModifier(t));
    }

    /** Magic.addJewelry: anvil hits to finish a new piece. */
    public static int hits(JewelryType t, Binding b, JewelryMaterial m) {
        return (int) (b.hits(t) * m.hitsModifier(t));
    }

    /** Enchantments a piece holds (type base plus the material's bonus). */
    public static int maxEnchantments(JewelryType t, JewelryMaterial m) {
        return t.maxEnchantments + (m == null ? 0 : m.extraEnchantments(t));
    }

    /** The highest enchantment level the parts allow (JewelryHandler.getLevel). */
    public static int maxLevel(JewelryType t, Binding b, JewelryMaterial m) {
        if (b == null || m == null) return 0;
        return Math.min(b.maxLevel(t), m.maxLevel(t));
    }

    /**
     * Matches a crafting grid (row-major item ids, "" for empty) against the shaped jewelry patterns (3x3 grids
     * only), or a single piece plus a single pearl for the material swap. {@code pieceBinding} reads the binding of
     * the jewelry item at a grid index (swap only).
     */
    public static Match match(List<String> grid, IntFunction<Binding> pieceBinding) {
        if (grid.size() == 9) {
            for (JewelryType t : JewelryType.values()) {
                Match m = matchShaped(t, grid);
                if (m != null) return m;
            }
        }
        JewelryType type = null;
        JewelryMaterial material = null;
        Binding binding = null;
        int filled = 0;
        for (int i = 0; i < grid.size(); i++) {
            String id = grid.get(i);
            if (id.isEmpty()) continue;
            filled++;
            JewelryType t = JewelryType.byItem(id);
            if (t != null && type == null) {
                type = t;
                binding = pieceBinding.apply(i);
            } else if (JewelryMaterial.byItem(id) != null && material == null) {
                material = JewelryMaterial.byItem(id);
            } else {
                return null;
            }
        }
        if (filled != 2 || type == null || material == null || binding == null) return null;
        return new Match(type, binding, material, hits(type, binding, material) / 3, true);
    }

    private static Match matchShaped(JewelryType t, List<String> grid) {
        String[] rows = PATTERNS.get(t);
        String pearl = null;
        String bindingItem = null;
        for (int i = 0; i < 9; i++) {
            char c = rows[i / 3].charAt(i % 3);
            String id = grid.get(i);
            if (c == ' ') {
                if (!id.isEmpty()) return null;
            } else if (c == 'B') {
                if (id.isEmpty() || (pearl != null && !pearl.equals(id))) return null;
                pearl = id;
            } else {
                if (id.isEmpty() || (bindingItem != null && !bindingItem.equals(id))) return null;
                bindingItem = id;
            }
        }
        JewelryMaterial m = JewelryMaterial.byItem(pearl);
        if (m == null) return null;
        for (Binding b : Binding.values()) {
            if (b.item(t).equals(bindingItem)) return new Match(t, b, m, hits(t, b, m), false);
        }
        return null;
    }

    /**
     * The worked item a match makes: it finishes into the piece after the match's hits and carries the ingredients'
     * enchantments in a shuffled order (ShapedJewelryRecipe.getCraftingResult). For a swap, the old piece's own
     * enchantments are not rolled again (ShapelessJewelryRecipe.java:94 skips jewelry) but passed as {@code kept}.
     */
    public static Stack worked(Match m, List<Enchant> ingredientEnchantments, List<Enchant> kept, Rng rng) {
        Map<String, Object> d = data(m.binding(), m.material());
        if (!ingredientEnchantments.isEmpty()) {
            List<Enchant> shuffled = new ArrayList<>(ingredientEnchantments);
            for (int i = shuffled.size() - 1; i > 0; i--) java.util.Collections.swap(shuffled, i, rng.nextInt(i + 1));
            d.put(K_PENDING, encode(shuffled));
        }
        if (!kept.isEmpty()) d.put(K_KEPT, encode(kept));
        String item = m.type().item();
        return Anvil.worked(new Anvil.Recipe(item, item, 1, Math.max(1, m.hits())), Stack.of(item, 1, d));
    }

    /**
     * The crafting table's result for a grid (all nine cells of a 3x3 grid, or just the cells of a smaller one), or
     * {@link Stack#EMPTY}. {@code enchantsAt} reads the loader enchantments of the stack at an index.
     */
    public static Stack craft(List<Stack> grid, IntFunction<List<Enchant>> enchantsAt, Rng rng) {
        List<String> ids = new ArrayList<>();
        for (Stack s : grid) ids.add(s.isEmpty() ? "" : s.item());
        Match m = match(ids, i -> binding(grid.get(i)));
        if (m == null) return Stack.EMPTY;
        List<Enchant> rolled = new ArrayList<>();
        List<Enchant> kept = new ArrayList<>();
        for (int i = 0; i < grid.size(); i++) {
            if (grid.get(i).isEmpty()) continue;
            (JewelryType.byItem(grid.get(i).item()) != null ? kept : rolled).addAll(enchantsAt.apply(i));
        }
        return worked(m, rolled, kept, rng);
    }

    public static Stack worked(Match m, List<Enchant> ingredientEnchantments, Rng rng) {
        return worked(m, ingredientEnchantments, List.of(), rng);
    }

    private static List<Map<String, Object>> encode(List<Enchant> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Enchant e : list) out.add(Map.of("id", e.id(), "level", e.level()));
        return out;
    }

    private static List<Enchant> decode(Object o) {
        List<Enchant> out = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                if (x instanceof Map<?, ?> e && e.get("id") instanceof String id && e.get("level") instanceof Number n) {
                    out.add(new Enchant(id, n.intValue()));
                }
            }
        }
        return out;
    }

    /** The enchantments a finished piece still has to roll (from {@link #worked}). */
    public static List<Enchant> pending(Stack piece) {
        return decode(piece.data().get(K_PENDING));
    }

    /** The piece without its pending and kept lists. */
    public static Stack withoutPending(Stack piece) {
        Map<String, Object> d = new LinkedHashMap<>(piece.data());
        d.remove(K_PENDING);
        d.remove(K_KEPT);
        return piece.withData(d);
    }

    /** A piece the anvil finished and the enchantments the target puts on it. */
    public record Finished(Stack piece, Map<String, Integer> enchantments) {}

    /**
     * TileAnvil.java:163-166: the anvil's result, finished if it is a jewelry piece with enchantments to roll or keep;
     * null when it is not one.
     */
    public static Finished finishOnAnvil(Stack result, Rng rng) {
        JewelryType t = JewelryType.byItem(result.item());
        if (t == null || !(result.data().containsKey(K_PENDING) || result.data().containsKey(K_KEPT))) return null;
        Map<String, Integer> existing = new LinkedHashMap<>();
        for (Enchant e : decode(result.data().get(K_KEPT))) existing.put(e.id(), e.level());
        Binding b = binding(result);
        JewelryMaterial m = material(result);
        Map<String, Integer> out = b == null || m == null ? existing
                : finish(t, b, m, pending(result), existing, Jewelry::enchantMaxLevel, rng);
        return new Finished(withoutPending(result), out);
    }

    /** An enchantment's normal maximum: the jewelry table, unbreaking 3; others only by the parts' cap. */
    public static int enchantMaxLevel(String id) {
        JewelryEnchantment e = JewelryEnchantment.byId(id);
        if (e != null) return e.maxLevel;
        return "minecraft:unbreaking".equals(id) ? 3 : Integer.MAX_VALUE;
    }

    /**
     * JewelryHandler.finishJewelry: each pending enchantment is kept with the binding's chance and capped at the parts'
     * level; one already on the piece may instead go up a level (1 in 101 - chance). Once the piece is full, a random
     * enchantment on it is replaced. Returns the piece's enchantments in order.
     */
    public static Map<String, Integer> finish(JewelryType t, Binding b, JewelryMaterial m, List<Enchant> pending,
            Map<String, Integer> existing, ToIntFunction<String> enchantMaxLevel, Rng rng) {
        Map<String, Integer> out = new LinkedHashMap<>(existing);
        int chance = b.keepChance(t);
        int max = maxEnchantments(t, m);
        int cap = maxLevel(t, b, m);
        int total = out.size();
        for (Enchant e : pending) {
            if (rng.nextInt(100) >= chance) continue;
            if (total >= max && !out.isEmpty()) {
                List<String> keys = new ArrayList<>(out.keySet());
                out.remove(keys.get(rng.nextInt(keys.size())));
            }
            int add = Math.min(e.level(), cap);
            int current = out.getOrDefault(e.id(), 0);
            if (current != 0 && current < enchantMaxLevel.applyAsInt(e.id()) && rng.nextInt(1 + 100 - chance) == 0) {
                current = Math.min(current + add, cap);
            } else if (current == 0) {
                current = add;
            }
            if (current > 0) out.put(e.id(), current);
            total++;
        }
        return out;
    }
}
