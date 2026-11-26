package mariculture.domain.magic;

/**
 * Jewelry bindings (magic/jewelry/parts/BindingBasic.java, BindingGold.java, 1.3.0). Values are per type in
 * ring, bracelet, necklace order: the crafting item, anvil hits, durability, the chance (in 100) that each
 * enchantment of the ingredients is kept, and the highest enchantment level the binding allows.
 */
public enum Binding {
    BASIC(new String[] {"minecraft:iron_ingot", "minecraft:string", "minecraft:white_wool"}, new int[] {25, 50, 75},
            new int[] {50, 150, 300}, new int[] {50, 50, 50}, new int[] {2, 3, 3}),
    GOLD(new String[] {"minecraft:gold_ingot", "mariculture:golden_silk", "mariculture:golden_thread"}, new int[] {35, 65, 100},
            new int[] {75, 200, 450}, new int[] {95, 85, 75}, new int[] {3, 4, 5});

    private final String[] items;
    private final int[] hits, durability, keepChance, maxLevel;

    Binding(String[] items, int[] hits, int[] durability, int[] keepChance, int[] maxLevel) {
        this.items = items;
        this.hits = hits;
        this.durability = durability;
        this.keepChance = keepChance;
        this.maxLevel = maxLevel;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The crafting item (legacy accepted any wool for the basic necklace; white wool here). */
    public String item(JewelryType t) {
        return items[t.ordinal()];
    }

    public int hits(JewelryType t) {
        return hits[t.ordinal()];
    }

    public int durability(JewelryType t) {
        return durability[t.ordinal()];
    }

    public int keepChance(JewelryType t) {
        return keepChance[t.ordinal()];
    }

    public int maxLevel(JewelryType t) {
        return maxLevel[t.ordinal()];
    }

    public static Binding byId(String id) {
        for (Binding b : values()) if (b.id().equals(id)) return b;
        return null;
    }
}
