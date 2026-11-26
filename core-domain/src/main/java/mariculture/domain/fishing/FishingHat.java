package mariculture.domain.fishing;

import mariculture.domain.fish.Fish;
import mariculture.domain.fish.Salinity;
import mariculture.domain.pearl.PearlColor;

/**
 * The bucket hat and its twelve pearl colours. Legacy kept one item with a "Color" NBT int
 * (fishery/items/ItemArmorFishingHat); here each colour is its own item, matching the compendium.
 * Effects apply to fish caught by the wearer (ICaughtAliveModifier / IHelmetFishManipulator).
 */
public final class FishingHat {
    public static final String PLAIN_ID = "bucket_hat";
    /** Enchanting level used for the purple hat (MirrorEnchantHelper.buildEnchantmentList(rand, loot, 10, 10)). */
    public static final int PURPLE_ENCHANT_LEVEL = 10;

    /**
     * What a hat changes. Null fields mean "no change"; a null {@code gender} means the legacy random draw.
     *
     * @param aliveModifier added to the caught-alive chance (getModifier: 15 for the plain hat, 5 for dyed ones)
     */
    public record Effects(double aliveModifier, Integer gender, Salinity salinity, Integer temperature, boolean forceLoot,
                          boolean forceFish, Integer yHeight, boolean nether, boolean alwaysDead, boolean enchantLoot) {}

    private FishingHat() {}

    public static String itemId(PearlColor color) {
        return color == null ? PLAIN_ID : color.id() + "_" + PLAIN_ID;
    }

    /** ItemArmorFishingHat 1.3.0 lines 48-50 and 210-286. {@code color} is null for the plain hat. */
    public static Effects effects(PearlColor color) {
        if (color == null) return new Effects(15, null, null, null, false, false, null, false, false, false);
        Integer gender = color == PearlColor.BLUE ? Integer.valueOf(Fish.MALE) : color == PearlColor.PINK ? Integer.valueOf(Fish.FEMALE) : null;
        Salinity salt = switch (color) {
            case WHITE, GREEN, RED -> Salinity.FRESH;
            case SILVER -> Salinity.SALINE;
            case YELLOW -> Salinity.BRACKISH;
            default -> null;
        };
        Integer temperature = color == PearlColor.GREEN ? Integer.valueOf(24) : color == PearlColor.RED ? Integer.valueOf(90) : null;
        return new Effects(5, gender, salt, temperature, color == PearlColor.GOLD, color == PearlColor.ORANGE,
                color == PearlColor.BROWN ? Integer.valueOf(10) : null, color == PearlColor.RED, color == PearlColor.BLACK,
                color == PearlColor.PURPLE);
    }
}
