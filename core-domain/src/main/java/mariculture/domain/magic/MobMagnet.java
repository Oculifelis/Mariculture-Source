package mariculture.domain.magic;

import java.util.LinkedHashMap;
import java.util.Map;

import mariculture.domain.machine.Stack;

/**
 * Mob Magnet (magic/ItemMobMagnet.java, MagicEventHandler.java:213-226, 1.3.0). Killing a mob while holding an unbound
 * magnet binds it to that mob's type; right-clicking a bound magnet pulls every mob of the type within 32 blocks of the
 * player (each axis) to the block above the player's feet, with the enderman portal sound. It has no durability
 * (ItemMCDamageable(0): attemptDamageItem never breaks it).
 *
 * <p>Changes: legacy matched by entity class, so a zombie magnet also pulled every subclass (husks, drowned, zombie
 * villagers); here only the exact type is pulled. Legacy counted any kill whose direct damage source was the player;
 * that is kept (arrows do not bind).
 */
public final class MobMagnet {
    public static final String K_MOB = "mob";
    /** Half the box edge around the player (ItemMobMagnet.java:45, expand(32, 32, 32)). */
    public static final double RANGE = 32;

    private MobMagnet() {}

    /** The magnet bound to {@code entityType}, or null if it is already bound or the kill does not count. */
    public static Stack bind(Stack magnet, String entityType, boolean victimIsPlayer, boolean directKillerIsPlayer) {
        if (victimIsPlayer || !directKillerIsPlayer || entityType == null || bound(magnet) != null) return null;
        Map<String, Object> d = new LinkedHashMap<>(magnet.data());
        d.put(K_MOB, entityType);
        return magnet.withData(d);
    }

    /** The entity type a magnet is bound to, or null. */
    public static String bound(Stack magnet) {
        return magnet.data().get(K_MOB) instanceof String s && !s.isEmpty() ? s : null;
    }

    /** Where pulled mobs land: the block above the player's feet (legacy truncated the coordinates; floored here). */
    public static int[] target(double x, double y, double z) {
        return new int[] {(int) Math.floor(x), (int) Math.floor(y + 1), (int) Math.floor(z)};
    }
}
