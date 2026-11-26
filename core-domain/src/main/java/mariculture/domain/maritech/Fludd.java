package mariculture.domain.maritech;

import java.util.HashMap;
import java.util.Map;

import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * F.L.U.D.D. (maritech/items/ItemFLUDD.java, handlers/FLUDDEvents.java, network/PacketFLUDD.java, 1.3.0): a water pack
 * worn on the chest holding up to 20000 units of pressurised water ("water" in its data). The activate key (V) works it:
 * sneaking switches between hover, rocket, turbo and squirt; holding it uses the mode. Every tick a mode works costs
 * one unit (damageFLUDD), and so does each squirt. It is filled with pressurised water in a pressure vessel's special
 * slot, and used on a block it becomes a FLUDD stand keeping its water.
 *
 * <p>The client moves the player; {@link Pilot} is the per-tick rule (LivingUpdateEvent) the targets apply.
 */
public final class Fludd {
    public static final String ITEM = "mariculture:fludd", WATER = "water", MODE = "mode";
    public static final int STORAGE = 20000;
    /** Hover lasts 300 ticks from leaving the ground; rocket and the hover lift push for the first 10. */
    public static final int HOVER_TICKS = 300, BOOST_TICKS = 10;
    public static final double ROCKET_LIFT = 0.35, HOVER_RISE = 0.05, HOVER_FORWARD = 0.02, HOVER_DROP = -0.05, HOVER_DIVE = -0.15;
    public static final double TURBO_GROUND = 0.5, TURBO_AIR = 0.1, TURBO_WATER = 0.05;
    /** EntityFLUDDSquirt: 10 damage to what it hits. */
    public static final float SQUIRT_DAMAGE = 10f;
    public static final String SOUND = "item.fludd.squirt";

    public enum Mode { HOVER, ROCKET, TURBO, SQUIRT, NONE }

    private Fludd() {}

    public static int water(Stack s) {
        return s.intData(WATER, 0);
    }

    /** ItemFLUDD.getMode: the chosen mode while it holds water, NONE when empty. */
    public static Mode mode(Stack s) {
        if (s.isEmpty() || !s.item().equals(ITEM) || water(s) <= 0) return Mode.NONE;
        int m = s.intData(MODE, 0);
        return Mode.values()[m >= 0 && m < 4 ? m : 0];
    }

    /** The FLUDD with the next mode (sneak + activate key), wrapping after squirt. */
    public static Stack nextMode(Stack s) {
        int m = s.intData(MODE, 0) + 1;
        return with(s, MODE, m > 3 ? 0 : m);
    }

    /** FLUDDEvents.damageFLUDD: one unit of water used in a mode; unchanged when empty. */
    public static Stack use(Stack s, Mode mode) {
        int w = water(s);
        if (w <= 0) return s;
        Map<String, Object> d = new HashMap<>(s.data());
        d.put(WATER, w - 1);
        d.put(MODE, mode.ordinal());
        return s.withData(d);
    }

    /** ItemFLUDD.build: a FLUDD holding {@code water} units in hover mode. */
    public static Stack of(int water) {
        Map<String, Object> d = new HashMap<>();
        d.put(MODE, 0);
        d.put(WATER, Math.max(0, water));
        return Stack.of(ITEM, 1, d);
    }

    /** ItemFLUDD.fill (IPVChargeable): tops the FLUDD up from pressurised water in the tank. */
    public static Stack fill(Stack s, Tank tank) {
        if (!s.item().equals(ITEM) || !tank.contains(Fluids.PRESSURISED_WATER) || tank.amount() <= 0) return s;
        int w = water(s);
        int taken = tank.drain(Fluids.PRESSURISED_WATER, STORAGE - w, false);
        if (taken <= 0) return s;
        return with(s, WATER, Math.min(STORAGE, w + taken));
    }

    /** Remaining water as a 0-13 bar. */
    public static int bar(Stack s) {
        return Math.round(13f * Math.max(0, Math.min(STORAGE, water(s))) / STORAGE);
    }

    private static Stack with(Stack s, String key, int value) {
        Map<String, Object> d = new HashMap<>(s.data());
        d.put(key, value);
        return s.withData(d);
    }

    /** What the player's input does this tick. */
    public record Input(boolean held, boolean onGround, boolean inWater, boolean forward, boolean jump) {}

    /**
     * One tick's effect: {@code addY} added to the vertical motion, then {@code setY} (when not NaN) replacing it;
     * {@code forward} pushed along the look; whether it costs a unit of water; whether the fall is forgiven; and
     * whether hovering stopped this tick (fall distance reset on release).
     */
    public record Step(double addY, double setY, double forward, boolean cost, boolean noFall, boolean landed) {
        static final Step NOTHING = new Step(0, Double.NaN, 0, false, false, false);
    }

    /** The client's hover and rocket timers (FLUDDEvents.hoverTick/jumpTick). */
    public static final class Pilot {
        private int hoverTick, jumpTick;
        private boolean wasHeld;

        public Step tick(Mode mode, Input in) {
            if (!in.held()) {
                if (in.onGround()) hoverTick = 0;
                boolean landed = wasHeld && mode == Mode.HOVER;
                wasHeld = false;
                return landed ? new Step(0, Double.NaN, 0, false, true, true) : Step.NOTHING;
            }
            wasHeld = true;
            double addY = 0, setY = Double.NaN, forward = 0;
            boolean cost = false, noFall = false;
            if (mode == Mode.TURBO) {
                forward = in.inWater() ? TURBO_WATER : in.onGround() ? TURBO_GROUND : TURBO_AIR;
                cost = true;
            }
            if (mode == Mode.ROCKET && jumpTick < BOOST_TICKS) {
                addY = ROCKET_LIFT;
                noFall = true;
                cost = true;
            }
            if (in.onGround()) jumpTick = 0;
            hoverTick++;
            jumpTick++;
            if (mode == Mode.HOVER) {
                if (hoverTick < HOVER_TICKS) {
                    // Only the first ten ticks after leaving the ground use water (legacy).
                    if (jumpTick < BOOST_TICKS) cost = true;
                    noFall = true;
                    setY = in.forward() ? 0 : HOVER_RISE;
                    if (!in.onGround() && in.forward()) forward = HOVER_FORWARD;
                    if (in.jump()) setY = in.forward() ? HOVER_DIVE : HOVER_DROP;
                }
            } else if (in.onGround()) {
                hoverTick = 0;
            }
            return new Step(addY, setY, forward, cost, noFall, false);
        }
    }
}
