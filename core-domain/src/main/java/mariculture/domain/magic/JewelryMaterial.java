package mariculture.domain.magic;

import mariculture.domain.pearl.PearlColor;

/**
 * Pearl materials (magic/jewelry/parts/MaterialPearl*.java, 1.3.0). Values are per type in ring, bracelet,
 * necklace order: extra enchantments (added to the type's base), the highest enchantment level, and the
 * modifiers for anvil repair cost, anvil hits and durability. {@code colour} is the legacy name colour code.
 */
public enum JewelryMaterial {
    BLACK(PearlColor.BLACK, '7', new int[] {1, 1, 1}, new int[] {4, 4, 4}, 1.5f, new float[] {0.75f, 0.75f, 0.75f}, new float[] {2f, 2f, 2f}),
    BLUE(PearlColor.BLUE, 'b', new int[] {0, 0, 0}, new int[] {4, 4, 4}, 0.75f, new float[] {1f, 1f, 1f}, new float[] {1f, 1f, 1f}),
    BROWN(PearlColor.BROWN, '6', new int[] {1, 1, 1}, new int[] {5, 5, 5}, 1f, new float[] {0.75f, 0.75f, 0.75f}, new float[] {1.25f, 1.25f, 1.25f}),
    GOLD(PearlColor.GOLD, 'e', new int[] {0, 2, 1}, new int[] {4, 5, 4}, 2f, new float[] {1f, 1f, 1f}, new float[] {0.8f, 0.8f, 0.8f}),
    GREEN(PearlColor.GREEN, '2', new int[] {1, 1, 1}, new int[] {4, 4, 4}, 0.5f, new float[] {1f, 1f, 1f}, new float[] {1f, 1f, 1f}),
    ORANGE(PearlColor.ORANGE, '6', new int[] {0, 0, 0}, new int[] {3, 3, 3}, 1.5f, new float[] {2.5f, 2.5f, 2.5f}, new float[] {1.5f, 1.5f, 1.5f}),
    PINK(PearlColor.PINK, 'd', new int[] {0, 0, 0}, new int[] {3, 3, 3}, 1.5f, new float[] {3f, 3f, 3f}, new float[] {0.75f, 0.75f, 0.75f}),
    PURPLE(PearlColor.PURPLE, '5', new int[] {1, 0, 0}, new int[] {5, 3, 3}, 1.5f, new float[] {0.75f, 0.75f, 0.75f}, new float[] {1f, 1f, 1f}),
    RED(PearlColor.RED, 'c', new int[] {0, -1, -2}, new int[] {1, 1, 2}, 2.5f, new float[] {2f, 3.5f, 3.5f}, new float[] {5f, 7.5f, 7.5f}),
    SILVER(PearlColor.SILVER, 'f', new int[] {0, 0, 1}, new int[] {4, 4, 5}, 1.75f, new float[] {2f, 2f, 2f}, new float[] {0.9f, 0.9f, 0.9f}),
    WHITE(PearlColor.WHITE, 'f', new int[] {0, 0, 0}, new int[] {4, 4, 4}, 1.75f, new float[] {2.5f, 2.5f, 2.5f}, new float[] {0.75f, 0.75f, 0.75f}),
    YELLOW(PearlColor.YELLOW, 'e', new int[] {0, 0, 0}, new int[] {4, 4, 4}, 0.5f, new float[] {2f, 2f, 2f}, new float[] {0.8f, 0.8f, 0.8f});

    public final PearlColor pearl;
    public final char colour;
    private final int[] extraEnchantments, maxLevel;
    public final float repairModifier;
    private final float[] hitsModifier, durabilityModifier;

    JewelryMaterial(PearlColor pearl, char colour, int[] extraEnchantments, int[] maxLevel, float repairModifier, float[] hitsModifier,
            float[] durabilityModifier) {
        this.pearl = pearl;
        this.colour = colour;
        this.extraEnchantments = extraEnchantments;
        this.maxLevel = maxLevel;
        this.repairModifier = repairModifier;
        this.hitsModifier = hitsModifier;
        this.durabilityModifier = durabilityModifier;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String item() {
        return "mariculture:" + pearl.id() + "_pearl";
    }

    public int extraEnchantments(JewelryType t) {
        return extraEnchantments[t.ordinal()];
    }

    public int maxLevel(JewelryType t) {
        return maxLevel[t.ordinal()];
    }

    public float hitsModifier(JewelryType t) {
        return hitsModifier[t.ordinal()];
    }

    public float durabilityModifier(JewelryType t) {
        return durabilityModifier[t.ordinal()];
    }

    public static JewelryMaterial byId(String id) {
        for (JewelryMaterial m : values()) if (m.id().equals(id)) return m;
        return null;
    }

    public static JewelryMaterial byItem(String item) {
        for (JewelryMaterial m : values()) if (m.item().equals(item)) return m;
        return null;
    }
}
