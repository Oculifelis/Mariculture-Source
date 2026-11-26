package mariculture.domain.content;

/** Armour slots in the order of the legacy {@code int[]} reduction arrays (helmet first). */
public enum ArmorSlot {
    HEAD, CHEST, LEGS, FEET;

    /** Vanilla base durability per slot, the same in 1.7 (ItemArmor.maxDamageArray) and today. */
    public int baseDurability() {
        return switch (this) {
            case HEAD -> 11;
            case CHEST -> 16;
            case LEGS -> 15;
            case FEET -> 13;
        };
    }
}
