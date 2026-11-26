package mariculture.domain.magic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import mariculture.domain.machine.Stack;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.Rng;

/**
 * Enchanting in a magic or celestial mirror (magic/MirrorEnchantHelper.java, gui/ContainerMirror.java:24-56,
 * ItemMagicMirror.java, 1.3.0). The mirror offers three levels from its range; spending that many experience
 * levels enchants the item in its fourth slot like an enchanting table that reaches level 60.
 *
 * <p>Above level 30 a rolled enchantment may go one level higher (twice above 55), past its normal maximum, except
 * protection, fortune, single-level and jewelry enchantments. Legacy tested {@code rand.nextInt() < level} on an
 * unbounded int, which is true about half the time; that half chance is kept.
 */
public final class MirrorEnchanting {
    /** An enchantment the item accepts, with its table numbers. {@code noBonus}: protection and fortune. */
    public record Candidate(String id, int weight, int minLevel, int maxLevel, java.util.function.IntUnaryOperator minCost,
            java.util.function.IntUnaryOperator maxCost, boolean jewelry, boolean noBonus) {}

    /** A mirror's level range: magic 1-30, celestial 31-60 (Magic.java:81-82). */
    public record Range(int min, int max) {}

    public static final Range MAGIC = new Range(1, 30), CELESTIAL = new Range(31, 60);

    private MirrorEnchanting() {}

    public static final String K_LEVELS = "levels";
    /** ItemPearl.getItemEnchantability. */
    public static final int PEARL_ENCHANTABILITY = 2;

    /** The range a mirror item enchants in: none for the plain mirror (jewelry storage only). */
    public static Range range(String mirrorItem) {
        return switch (mirrorItem) {
            case "mariculture:magic_mirror" -> MAGIC;
            case "mariculture:celestial_mirror" -> CELESTIAL;
            default -> null;
        };
    }

    /** getIsRepairable (ItemMirror.java:69, ItemMagicMirror.java:76): an item id or "#tag", null for other items. */
    public static String repairItem(String mirrorItem) {
        return switch (mirrorItem) {
            case "mariculture:mirror" -> "mariculture:aluminum_ingot";
            case "mariculture:magic_mirror" -> "#mariculture:pearls";
            case "mariculture:celestial_mirror" -> "mariculture:droplet_magic";
            default -> null;
        };
    }

    /** The levels a mirror stack offers now (its data, or the range's start). */
    public static int[] levels(Stack mirror) {
        Range r = range(mirror.item());
        if (r == null) return new int[3];
        if (mirror.data().get(K_LEVELS) instanceof List<?> l && l.size() == 3) {
            int[] out = new int[3];
            for (int i = 0; i < 3; i++) out[i] = l.get(i) instanceof Number n ? n.intValue() : 0;
            if (out[0] >= r.min() && out[2] <= r.max()) return out;
        }
        return startLevels(r);
    }

    /** ItemMagicMirror.onItemRightClick while sneaking: the mirror with its next three levels. */
    public static Stack cycle(Stack mirror) {
        Range r = range(mirror.item());
        if (r == null) return mirror;
        int[] next = nextLevels(r, levels(mirror));
        Map<String, Object> d = new LinkedHashMap<>(mirror.data());
        d.put(K_LEVELS, List.of(next[0], next[1], next[2]));
        return mirror.withData(d);
    }

    /**
     * ContainerMirror.enchantItem: the enchantments one click at {@code level} adds to the item in the enchanting
     * slot, or an empty list when nothing rolls. {@code pearl} is the item's pearl colour (null otherwise).
     */
    public static List<Jewelry.Enchant> enchant(Rng rng, int enchantability, int level, List<Candidate> candidates,
            BiPredicate<String, String> compatible, PearlColor pearl, boolean book, java.util.function.ToIntFunction<String> maxLevelOf) {
        List<Jewelry.Enchant> rolled = roll(rng, enchantability, level, pearl != null ? jewelryCandidates() : candidates, compatible);
        if (rolled.isEmpty()) return rolled;
        if (pearl != null) return forPearl(pearl, level, rolled, maxLevelOf, rng);
        return book ? forBook(rolled, rng) : rolled;
    }

    /** Jewelry enchantments never go together (EnchantmentJewelry.canApplyTogether); others ask the target. */
    public static boolean jewelryCompatible(String a, String b) {
        return a.equals(b) ? false : JewelryEnchantment.byId(a) == null || JewelryEnchantment.byId(b) == null;
    }

    /** The three levels a fresh mirror offers. */
    public static int[] startLevels(Range r) {
        return new int[] {r.min(), r.min() + 1, r.min() + 2};
    }

    /** ItemMagicMirror.onItemRightClick while sneaking: the next three levels, back to the start past the top. */
    public static int[] nextLevels(Range r, int[] current) {
        int first = current[2] + 1 > r.max() ? r.min() : current[0] + 3;
        return new int[] {first, first + 1, first + 2};
    }

    /** The jewelry enchantments as candidates for a pearl (EnchantmentJewelry.canApplyAtEnchantingTable). */
    public static List<Candidate> jewelryCandidates() {
        List<Candidate> out = new ArrayList<>();
        for (JewelryEnchantment e : JewelryEnchantment.values()) {
            if (e.discoverable()) out.add(new Candidate(e.id(), e.weight, 1, e.maxLevel, e::minCost, e::maxCost, true, false));
        }
        return out;
    }

    /**
     * MirrorEnchantHelper.buildEnchantmentList: the enchantments one click gives, or an empty list when the item cannot
     * be enchanted. {@code compatible} answers Enchantment.canApplyTogether.
     */
    public static List<Jewelry.Enchant> roll(Rng rng, int enchantability, int level, List<Candidate> candidates,
            BiPredicate<String, String> compatible) {
        if (enchantability <= 0) return List.of();
        int chance = Math.max(1, 75 - level);
        int j = enchantability / 2;
        j = 1 + rng.nextInt((j >> 1) + 1) + rng.nextInt((j >> 1) + 1);
        int k = j + level;
        float f = (rng.nextFloat() + rng.nextFloat() - 1.0f) * 0.15f;
        int l = Math.max(1, (int) (k * (1.0f + f) + 0.5f));
        Map<String, Weighted> map = available(rng, l, level, candidates);
        List<Jewelry.Enchant> out = new ArrayList<>();
        if (map.isEmpty()) return out;
        Weighted first = pick(rng, map);
        out.add(first.enchant);
        for (int i = l; rng.nextInt(chance) <= i; i >>= 1) {
            map.values().removeIf(w -> {
                for (Jewelry.Enchant e : out) if (!compatible.test(e.id(), w.enchant.id())) return true;
                return false;
            });
            if (map.isEmpty()) break;
            out.add(pick(rng, map).enchant);
        }
        return out;
    }

    private record Weighted(Jewelry.Enchant enchant, int weight) {}

    /** MirrorEnchantHelper.mapEnchantmentData: the highest level of each candidate the enchantability reaches. */
    private static Map<String, Weighted> available(Rng rng, int enchantability, int level, List<Candidate> candidates) {
        Map<String, Weighted> map = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            for (int lv = c.minLevel(); lv <= c.maxLevel(); lv++) {
                if (c.jewelry() && enchantability > c.maxCost().applyAsInt(lv)) continue;
                if (enchantability < c.minCost().applyAsInt(lv)) continue;
                int chosen = lv;
                if (level > 30 && !c.noBonus() && c.maxLevel() != 1 && !c.jewelry()) {
                    int tries = level > 55 ? 2 : 1;
                    for (int t = 0; t < tries; t++) if (rng.nextInt(2) == 0) chosen++;
                }
                map.put(c.id(), new Weighted(new Jewelry.Enchant(c.id(), chosen), Math.max(1, c.weight())));
            }
        }
        return map;
    }

    private static Weighted pick(Rng rng, Map<String, Weighted> map) {
        int total = 0;
        for (Weighted w : map.values()) total += w.weight;
        int r = rng.nextInt(total);
        for (Weighted w : map.values()) {
            r -= w.weight;
            if (r < 0) return w;
        }
        return map.values().iterator().next();
    }

    /**
     * ContainerMirror.enchantItem for a pearl: for each rolled enchantment the pearl's colour may substitute its own at a
     * random level (and stop there); otherwise the roll is applied. {@code maxLevelOf} gives an enchantment's maximum.
     */
    public static List<Jewelry.Enchant> forPearl(PearlColor colour, int level, List<Jewelry.Enchant> rolled,
            java.util.function.ToIntFunction<String> maxLevelOf, Rng rng) {
        List<Jewelry.Enchant> out = new ArrayList<>();
        for (Jewelry.Enchant e : rolled) {
            String biased = JewelryEnchantment.biased(colour, level, rng);
            if (biased != null) {
                out.add(new Jewelry.Enchant(biased, rng.nextInt(Math.max(1, maxLevelOf.applyAsInt(biased))) + 1));
                break;
            }
            out.add(e);
        }
        return out;
    }

    /** ContainerMirror.enchantItem for a book: all rolled enchantments but one random one when there are several. */
    public static List<Jewelry.Enchant> forBook(List<Jewelry.Enchant> rolled, Rng rng) {
        if (rolled.size() <= 1) return rolled;
        List<Jewelry.Enchant> out = new ArrayList<>(rolled);
        out.remove(rng.nextInt(out.size()));
        return out;
    }
}
