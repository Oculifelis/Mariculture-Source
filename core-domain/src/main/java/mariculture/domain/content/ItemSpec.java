package mariculture.domain.content;

import java.util.Objects;

/**
 * An item with a generated flat model. {@code texture} is under textures/item/; {@code kind} tells
 * the targets which item class to use.
 */
public record ItemSpec(String id, String englishName, String legacy, String texture, int maxStack, ItemKind kind) {
    public ItemSpec {
        Ids.requireValid(id);
        Objects.requireNonNull(englishName);
        Objects.requireNonNull(texture);
        Objects.requireNonNull(kind);
        if (maxStack < 1 || maxStack > 99) throw new IllegalArgumentException(id + ": bad stack size " + maxStack);
        if ((kind instanceof ItemKind.Rod || kind instanceof ItemKind.Armor || kind instanceof ItemKind.FluidBucket || kind instanceof ItemKind.FluidContainer fc && !fc.heatBottle()) && maxStack != 1) {
            throw new IllegalArgumentException(id + ": tools, armour and buckets stack to 1");
        }
    }

    public ItemSpec(String id, String englishName, String legacy, String texture, int maxStack) {
        this(id, englishName, legacy, texture, maxStack, ItemKind.PLAIN);
    }
}
