package mariculture.domain.magic;

/**
 * Blink (magic/enchantments/EnchantmentBlink.java, core/network/PacketTeleport.java, 1.3.0): with the activate key
 * held, right-clicking the air looks for the block the player aims at and teleports them on top of it, if the two
 * blocks above are free and it is within 16 blocks per Blink level. One point of wear per blink.
 *
 * <p>Changes: legacy took the cube root of the squared distance (plus 6), so the range check almost never failed
 * and any block up to the 2000-block ray trace was reachable (BUG-061); here the straight-line distance is used.
 * The ray trace limit and the time between blinks are settings.
 * The activate key is its own keybinding (V) that blinks when pressed.
 */
public final class Blink {
    public static final int BLOCKS_PER_LEVEL = 16;

    private Blink() {}

    /** Whether a blink from the player's feet to the top of the aimed block is in range. */
    public static boolean inRange(double dx, double dy, double dz, int strength) {
        if (strength <= 0) return false;
        double max = Math.min(strength * BLOCKS_PER_LEVEL, mariculture.domain.config.Settings.BLINK_RAY_TRACE.get());
        return dx * dx + dy * dy + dz * dz <= max * max;
    }

    /** Whether a player may blink again, {@code sinceLastMs} after their last blink. */
    public static boolean cooledDown(long sinceLastMs) {
        return sinceLastMs >= mariculture.domain.config.Settings.BLINK_COOLDOWN_MS.get();
    }

    /** How far the client looks: Blink III on all three pieces (the server checks the real range). */
    public static final int CLIENT_LOOK = 3 * JewelryEnchantment.BLINK.maxLevel * BLOCKS_PER_LEVEL;
}
