package mariculture.domain.maritech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.config.Settings;

class SpeedBoatTest {
    @AfterEach
    void reset() {
        Settings.reset();
    }

    private static SpeedBoat.Input ride(double water, float yaw, float forward) {
        return new SpeedBoat.Input(water, true, yaw, 0, forward, false);
    }

    @Test
    void floatsUpWhenHalfSunkAndSinksWhenOut() {
        assertEquals(0.04 * -1, SpeedBoat.drive(SpeedBoat.State.start(), new SpeedBoat.Input(0, false, 0, 0, 0, false)).y(), 1e-6);
        assertEquals(0, SpeedBoat.drive(SpeedBoat.State.start(), new SpeedBoat.Input(0.5, false, 0, 0, 0, false)).y(), 1e-9);
        assertEquals(0.04 * 0.6, SpeedBoat.drive(SpeedBoat.State.start(), new SpeedBoat.Input(0.8, false, 0, 0, 0, false)).y(), 1e-6);
    }

    @Test
    void fullySunkRisesAtTheConfiguredRate() {
        SpeedBoat.State down = new SpeedBoat.State(0, -0.2, 0, 1);
        assertEquals(-0.1 + 0.014, SpeedBoat.drive(down, new SpeedBoat.Input(1, false, 0, 0, 0, false)).y(), 1e-6, "falling halves, then x2");
        Settings.SPEEDBOAT_VERTICAL_PERCENT.set(100);
        assertEquals(0.007, SpeedBoat.drive(SpeedBoat.State.start(), new SpeedBoat.Input(1, false, 0, 0, 0, false)).y(), 1e-6);
    }

    @Test
    void forwardFollowsTheRidersLookAndStrafeTurnsIt() {
        SpeedBoat.State s = SpeedBoat.drive(SpeedBoat.State.start(), ride(0.5, 0, 1));
        assertEquals(0.3 * 0.05, s.z(), 1e-6, "yaw 0 is +z");
        assertEquals(0, s.x(), 1e-9);
        SpeedBoat.State left = SpeedBoat.drive(SpeedBoat.State.start(), new SpeedBoat.Input(0.5, true, 0, 1, 1, false));
        assertEquals(0.3 * 0.05, left.x(), 1e-6, "strafe 1 swings 90 degrees");
    }

    @Test
    void speedIsCappedAndTheMultiplierClimbsToTen() {
        SpeedBoat.State s = SpeedBoat.State.start();
        for (int i = 0; i < 400; i++) s = SpeedBoat.coast(SpeedBoat.drive(s, ride(0.5, 0, 1)), true);
        assertTrue(s.horizontal() <= SpeedBoat.MAX_SPEED);
        assertEquals(SpeedBoat.MAX_SPEED * 0.99, s.horizontal(), 1e-6);
        assertTrue(s.multiplier() > 10, "past the cap it drifts towards 15 (legacy)");
        assertTrue(s.multiplier() < 15);
    }

    @Test
    void emptyBoatStopsAndGroundHalvesMotion() {
        SpeedBoat.State moving = new SpeedBoat.State(0.2, 0.1, 0.2, 1);
        SpeedBoat.State stopped = SpeedBoat.coast(moving, false);
        assertEquals(0, stopped.horizontal(), 1e-9);
        assertEquals(1, stopped.multiplier(), 1e-9);
        SpeedBoat.State ground = SpeedBoat.drive(new SpeedBoat.State(0.2, 0, 0, 1), new SpeedBoat.Input(0.5, false, 0, 0, 0, true));
        assertEquals(0.1, ground.x(), 1e-9);
    }

    @Test
    void turnsTowardsWhereItCameFromTwentyDegreesATick() {
        assertEquals(0, SpeedBoat.turn(0, 0, 0, -1, 0), 1e-4, "moving -x it came from +x, yaw 0");
        assertEquals(-20, SpeedBoat.turn(0, 0, 0, 1, 0), 1e-4, "moving +x wants 180, at most 20 a tick");
        assertEquals(-20, SpeedBoat.turn(0, 0, 0, 0, 1), 1e-4, "moving +z wants -90");
        assertEquals(180, SpeedBoat.turn(170, 0, 0, 1, 0), 1e-4);
        assertEquals(30, SpeedBoat.turn(30, 0, 0, 0.01, 0.01), 1e-4, "too small a move keeps the heading");
    }

    @Test
    void riderSitsTowardsTheMotorAndTheMotorSwings() {
        double[] o = SpeedBoat.riderOffset(0);
        assertEquals(0.4, o[0], 1e-9);
        assertEquals(0, o[1], 1e-9);
        SpeedBoat.Motor m = new SpeedBoat.Motor();
        assertEquals(0, m.tick(false), 1e-9);
        float max = 0, min = 0;
        for (int i = 0; i < 20; i++) {
            float p = m.tick(true);
            max = Math.max(max, p);
            min = Math.min(min, p);
        }
        assertEquals(0.2f, max, 1e-5);
        assertEquals(-0.2f, min, 1e-5);
    }
}
