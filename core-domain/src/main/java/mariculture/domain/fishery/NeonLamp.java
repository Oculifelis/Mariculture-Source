package mariculture.domain.fishery;

/**
 * Neon lamps (fishery/blocks/BlockNeonLamp.java:51-73): lit unless powered. A lit lamp that receives power goes out
 * at once; an unlit lamp that loses power lights again after a 4-tick scheduled update, checked once more then.
 * Lit lamps give full light (setLightLevel(1.0F)). One block per colour with a "lit" state replaces the two legacy
 * blocks lamps_on / lamps_off.
 */
public final class NeonLamp {
    public static final int RELIGHT_DELAY = 4;
    public static final int LIGHT = 15;

    /** What a neighbour change or placement does. */
    public enum Action { NONE, TURN_OFF, SCHEDULE_ON }

    private NeonLamp() {}

    public static Action onChange(boolean lit, boolean powered) {
        if (lit && powered) return Action.TURN_OFF;
        if (!lit && !powered) return Action.SCHEDULE_ON;
        return Action.NONE;
    }

    /** At the scheduled tick: light if still unpowered. */
    public static boolean litAfterTick(boolean lit, boolean powered) {
        return lit || !powered;
    }
}
