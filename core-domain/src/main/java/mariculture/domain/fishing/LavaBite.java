package mariculture.domain.fishing;

import mariculture.domain.random.Rng;

/**
 * Lava fishing (fishery/EntityHook.java:189-310, FISH-033): in the Nether the hook floats in lava as it does in water
 * ({@code isHellWorld && Material.lava}) and fish bite in the same three steps: a wait until something is lured, a
 * fish swimming in, then a bite that lasts until the player reels in or it escapes. Catches come out as fire-immune
 * items (EntityItemFireImmune). One instance per hook; the editions call {@link #tick} once a server tick while the
 * hook is in lava.
 *
 * <p>The wait is vanilla's (100-600 ticks less the lure ticks of enchantment and bait), as the port's water hook is
 * vanilla's; legacy's own hook waited 100-1500. The bite lasts 20-50 ticks and the swim-in 20-80, as in legacy.
 * Under a roof every other tick may not count ({@code k} drops to 0 half the time), as in both.
 *
 * <p>The same steps drive a Mariculture hook in water while the fishing speed multiplier ({@link FishingSpeed}) is on:
 * {@code speed} more ticks come off the wait every tick (EntityHook.java:254), roof or not.
 */
public final class LavaBite {
    /** HOOKED is the tick the fish bites (splash), BITE the ticks it stays on. */
    public enum Event { LURING, SWIMMING, HOOKED, BITE, ESCAPED }

    private final int lureTicks, speed;
    private int lured, hooked, nibble;
    private float angle;

    public LavaBite(int lureTicks) {
        this(lureTicks, 0);
    }

    public LavaBite(int lureTicks, int speed) {
        this.lureTicks = Math.max(0, lureTicks);
        this.speed = Math.max(0, speed);
    }

    /** One server tick; {@code underRoof} is "cannot see the sky" above the hook. */
    public Event tick(Rng rng, boolean underRoof) {
        int k = underRoof && rng.nextFloat() < 0.5f ? 0 : 1;
        if (nibble > 0) {
            if (--nibble <= 0) {
                lured = 0;
                hooked = 0;
                return Event.ESCAPED;
            }
            return Event.BITE;
        }
        if (hooked > 0) {
            hooked -= k;
            if (hooked <= 0) {
                nibble = 20 + rng.nextInt(31);
                return Event.HOOKED;
            }
            angle += (rng.nextFloat() - 0.5f) * 8f;
            return Event.SWIMMING;
        }
        if (lured > 0) {
            lured -= k + speed;
            if (lured <= 0) {
                angle = rng.nextFloat() * 360f;
                hooked = 20 + rng.nextInt(61);
            }
            return Event.LURING;
        }
        lured = Math.max(1, 100 + rng.nextInt(501) - lureTicks);
        return Event.LURING;
    }

    /** Whether reeling in now catches something. */
    public boolean biting() {
        return nibble > 0;
    }

    /** Ticks until the fish arrives (for the trail it leaves), and its heading in degrees. */
    public int swimTicks() {
        return hooked;
    }

    public float angle() {
        return angle;
    }
}
