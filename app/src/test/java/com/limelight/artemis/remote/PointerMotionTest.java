package com.limelight.artemis.remote;

import static org.junit.Assert.*;

import org.junit.Test;

public class PointerMotionTest {
    private static final double EPS = 1e-9;

    @Test public void aTapMovesOneSmallStepAndStops() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.RIGHT, 1000, false);
        m.release(Direction.RIGHT, 1001);
        double[] d = m.take(1001);
        assertEquals(PointerMotion.TAP_STEP, d[0] + d[2], EPS);
        assertEquals(0, d[1] + d[3], EPS);
        d = m.take(3000);
        assertEquals(0, d[0] + d[2], EPS);
        assertFalse(m.isMoving(3000));
    }

    @Test public void upIsNegativeAndDownPositive() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.UP, 0, false);
        assertEquals(-PointerMotion.TAP_STEP, m.take(1)[1], EPS);
        m.press(Direction.DOWN, 5000, false);
        assertEquals(PointerMotion.TAP_STEP, m.take(5001)[1], EPS);
    }

    @Test public void injectedRepeatsGlideAndSpeedUp() {
        // An injected hold: a press, the first repeat after 400 ms, then one every 100 ms; each a down and up together.
        PointerMotion m = new PointerMotion();
        m.press(Direction.RIGHT, 0, false); m.release(Direction.RIGHT, 1);
        m.take(1);
        double early = 0, late = 0;
        for (long t = 400; t <= 3000; t += 100) {
            m.press(Direction.RIGHT, t, false); m.release(Direction.RIGHT, t + 1);
            double dx = m.take(t + 100)[2];
            if (t >= 400 && t < 700) early += dx;
            if (t >= 2000 && t < 2300) late += dx;
        }
        assertTrue("glides", early > 0);
        // The ramp counts from the chain's first press: 400–700 ms averages 0.25 + 0.85 × 550/1200 ≈ 0.64 widths/s.
        assertEquals(PointerMotion.speedAfter(550) * 0.3, early, 0.005);
        assertTrue("faster later: " + early + " " + late, late > early * 1.5);
        // Top speed: 110 % of the width per second, so 300 ms ≈ 0.33.
        assertEquals(PointerMotion.TOP_SPEED * 0.3, late, 0.01);
    }

    @Test public void anInjectedGlideRunsOutShortlyAfterTheLastPress() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.LEFT, 0, false);
        m.press(Direction.LEFT, 400, false);
        m.take(400);
        assertTrue(m.isMoving(500));
        m.take(400 + PointerMotion.GLIDE_MS);
        assertFalse(m.isMoving(400 + PointerMotion.GLIDE_MS + 1));
        assertEquals(0, m.take(5000)[2], EPS);
    }

    @Test public void aRealHoldStopsOnKeyUp() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.DOWN, 0, false);
        for (long t = 500; t <= 1000; t += 50) m.press(Direction.DOWN, t, true);
        m.take(1010);
        m.release(Direction.DOWN, 1010);
        assertEquals(0, m.take(1200)[3], EPS);
        assertFalse(m.isMoving(1200));
    }

    @Test public void pressesFarApartAreSeparateTaps() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.RIGHT, 0, false);
        m.press(Direction.RIGHT, 1000, false);
        double[] d = m.take(2000);
        assertEquals(2 * PointerMotion.TAP_STEP, d[0], EPS);
        assertEquals(0, d[2], EPS);
    }

    @Test public void theOppositeDirectionStopsAGlide() {
        PointerMotion m = new PointerMotion();
        m.press(Direction.RIGHT, 0, false);
        m.press(Direction.RIGHT, 400, false);
        m.take(450);
        m.press(Direction.LEFT, 450, false);
        double[] d = m.take(600);
        assertEquals(-PointerMotion.TAP_STEP, d[0], EPS);
        assertEquals(0, d[2], EPS);
    }

    @Test public void speedRampsFromStartToTop() {
        assertEquals(PointerMotion.START_SPEED, PointerMotion.speedAfter(0), EPS);
        assertEquals(PointerMotion.TOP_SPEED, PointerMotion.speedAfter(PointerMotion.RAMP_MS), EPS);
        assertEquals(PointerMotion.TOP_SPEED, PointerMotion.speedAfter(10_000), EPS);
    }
}
