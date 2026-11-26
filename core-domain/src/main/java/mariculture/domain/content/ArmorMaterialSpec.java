package mariculture.domain.content;

import java.util.Objects;

/**
 * A legacy {@code EnumHelper.addArmorMaterial(name, durabilityFactor, reductions, enchantability)}.
 * Durability per piece is {@code factor * slot.baseDurability()}, unchanged since 1.7.
 *
 * <p>A durability factor of 0 means the piece never wears (legacy material durability 0, the F.L.U.D.D.).
 *
 * @param texture armour layer texture name under content-source/textures/armor/ (layer 1; layer 2 is
 *                {@code texture + "_legs"} when {@link #hasLegsLayer})
 */
public record ArmorMaterialSpec(String id, int durabilityFactor, int head, int chest, int legs, int feet,
                                int enchantability, String repairItem, String texture, boolean hasLegsLayer) {
    public ArmorMaterialSpec {
        Ids.requireValid(id);
        Objects.requireNonNull(texture);
        if (durabilityFactor < 0) throw new IllegalArgumentException(id + ": bad durability factor");
        if (repairItem != null) repairItem = Ids.full(repairItem);
    }

    public int defense(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> head;
            case CHEST -> chest;
            case LEGS -> legs;
            case FEET -> feet;
        };
    }

    public int durability(ArmorSlot slot) {
        return durabilityFactor * slot.baseDurability();
    }
}
