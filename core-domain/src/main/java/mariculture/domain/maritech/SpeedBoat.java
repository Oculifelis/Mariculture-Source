package mariculture.domain.maritech;

import mariculture.domain.config.Settings;

/**
 * The MariTech speed boat (maritech/entity/EntitySpeedBoat.java and items/ItemSpeedBoat.java, 1.3.0): a fast motor
 * boat steered like a horse, with its own buoyancy, a top speed of 0.35 blocks a tick and a thirst for lily pads and
 * squid.
 *
 * <p>The edition entity feeds {@link #drive} and {@link #coast} each tick it is in charge of the boat (the rider's
 * client, or the server while empty) and moves itself in between; {@link #turn} gives its new heading. Yaw is the
 * legacy one, the direction from where the boat is to where it was (the motor end), not the usual entity facing.
 *
 * <p>Changes: the legacy hit, any player's, picked the boat up at once; non-player damage did nothing, which is kept.
 * The speed multiplier keeps its legacy drift (EntitySpeedBoat.java:293-305): the slowing branch subtracts
 * {@code (m - 15) / 150}, which raises it towards 15 instead, so the boat only ever gets keener; the 0.35 cap is what
 * really limits it.
 */
public final class SpeedBoat {
    public static final String ID = "speed_boat";
    public static final String ITEM = "mariculture:" + ID;
    /** setSize(1.5, 0.6). */
    public static final float WIDTH = 1.5f, HEIGHT = 0.6f;
    /** speedMultiplier starts at 0.3 (0.15 in 1.2.4). */
    public static final double START_MULTIPLIER = 0.3;
    /** Horizontal speed cap (EntitySpeedBoat.java:286). */
    public static final double MAX_SPEED = 0.35;
    /** Squid within a block of the hull take 25 (EntitySpeedBoat.java:315-320). */
    public static final float SQUID_DAMAGE = 25;
    /** The rider sits 0.4 blocks towards the motor (updateRiderPosition). */
    public static final double RIDER_BACK = 0.4;
    /** The item places a boat on water within 5 blocks of the eye (ItemSpeedBoat.java:41). */
    public static final double PLACE_REACH = 5;
    /** Splashes past this speed (EntitySpeedBoat.java:197). */
    public static final double SPLASH_SPEED = 0.26249999999999996;
    /** Water slices sampled for buoyancy. */
    public static final int SLICES = 5;

    private SpeedBoat() {}

    /** Motion and the speed multiplier. */
    public record State(double x, double y, double z, double multiplier) {
        public static State start() { return new State(0, 0, 0, START_MULTIPLIER); }
        public double horizontal() { return Math.sqrt(x * x + z * z); }
    }

    /**
     * One tick's input: how much of the hull is in water (0 to 1, in fifths), whether a living rider steers it and
     * with what yaw, strafe and forward (-1 to 1), and whether the boat sits on the ground.
     */
    public record Input(double water, boolean ridden, float riderYaw, float strafe, float forward, boolean onGround) {}

    /** The motion to move by this tick (EntitySpeedBoat.java:262-328), with the updated multiplier. */
    public static State drive(State s, Input in) {
        double before = s.horizontal();
        double mx = s.x(), my = s.y(), mz = s.z(), m = s.multiplier();
        if (in.water() < 1) {
            my += 0.03999999910593033 * (in.water() * 2 - 1);
        } else {
            if (my < 0) my /= 2;
            my += 0.007000000216066837 * (in.water() > 0.9 ? Settings.SPEEDBOAT_VERTICAL_PERCENT.get() / 100.0 : 1);
        }
        if (in.ridden()) {
            double yaw = (in.riderYaw() - in.strafe() * 90f) * (float) Math.PI / 180f;
            mx += -Math.sin(yaw) * m * in.forward() * 0.05000000074505806;
            mz += Math.cos(yaw) * m * in.forward() * 0.05000000074505806;
        }
        double speed = Math.sqrt(mx * mx + mz * mz);
        if (speed > MAX_SPEED) {
            double k = MAX_SPEED / speed;
            mx *= k;
            mz *= k;
            speed = MAX_SPEED;
        }
        if (speed > before && m < 10) {
            m = Math.min(10, m + (25 - m) / 150.0);
        } else {
            m = Math.max(0.07, m - (m - 15) / 150.0);
        }
        if (in.onGround()) {
            mx *= 0.5;
            my *= 0.5;
            mz *= 0.5;
        }
        return new State(mx, my, mz, m);
    }

    /** After moving: an empty boat stops dead, a ridden one drifts on (EntitySpeedBoat.java:332-340). */
    public static State coast(State s, boolean ridden) {
        if (!ridden) return new State(0, 0, 0, s.multiplier());
        return new State(s.x() * 0.9900000095367432, s.y() * 0.949999988079071, s.z() * 0.9900000095367432, s.multiplier());
    }

    /**
     * The new yaw after moving from (prevX, prevZ) to (x, z): towards where it came from, at most 20 degrees a tick
     * (EntitySpeedBoat.java:342-362).
     */
    public static float turn(float yaw, double prevX, double prevZ, double x, double z) {
        double dx = prevX - x, dz = prevZ - z;
        double target = yaw;
        if (dx * dx + dz * dz > 0.001) target = (float) (Math.atan2(dz, dx) * 180 / Math.PI);
        double d = wrap(target - yaw);
        return (float) (yaw + Math.max(-20, Math.min(20, d)));
    }

    /** Where the rider sits, as an x/z offset from the boat's centre. */
    public static double[] riderOffset(float yaw) {
        double r = yaw * Math.PI / 180;
        return new double[] {Math.cos(r) * RIDER_BACK, Math.sin(r) * RIDER_BACK};
    }

    /** The motor swings while ridden, 0.1 radians a tick between -0.15 and 0.15 (EntitySpeedBoat.java:223-233). */
    public static final class Motor {
        private float pos;
        private boolean goneRight;

        public float tick(boolean ridden) {
            if (!ridden) return pos;
            if (!goneRight) {
                pos += 0.1f;
                if (pos > 0.15f) goneRight = true;
            } else {
                pos -= 0.1f;
                if (pos < -0.15f) goneRight = false;
            }
            return pos;
        }
    }

    private static double wrap(double deg) {
        deg %= 360;
        if (deg >= 180) deg -= 360;
        if (deg < -180) deg += 360;
        return deg;
    }
}
