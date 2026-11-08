package com.limelight.artemis.remote;

/**
 * How far the remote's D-pad moves the pointer, in fractions of the screen's width (so a step is the same
 * physical distance across and up/down).
 *
 * <p>A press starts a "chain": the first press of a chain moves one small step at once. Presses of the same
 * direction that keep coming (a real remote's key repeats while it's held, an injected remote's repeats, or quick
 * taps) continue the chain: each keeps the pointer gliding for a short while, faster the longer the chain lasts.
 * A key-up after a real hold (Android's key repeat) stops the glide at once; injected keys (each press a
 * key-down and key-up together) have no hold to end, so their glide runs out by itself.
 *
 * <p>Plain Java, no Android: times are milliseconds from any clock.
 */
public final class PointerMotion {
    /** One tap: 1.5 % of the screen's width. */
    public static final double TAP_STEP = 0.015;
    /** Glide speed when a chain starts, and after RAMP_MS, in screen widths per second. */
    public static final double START_SPEED = 0.25;
    public static final double TOP_SPEED = 1.10;
    public static final long RAMP_MS = 1200;
    /** Presses closer than this continue the chain (an injected hold's first repeat comes after 400 ms). */
    public static final long CHAIN_GAP_MS = 450;
    /** How long one continuing press keeps the pointer gliding. */
    public static final long GLIDE_MS = 150;

    private final Axis[] dirs = new Axis[4];

    public PointerMotion() {
        for (int i = 0; i < 4; i++) {
            dirs[i] = new Axis();
        }
    }

    private static final class Axis {
        long chainStart = Long.MIN_VALUE;
        long lastPress = Long.MIN_VALUE;
        long glideUntil = Long.MIN_VALUE;
        long integratedTo = Long.MIN_VALUE;
        boolean realHold;
        double pending; // a tap's step, not yet handed out

        boolean inChain(long t) {
            return lastPress != Long.MIN_VALUE && t - lastPress <= CHAIN_GAP_MS;
        }
    }

    /**
     * A key-down of a direction. repeat: Android's own key repeat (a key held on a real remote); an injected
     * key's repeats come as fresh presses.
     */
    public void press(Direction d, long t, boolean repeat) {
        Axis a = dirs[d.ordinal()];
        // The opposite direction stops at once: no tug of war.
        Axis o = dirs[d.opposite().ordinal()];
        o.glideUntil = Math.min(o.glideUntil, t);
        o.lastPress = Long.MIN_VALUE;

        if (!a.inChain(t) && !repeat) {
            a.chainStart = t;
            a.pending += TAP_STEP;
            a.glideUntil = t;
            a.integratedTo = t;
            a.realHold = false;
        } else {
            if (a.chainStart == Long.MIN_VALUE) {
                a.chainStart = t;
            }
            if (a.glideUntil < t) {
                // The glide restarts from here (a gap between presses isn't distance).
                a.integratedTo = t;
            }
            a.glideUntil = Math.max(a.glideUntil, t + GLIDE_MS);
        }
        a.realHold |= repeat;
        a.lastPress = t;
    }

    /** The key-up of a direction: after a real hold, the glide stops now. */
    public void release(Direction d, long t) {
        Axis a = dirs[d.ordinal()];
        if (a.realHold) {
            a.glideUntil = Math.min(a.glideUntil, t);
            a.lastPress = Long.MIN_VALUE;
            a.realHold = false;
        }
    }

    /** Stops everything (the session menu opened, the mode changed). */
    public void reset() {
        for (int i = 0; i < 4; i++) {
            dirs[i] = new Axis();
        }
    }

    /** True while some direction still moves (so the caller keeps ticking). */
    public boolean isMoving(long t) {
        for (Axis a : dirs) {
            if (a.pending != 0 || a.glideUntil > a.integratedTo) {
                return true;
            }
        }
        return false;
    }

    /** True while a chain of presses goes on in this direction (for edge scrolling's first push). */
    public boolean isGliding(Direction d, long t) {
        Axis a = dirs[d.ordinal()];
        return a.glideUntil > t;
    }

    /** The glide speed after a chain has run for this long, in screen widths per second. */
    public static double speedAfter(long chainMs) {
        double f = Math.max(0, Math.min(1, chainMs / (double) RAMP_MS));
        return START_SPEED + (TOP_SPEED - START_SPEED) * f;
    }

    /**
     * The movement since the last call, up to time t, in screen widths (right and down are positive):
     * {tapX, tapY, glideX, glideY}, taps' steps apart from the glide (edge scrolling treats them differently).
     */
    public double[] take(long t) {
        double[] out = new double[4];
        for (Direction d : Direction.values()) {
            Axis a = dirs[d.ordinal()];
            double tap = a.pending;
            a.pending = 0;
            double glide = 0;
            long end = Math.min(t, a.glideUntil);
            if (end > a.integratedTo) {
                // The speed ramps up linearly over RAMP_MS from the chain's start.
                glide = integrate(a.integratedTo - a.chainStart, end - a.chainStart);
                a.integratedTo = end;
            }
            out[0] += tap * d.dx;
            out[1] += tap * d.dy;
            out[2] += glide * d.dx;
            out[3] += glide * d.dy;
        }
        return out;
    }

    private static double integrate(long fromMs, long toMs) {
        // Piecewise: linear ramp until RAMP_MS, then constant.
        double total = 0;
        if (fromMs < RAMP_MS) {
            long rampEnd = Math.min(toMs, RAMP_MS);
            double v0 = speedAfter(fromMs), v1 = speedAfter(rampEnd);
            total += (v0 + v1) / 2 * (rampEnd - fromMs) / 1000.0;
            fromMs = rampEnd;
        }
        if (toMs > fromMs) {
            total += TOP_SPEED * (toMs - fromMs) / 1000.0;
        }
        return total;
    }
}
