package mariculture.domain.diving;

/**
 * Diving gear (diving/Diving.java, DivingEventHandler.java, Snorkel.java, DivingBoots.java, LifeJacket.java,
 * HardcoreDiving.java, 1.3.0). The targets ask these rules what to do each tick; the numbers are the legacy ones.
 *
 * <p>Changes: the boots and lifejacket moved the player on the client only, so they read the keyboard; here they read
 * the player's movement input on the client side the same way. The lifejacket is drawn as a chest armour layer
 * painted from its texture instead of the legacy log-ring model.
 */
public final class Diving {
    public static final String HELMET = "diving_helmet", TOP = "diving_top", PANTS = "diving_pants", BOOTS = "diving_boots",
            SNORKEL = "snorkel", LIFEJACKET = "lifejacket";

    /** Snorkel: one more air every two ticks while the head is under water (Snorkel.java). */
    public static final int SNORKEL_AIR_EVERY_TICKS = 2;
    /** Diving boots: x1.13 horizontal, x0.98 vertical while moving forward in water; sneaking sinks at 0.55. */
    public static final double BOOTS_SPEED = 1.13, BOOTS_VERTICAL = 0.98, BOOTS_SINK = -0.55;
    /** Diving pants: blocks break twice as fast under water, oysters 64 times. */
    public static final float PANTS_BREAK = 2, PANTS_OYSTER_BREAK = 64;
    /** Lifejacket: rises at 0.25 under water and floats at the surface unless sneaking (motion above -0.15 only). */
    public static final double LIFEJACKET_RISE = 0.25, LIFEJACKET_ACTIVE_ABOVE = -0.15;
    /** Hardcore diving runs every five ticks. */
    public static final int HARDCORE_EVERY_TICKS = 5;

    /** ArmorClientHandler: the diving helmet sets the water fog density to 0; here the fog starts 96 and ends 192 blocks out. */
    public static final float HELMET_FOG_NEAR = 96, HELMET_FOG_FAR = 192;

    private Diving() {}

    public static boolean snorkelGivesAir(long gameTime, boolean headUnderWater, boolean creative) {
        return !creative && headUnderWater && gameTime % SNORKEL_AIR_EVERY_TICKS == 0;
    }

    /**
     * HardcoreDiving: every five ticks a player with air left and the head under water loses {@code setting} air,
     * unless wearing any Mariculture helmet (ItemMCBaseArmor implements IDisablesHardcoreDiving). 0 turns it off.
     */
    public static int hardcoreAirLoss(int setting, long gameTime, int air, boolean headUnderWater, boolean maricultureHelmet) {
        if (setting <= 0 || gameTime % HARDCORE_EVERY_TICKS != 0 || air <= 0 || !headUnderWater || maricultureHelmet) return 0;
        return setting;
    }

    /** DivingEventHandler.onBreaking: x2 with the pants in water for blocks the player can harvest, x64 for oysters. */
    public static float breakSpeed(float original, boolean pants, boolean inWater, boolean canHarvest, boolean oyster) {
        if (!pants || !inWater || !canHarvest) return original;
        return original * (oyster ? PANTS_OYSTER_BREAK : PANTS_BREAK);
    }

    /** Motion as x, y, z. */
    public record Motion(double x, double y, double z) {}

    /** DivingBoots.init: forward in water speeds up; otherwise sneaking sinks. Null leaves the motion alone. */
    public static Motion boots(Motion m, boolean inWater, boolean flying, boolean forward, boolean sneaking) {
        if (!inWater || flying) return null;
        if (forward) return new Motion(m.x() * BOOTS_SPEED, m.y() * BOOTS_VERTICAL, m.z() * BOOTS_SPEED);
        if (sneaking) return new Motion(m.x(), BOOTS_SINK, m.z());
        return null;
    }

    /** LifeJacket.init: the new vertical motion, or NaN to leave it. */
    public static double lifejacket(double motionY, boolean headUnderWater, boolean inWater, boolean sneaking) {
        if (motionY <= LIFEJACKET_ACTIVE_ABOVE) return Double.NaN;
        if (headUnderWater) return LIFEJACKET_RISE;
        if (inWater && !sneaking) return 0;
        return Double.NaN;
    }

    /** DivingEventHandler.onAnvilUpdate: the snorkel takes no enchanted books. */
    public static boolean anvilAllowed(String left, String right) {
        return !(isSnorkel(left) && "minecraft:enchanted_book".equals(right)) && !(isSnorkel(right) && "minecraft:enchanted_book".equals(left));
    }

    private static boolean isSnorkel(String item) {
        return ("mariculture:" + SNORKEL).equals(item);
    }
}
