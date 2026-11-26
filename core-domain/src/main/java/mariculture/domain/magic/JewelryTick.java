package mariculture.domain.magic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * What worn jewelry does to a player each server tick (magic/MagicEventHandler.onPlayerTick and the
 * Enchantment*.activate methods it calls, 1.3.0), as a plan the target applies: attribute bonuses, hidden effects,
 * flight, feeding, repairs and the wear to spread over the pieces.
 *
 * <p>Changes: legacy kept the wear counters in static fields shared by every player and ran the movement parts on
 * the client only. Here the counters are the world tick (each effect wears once per its interval while in use), the
 * server decides, and the client gets attributes and effects: speed and step-up are attribute bonuses, jump is a
 * hidden jump boost of about the same height (legacy added 0.15 per level, jump boost 0.1 per amplifier level), glide
 * is a hidden slow falling while not sneaking (legacy divided the fall speed by 1.6 with a key toggled on), and spider
 * is a hidden effect the client turns into climbing. Never Hungry does not drain pieces in an open mirror.
 */
public final class JewelryTick {
    /** The player's state this tick. {@code againstWall}: moving into a block sideways (horizontal collision). */
    public record State(long tick, boolean sprinting, boolean onGround, boolean inWater, boolean againstWall, boolean flying,
            boolean creative, int food) {}

    /**
     * The plan: movement speed bonus (multiplied base), step height bonus (added), jump boost amplifier (-1: none),
     * glide, spider amplifier (-1: none), whether the player may fly, a feeding of one food with the saturation, the
     * repair, and the wear per enchantment id.
     */
    public record Plan(double speed, double stepBonus, int jumpAmplifier, boolean glide, int spiderAmplifier, boolean mayFly,
            float feedSaturation, JewelryEffects.Repair repair, Map<String, Integer> wear) {}

    /** Vanilla's normal step height (legacy 0.5). */
    public static final double VANILLA_STEP = 0.6;

    private JewelryTick() {}

    public static Plan plan(State s, ToIntFunction<String> strength) {
        Map<String, Integer> wear = new LinkedHashMap<>();
        int speed = strength.applyAsInt(JewelryEnchantment.SPEED.id());
        double speedBonus = 0;
        if (speed > 0 && s.sprinting() && s.onGround() && !s.inWater()) {
            speedBonus = JewelryEffects.speedBonus(speed);
            if (s.tick() % mariculture.domain.config.Settings.SPEED_TICKS_PER_WEAR.get() == 0) wear.merge(JewelryEnchantment.SPEED.id(), 1, Integer::sum);
        }
        int step = strength.applyAsInt(JewelryEnchantment.STEP_UP.id());
        double stepBonus = step > 0 ? JewelryEffects.stepHeight(step) - VANILLA_STEP : 0;
        if (step > 0 && s.tick() % JewelryEffects.STEP_UP_TICKS_PER_WEAR == 0) wear.merge(JewelryEnchantment.STEP_UP.id(), 1, Integer::sum);
        int jump = strength.applyAsInt(JewelryEnchantment.JUMP.id());
        int jumpAmp = jump > 0 ? jumpAmplifier(jump) : -1;
        boolean glide = strength.applyAsInt(JewelryEnchantment.GLIDE.id()) > 0;
        int spider = strength.applyAsInt(JewelryEnchantment.SPIDER.id());
        if (spider > 0 && s.againstWall() && s.tick() % JewelryEffects.SPIDER_TICKS_PER_WEAR == 0) {
            wear.merge(JewelryEnchantment.SPIDER.id(), 1, Integer::sum);
        }
        int flight = strength.applyAsInt(JewelryEnchantment.FLIGHT.id());
        boolean fly = JewelryEffects.canFly(flight);
        if (fly && s.flying() && !s.creative() && s.tick() % JewelryEffects.FLIGHT_TICKS_PER_WEAR == 0) {
            wear.merge(JewelryEnchantment.FLIGHT.id(), 1, Integer::sum);
        }
        float feed = s.creative() ? -1 : JewelryEffects.hungrySaturation(s.food(), strength.applyAsInt(JewelryEnchantment.HUNGRY.id()));
        if (feed >= 0) wear.merge(JewelryEnchantment.HUNGRY.id(), 1, Integer::sum);
        JewelryEffects.Repair repair = JewelryEffects.repair(s.tick(), strength.applyAsInt(JewelryEnchantment.REPAIR.id()));
        return new Plan(speedBonus, stepBonus, jumpAmp, glide, spider > 0 ? spider - 1 : -1, fly, feed, repair, wear);
    }

    /** Jump boost amplifier for a jump strength: 0.1 per amplifier level against legacy's 0.15 per level. */
    public static int jumpAmplifier(int strength) {
        return Math.max(0, Math.round(1.5f * strength) - 1);
    }

    /** EnchantmentJump: one wear per configured number of jumps (10); {@code jumps} counts this player's. */
    public static boolean jumpWears(int jumps) {
        return jumps > 0 && jumps % mariculture.domain.config.Settings.JUMPS_PER_WEAR.get() == 0;
    }
}
