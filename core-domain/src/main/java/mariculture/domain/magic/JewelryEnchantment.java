package mariculture.domain.magic;

import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.Rng;

/**
 * The fourteen jewelry enchantments (magic/Magic.java:150-193 weights; magic/enchantments/Enchantment*.java max
 * levels and enchantability windows, 1.3.0). They go on pearls only, never on books, and no two go together
 * (EnchantmentJewelry.java:17-47); worn jewelry adds up the levels of its three pieces.
 *
 * <p>Changes: the One Ring had weight 0, which modern registries reject; it gets 1 and is never offered by the
 * mirror (it only comes from dungeon chests).
 */
public enum JewelryEnchantment {
    SPIDER(3, 1, 1, 25),
    BLINK(5, 3, 25, 40),
    FALL(5, 4, 1, 35),
    FLIGHT(1, 3, 55, 60),
    GLIDE(1, 2, 1, 15),
    HEALTH(4, 3, 30, 45),
    JUMP(6, 4, 1, 35),
    HUNGRY(2, 5, 45, 60),
    ONE_RING(1, 1, 100, 100),
    REPAIR(3, 3, 40, 55),
    RESURRECTION(1, 3, 50, 55),
    SPEED(6, 5, 1, 35),
    STEP_UP(5, 3, 1, 30),
    ELEMENTAL(4, 1, 35, 50);

    public final int weight;
    public final int maxLevel;
    /** EnchantmentJewelry.minLevel / maxLevel: the enchantability window the formulas start from. */
    private final int windowMin, windowMax;

    JewelryEnchantment(int weight, int maxLevel, int windowMin, int windowMax) {
        this.weight = weight;
        this.maxLevel = maxLevel;
        this.windowMin = windowMin;
        this.windowMax = windowMax;
    }

    public String path() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String id() {
        return "mariculture:" + path();
    }

    public static JewelryEnchantment byId(String id) {
        for (JewelryEnchantment e : values()) if (e.id().equals(id)) return e;
        return null;
    }

    /** The integer part both formulas share: level * level / 3 * 2 + level * max level / window max. */
    private int req(int level) {
        return level * level / 3 * 2 + level * maxLevel / windowMax;
    }

    /** EnchantmentJewelry.getMinEnchantability. */
    public int minCost(int level) {
        return req(level) + windowMin + level;
    }

    /** EnchantmentJewelry.getMaxEnchantability. */
    public int maxCost(int level) {
        return req(level) + windowMax + level;
    }

    /** Whether the mirror can roll it (the One Ring is chest loot only). */
    public boolean discoverable() {
        return this != ONE_RING;
    }

    /**
     * ItemPearl.getBiasedEnchantment (core/items/ItemPearl.java:79-107): each pearl colour may override a mirror roll
     * with its own enchantment. Returns an enchantment id, or null to keep the roll.
     */
    public static String biased(PearlColor colour, int level, Rng rng) {
        return switch (colour) {
            case WHITE -> level > 55 && rng.nextInt(5) == 0 ? FLIGHT.id() : null;
            case GREEN -> rng.nextInt(5) == 0 ? JUMP.id() : null;
            case YELLOW -> level > 45 && rng.nextInt(6) == 0 ? HUNGRY.id() : null;
            case ORANGE -> level > 40 && rng.nextInt(6) == 0 ? REPAIR.id() : null;
            case RED -> level > 30 && rng.nextInt(6) == 0 ? HEALTH.id() : null;
            case GOLD -> rng.nextInt(5) == 0 ? FALL.id() : null;
            case BROWN -> rng.nextInt(8) == 0 ? STEP_UP.id() : null;
            case PURPLE -> rng.nextInt(10) == 0 ? GLIDE.id() : null;
            case BLUE -> rng.nextInt(6) == 0 ? SPEED.id() : null;
            case BLACK -> rng.nextInt(12) == 0 ? SPIDER.id() : null;
            case PINK -> level > 50 && rng.nextInt(6) == 0 ? RESURRECTION.id() : null;
            case SILVER -> rng.nextInt(6) == 0 ? "minecraft:unbreaking" : null;
        };
    }
}
