package mariculture.domain.diving;

import java.util.LinkedHashMap;
import java.util.Map;

import mariculture.domain.machine.Stack;

/**
 * Scuba gear (maritech/extensions/modules/ExtensionDiving.java, items/ItemArmorScuba.java, handlers/ScubaMask.java,
 * ScubaTank.java, ScubaFin.java, ScubaEvent.java, 1.3.0). Armour material SCUBA: durability factor 15, armour
 * 0/0/1/0, enchantability 10; the mask, suit and fins are repaired with neoprene, the tank not at all.
 *
 * <p>The mask works every five ticks (onArmorTick). With the tank on the chest and the head in water, a tank with air
 * left gives full breath, and every 150 game ticks it loses one air. A mask with its land lights on (sneak and use
 * it) wears by one every 150 of its five-tick checks out of water and lava. Under water, or in lava with Fire
 * Protection on the piece, the mask clears the fog and lights the view; the suit breaks blocks four times as fast
 * (oysters 128 times) and the fins five times as fast off the ground. A player wearing the mask and a tank with Fire
 * Protection loses one air instead of taking fire or lava damage and gets six seconds of Fire Resistance.
 *
 * <p>Changes: the tank keeps its air in item data (legacy used damage, and crafted the tank holding one air): a new
 * tank is empty, the air compressor fills it to 240 (legacy stopped one short, BUG-066). The mask lit the view by
 * setting the client's gamma to 10; here it gives a hidden Night Vision effect. The fins gave flight in water at
 * fly speed 0.025 (0.05 with the suit) and cut walking on land to a quarter; here they raise the swim speed
 * attribute by 100% (200% with the suit) and slow walking on land by 75%, which the server understands.
 */
public final class Scuba {
    public static final String MASK = "scuba_mask", TANK = "scuba_tank", SUIT = "scuba_suit", FINS = "swimfin";
    /** Chest durability 16 * factor 15: the legacy tank's air. */
    public static final int TANK_AIR = 240;
    public static final String AIR = "Air", LAND_LIGHTS = "ScubaMaskOnOutOfWater";
    public static final int EVERY_TICKS = 5, AIR_EVERY = 150, LAND_LIGHT_WEAR_EVERY = 150 * EVERY_TICKS;
    public static final float SUIT_BREAK = 4, SUIT_OYSTER_BREAK = 128, FINS_BREAK = 5;
    public static final double FINS_SWIM = 1.0, FINS_SUIT_SWIM = 2.0, FINS_LAND = -0.75;
    /** PotionEffect(fireResistance, 120) on a fire hit. */
    public static final int FIRE_RESISTANCE_TICKS = 120;
    /** Night vision is refreshed on each five-tick check; it lasts long enough not to flicker. */
    public static final int NIGHT_VISION_TICKS = 300;

    /**
     * Thaumaturge warded gear: an infusion marks a mask, suit or swimfin with this custom_data flag. All three marked
     * pieces worn together keep the diver's breath full underwater and add this much swim speed (on top of the fins).
     */
    public static final String WARDED = "mariculture_warded";
    public static final double WARDED_SWIM = 0.5;

    /** Whether the worn mask, suit and fins are all warded. */
    public static boolean wardedSet(boolean mask, boolean suit, boolean fins) {
        return mask && suit && fins;
    }

    private Scuba() {}

    public static int air(Stack tank) {
        return Math.max(0, Math.min(TANK_AIR, tank.intData(AIR, 0)));
    }

    public static Stack withAir(Stack tank, int air) {
        Map<String, Object> data = new LinkedHashMap<>(tank.data());
        data.put(AIR, Math.max(0, Math.min(TANK_AIR, air)));
        return tank.withData(data);
    }

    public static boolean landLights(Stack mask) {
        return mask.data().get(LAND_LIGHTS) instanceof Boolean b ? b : mask.intData(LAND_LIGHTS, 0) != 0;
    }

    /** Sneak-use of the mask flips its land lights. */
    public static Stack toggleLandLights(Stack mask) {
        Map<String, Object> data = new LinkedHashMap<>(mask.data());
        data.put(LAND_LIGHTS, !landLights(mask));
        return mask.withData(data);
    }

    /** What one five-tick mask check does. */
    public record Check(boolean fullBreath, Stack tank, int maskWear) {}

    /**
     * ScubaMask.activate: called when {@code gameTime % 5 == 0} with the mask on. {@code tank} is the chest piece (empty
     * or another item when not a tank).
     */
    public static Check check(long gameTime, Stack mask, Stack tank, boolean headInWater, boolean headInLava) {
        boolean breath = false;
        Stack after = tank;
        if (tank.is("mariculture:" + TANK) && headInWater) {
            int air = air(tank);
            if (air > 0) {
                breath = true;
                if (gameTime % AIR_EVERY == 0) after = withAir(tank, air - 1);
            }
        }
        int wear = landLights(mask) && !headInWater && !headInLava && gameTime % LAND_LIGHT_WEAR_EVERY == 0 ? 1 : 0;
        return new Check(breath, after, wear);
    }

    /** ScubaEvent.canActivate: water, or lava with Fire Protection on the piece. */
    public static boolean active(boolean inWater, boolean inLava, boolean fireProtection) {
        return inWater || inLava && fireProtection;
    }

    /** The mask lights the view (ScubaMask.init): active, or land lights on. */
    public static boolean lights(boolean active, Stack mask) {
        return active || landLights(mask);
    }

    /** ScubaEvent.onBreaking. */
    public static float breakSpeed(float original, boolean suit, boolean fins, boolean active, boolean canHarvest, boolean oyster, boolean onGround) {
        if (!canHarvest || !active) return original;
        float speed = original;
        if (suit) speed = original * (oyster ? SUIT_OYSTER_BREAK : SUIT_BREAK);
        if (fins && !onGround) speed *= FINS_BREAK;
        return speed;
    }

    /**
     * ScubaEvent.onLivingHurt: fire or lava damage with the mask on and a Fire Protection tank holding air is cancelled;
     * returns the tank afterwards, or null when the damage goes through.
     */
    public static Stack fireHit(boolean fireDamage, boolean mask, Stack tank, boolean tankFireProtection) {
        if (!fireDamage || !mask || !tankFireProtection || !tank.is("mariculture:" + TANK)) return null;
        int air = air(tank);
        if (air <= 0) return null;
        return withAir(tank, air - 1);
    }

    /** The fins' swim speed bonus (multiplied with the base), or 0 out of water. */
    public static double finsSwim(boolean active, boolean suit) {
        return active ? suit ? FINS_SUIT_SWIM : FINS_SWIM : 0;
    }

    /** ScubaFin.deactivate: walking on land with the fins is slowed; in water or flying it is not. */
    public static double finsLand(boolean inWater, boolean flying) {
        return inWater || flying ? 0 : FINS_LAND;
    }

    /** The air bar, 0 to 13 pixels. */
    public static int bar(Stack tank) {
        return (int) Math.round(13.0 * air(tank) / TANK_AIR);
    }
}
