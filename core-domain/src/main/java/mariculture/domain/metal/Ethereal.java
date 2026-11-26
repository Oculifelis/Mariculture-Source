package mariculture.domain.metal;

import java.util.List;

/**
 * Ethereal-upgrade melting (core/handlers/CrucibleHandler.java:40-50, 136-139, 1.3.0): at 1500 degrees or more a
 * crucible with an ethereal upgrade melts any enchanted item into liquid experience before trying its melting table,
 * and returns the item with its enchantments removed. The experience is 100 mB (the balanced volume of "xp",
 * core/Core.java:224) times the item's enchantment value.
 */
public final class Ethereal {
    public static final String UPGRADE = "ethereal";
    public static final int MELTING_POINT = 1500;
    public static final int XP_PER_VALUE = 100;
    /** The highest enchantment weight in vanilla (common enchantments); adapters may report the registry's own. */
    public static final int VANILLA_MAX_WEIGHT = 10;

    private Ethereal() {}

    /**
     * EnchantHelper.getEnchantmentValue (core/helpers/EnchantHelper.java:68-79): each enchantment adds its level times
     * (max weight + 1 - its weight), so rare enchantments count more; the sum over the max weight, times 25, at least 1.
     *
     * @param enchantments {level, weight} pairs
     */
    public static int value(List<int[]> enchantments, int maxWeight) {
        int max = Math.max(1, maxWeight);
        int volume = 0;
        for (int[] e : enchantments) volume += e[0] * ((max + 1) - e[1]);
        return (int) Math.max(1, ((double) volume / max) * 5 * 5);
    }

    /** Liquid experience from one enchanted item. */
    public static int experience(List<int[]> enchantments, int maxWeight) {
        return XP_PER_VALUE * value(enchantments, maxWeight);
    }
}
