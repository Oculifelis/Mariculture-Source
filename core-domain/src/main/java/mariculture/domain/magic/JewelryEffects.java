package mariculture.domain.magic;

/**
 * What the worn jewelry enchantments do (magic/enchantments/Enchantment*.java, MagicEventHandler.java,
 * ResurrectionTracker.java, core/config/Enchantments.java defaults, 1.3.0). {@code strength} is always the summed
 * level over the worn pieces ({@link WornJewelry#strength}); "wear" is points of damage spread over the pieces with
 * the enchantment.
 *
 * <p>Changes: speed, jump and step-up are attribute bonuses held while worn instead of client-side motion nudges
 * (legacy applied them only on the client and only with a key held); glide and spider are on while worn (legacy
 * toggled them with a key). Resurrection lets the death happen and restores inventory and armour on respawn
 * (legacy cancelled the death event, which left the player at zero health).
 */
public final class JewelryEffects {
    // core/config/Enchantments.java:57-66 defaults; the wear intervals and repair ticks are settings (Settings "jewelry").
    public static final double JUMP_FACTOR = 0.15;
    public static final double SPEED_FACTOR = 0.05;
    public static final int STEP_UP_TICKS_PER_WEAR = 900;
    public static final int SPIDER_TICKS_PER_WEAR = 1200;
    public static final int FLIGHT_TICKS_PER_WEAR = 300;

    private JewelryEffects() {}

    /** EnchantmentFallDamage.activate: the fall damage left (distance in blocks) and the wear, or a cancelled fall. */
    public record Fall(boolean cancel, float distance, int wear) {}

    public static Fall fall(float distance, int strength) {
        if (strength <= 0) return new Fall(false, distance, 0);
        int damage = (int) (distance - 3);
        int absorbed = strength * 10;
        if (damage < absorbed) return new Fall(true, 0, damage > 0 ? damage / 5 : 0);
        float left = distance - absorbed;
        return new Fall(false, left, damage > 0 ? (int) (left / 5) : 0);
    }

    /** EnchantmentHealth.activate ("1 Up"): a hit that would kill instead sets health to 5 per level; wear = level. */
    public static float oneUpHealth(float health, float amount, int strength) {
        return strength > 0 && health - amount <= 0 ? strength * 5 : -1;
    }

    /** EnchantmentNeverHungry.activate: below full food, each tick adds 1 food and 0.05 saturation per level, wear 1. */
    public static float hungrySaturation(int foodLevel, int strength) {
        return strength > 0 && foodLevel < 20 ? 0.05f * strength : -1;
    }

    /**
     * EnchantmentRestore.activate: every 100 ticks the held item is repaired one point; from level 2 the hotbar too;
     * from level 3 each armour piece (level - 1) points. Each repaired point wears 1.
     */
    public record Repair(boolean held, boolean hotbar, int armourPoints) {}

    public static Repair repair(long tick, int strength) {
        if (strength <= 0 || tick % mariculture.domain.config.Settings.REPAIR_TICKS.get() != 0) return new Repair(false, false, 0);
        return new Repair(true, strength > 1, strength > 2 ? strength - 1 : 0);
    }

    /** EnchantmentResurrection / ResurrectionTracker: what survives a death, and the wear it costs on respawn. */
    public record Resurrection(boolean keepInventory, boolean respawnAtDeath, int invulnerableTicks, int wear) {}

    public static Resurrection resurrection(int strength, boolean fireOrUnblockable) {
        if (strength <= 0) return new Resurrection(false, false, 0, 0);
        boolean atDeath = strength > 1 && !fireOrUnblockable;
        int resist = strength > 2 ? 50 * strength : 0;
        return new Resurrection(true, atDeath, resist, 1 + (atDeath ? 2 : 0) + resist / 50);
    }

    /** EnchantmentSpeed: extra movement per level while sprinting on the ground (legacy moveFlying factor). */
    public static double speedBonus(int strength) {
        return strength > 0 ? SPEED_FACTOR * strength : 0;
    }

    /** EnchantmentJump: extra jump velocity per level. */
    public static double jumpBonus(int strength) {
        return strength > 0 ? JUMP_FACTOR * strength : 0;
    }

    /** EnchantmentStepUp: step height 0.5 * (level + 1); the bonus over the normal 0.5 (vanilla 0.6). */
    public static double stepHeight(int strength) {
        return strength > 0 ? 0.5 * (strength + 1) : 0;
    }

    /** EnchantmentGlide: falling speed is divided by 1.6 while gliding (not sneaking, not in water or on a ladder). */
    public static double glide(double motionY, int strength, boolean sneaking, boolean inWaterOrClimbing) {
        if (strength <= 0 || motionY >= 0 || sneaking || inWaterOrClimbing) return motionY;
        return motionY / 1.6;
    }

    /** EnchantmentSpider: against a wall, horizontal speed is capped at 0.15 and the player climbs at 0.2. */
    public static final double SPIDER_CAP = 0.15, SPIDER_CLIMB = 0.2;

    /** EnchantmentFlight: flight is allowed from level 2; fly speed (mode + 1) * 0.025 with mode 0. */
    public static boolean canFly(int strength) {
        return strength - 1 > 0;
    }

    public static final float FLIGHT_SPEED = 0.025f;

    /** EnchantmentOneRing.activate: what taking the One Ring off costs, by difficulty id (1 easy .. 3 hard). */
    public record RingWithdrawal(int hungerTicks, int witherTicks, int weaknessTicks, int food, int saturation, int damage) {}

    public static RingWithdrawal oneRingWithdrawal(int difficulty) {
        if (difficulty <= 0) return null;
        return new RingWithdrawal(600 * difficulty, 66 * difficulty, 200 * difficulty, -7 * difficulty, -14 * difficulty, 5 * difficulty);
    }
}
