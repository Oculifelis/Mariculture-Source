package mariculture.domain.magic;

/**
 * Ring, bracelet and necklace (magic/jewelry/ItemJewelry.java:33-47, 1.3.0). {@code maxEnchantments} is the base
 * number of enchantments a piece holds before its material's bonus; {@code slot} is its place in the mirror.
 */
public enum JewelryType {
    RING(1, 0), BRACELET(3, 1), NECKLACE(7, 2);

    public final int maxEnchantments;
    public final int slot;

    JewelryType(int maxEnchantments, int slot) {
        this.maxEnchantments = maxEnchantments;
        this.slot = slot;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String item() {
        return "mariculture:" + id();
    }

    /** The type an item id names, or null. */
    public static JewelryType byItem(String item) {
        for (JewelryType t : values()) if (t.item().equals(item)) return t;
        return null;
    }
}
