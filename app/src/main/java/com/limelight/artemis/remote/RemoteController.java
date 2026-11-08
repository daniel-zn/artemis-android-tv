package com.limelight.artemis.remote;

/**
 * Turns the remote's keys into the PC's mouse, scrolling and keys, by mode (docs/ARTEMIS-TV.md, "During a
 * session"). Plain Java: the caller passes key events with their times and calls tick() while needsTicks().
 */
public final class RemoteController {
    public enum Mode { POINTER, SCROLL, ARROWS }
    public enum Key { UP, DOWN, LEFT, RIGHT, OK, BACK }

    /** A real hold of OK this long is a right click. */
    public static final long LONG_PRESS_MS = 500;
    /** Windows virtual keys. */
    static final short VK_RETURN = 0x0D, VK_LEFT = 0x25, VK_UP = 0x26, VK_RIGHT = 0x27, VK_DOWN = 0x28;
    /** One scroll notch, in high-resolution units. */
    static final int NOTCH = 120;
    /**
     * Edge scrolling while gliding: high-resolution units per screen width pushed past the edge. At the glide's
     * starting speed (a quarter of the width per second) that's 5 notches a second; at top speed about 22.
     */
    static final double GLIDE_SCROLL_PER_WIDTH = 2400;

    private final RemoteOutput out;
    private final PointerMotion motion = new PointerMotion();
    private int w, h;
    private Mode mode = Mode.POINTER;
    private boolean captured;
    private boolean holdingLeft;

    // The pointer, in the stream's pixels; placed in the middle on the first move.
    private double px, py;
    private boolean placed, sentOnce;
    // Fractions not yet sent (relative moves, scrolling).
    private double relRestX, relRestY, scrollRest, hscrollRest;

    private long okDownAt = Long.MIN_VALUE;
    private boolean okDown, okConsumed;

    public RemoteController(RemoteOutput out, int streamWidth, int streamHeight) {
        this.out = out;
        setStreamSize(streamWidth, streamHeight);
    }

    public void setStreamSize(int w, int h) {
        this.w = Math.max(1, w);
        this.h = Math.max(1, h);
        placed = false;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode m) {
        motion.reset();
        mode = m;
    }

    /** Apollo's cursor state: a game holds the cursor, so the D-pad moves the mouse relatively. */
    public void setCursorCaptured(boolean captured) {
        if (this.captured != captured) {
            motion.reset();
            this.captured = captured;
            // The PC's pointer may have moved meanwhile: start again from the middle.
            placed = false;
        }
    }

    public boolean isCursorCaptured() {
        return captured;
    }

    /** Hold left button: presses it now; the next OK lets go. */
    public void setHoldLeft(boolean hold) {
        if (hold == holdingLeft) {
            return;
        }
        holdingLeft = hold;
        out.button(hold, RemoteOutput.BUTTON_LEFT);
    }

    public boolean isHoldingLeft() {
        return holdingLeft;
    }

    private static Direction direction(Key k) {
        switch (k) {
            case UP: return Direction.UP;
            case DOWN: return Direction.DOWN;
            case LEFT: return Direction.LEFT;
            case RIGHT: return Direction.RIGHT;
            default: return null;
        }
    }

    private static short arrowKey(Direction d) {
        switch (d) {
            case UP: return VK_UP;
            case DOWN: return VK_DOWN;
            case LEFT: return VK_LEFT;
            default: return VK_RIGHT;
        }
    }

    /**
     * A key-down. repeatCount and longPress are the KeyEvent's (an injected long press arrives as a repeat flagged
     * long-press). Returns true when it's the remote's (always, for these keys).
     */
    public boolean keyDown(Key k, long t, int repeatCount, boolean longPress) {
        if (k == Key.BACK) {
            return true;
        }
        if (k == Key.OK) {
            if (repeatCount == 0) {
                okDown = true;
                okDownAt = t;
                okConsumed = false;
            } else if (okDown && !okConsumed && (longPress || t - okDownAt >= LONG_PRESS_MS)) {
                okConsumed = true;
                out.button(true, RemoteOutput.BUTTON_RIGHT);
                out.button(false, RemoteOutput.BUTTON_RIGHT);
            }
            return true;
        }
        Direction d = direction(k);
        switch (mode) {
            case ARROWS:
                out.key(arrowKey(d), true);
                break;
            case SCROLL:
                if (d.vertical()) {
                    out.scroll(-d.dy * NOTCH);
                } else {
                    out.hscroll(d.dx * NOTCH);
                }
                break;
            default:
                motion.press(d, t, repeatCount > 0);
                tick(t);
                break;
        }
        return true;
    }

    public boolean keyUp(Key k, long t, boolean canceled) {
        if (k == Key.BACK) {
            if (!canceled) {
                motion.reset();
                out.openMenu();
            }
            return true;
        }
        if (k == Key.OK) {
            boolean click = okDown && !okConsumed && !canceled;
            okDown = false;
            if (click) {
                if (holdingLeft) {
                    setHoldLeft(false);
                } else if (mode == Mode.ARROWS) {
                    out.key(VK_RETURN, true);
                    out.key(VK_RETURN, false);
                } else {
                    out.button(true, RemoteOutput.BUTTON_LEFT);
                    out.button(false, RemoteOutput.BUTTON_LEFT);
                }
            }
            return true;
        }
        Direction d = direction(k);
        if (mode == Mode.ARROWS) {
            out.key(arrowKey(d), false);
        } else if (mode == Mode.POINTER) {
            motion.release(d, t);
        }
        return true;
    }

    public boolean needsTicks(long t) {
        return motion.isMoving(t);
    }

    /** Sends the pointer's movement up to time t. */
    public void tick(long t) {
        double[] m = motion.take(t);
        double tapX = m[0] * w, tapY = m[1] * w, glideX = m[2] * w, glideY = m[3] * w;
        if (tapX == 0 && tapY == 0 && glideX == 0 && glideY == 0) {
            return;
        }
        if (captured) {
            relRestX += tapX + glideX;
            relRestY += tapY + glideY;
            int dx = (int) relRestX, dy = (int) relRestY;
            relRestX -= dx;
            relRestY -= dy;
            if (dx != 0 || dy != 0) {
                out.moveBy(dx, dy);
            }
            return;
        }
        if (!placed) {
            px = w / 2.0;
            py = h / 2.0;
            placed = true;
        }
        double tapStep = PointerMotion.TAP_STEP * w;
        // Horizontal
        double nx = px + tapX + glideX;
        if (nx < 0 || nx > w - 1) {
            double edge = nx < 0 ? 0 : w - 1;
            int sign = nx < 0 ? -1 : 1;
            // Only what was pushed past the edge scrolls; a tap that was already at the edge is one notch.
            double over = Math.abs(nx - edge);
            double glideOver = Math.min(over, Math.abs(glideX));
            double tapOver = over - glideOver;
            hscrollRest += sign * (tapOver / tapStep * NOTCH + glideOver / w * GLIDE_SCROLL_PER_WIDTH);
            nx = edge;
        }
        double ny = py + tapY + glideY;
        if (ny < 0 || ny > h - 1) {
            double edge = ny < 0 ? 0 : h - 1;
            int sign = ny < 0 ? 1 : -1; // pushing up scrolls up (+)
            double over = Math.abs(ny - edge);
            double glideOver = Math.min(over, Math.abs(glideY));
            double tapOver = over - glideOver;
            scrollRest += sign * (tapOver / tapStep * NOTCH + glideOver / w * GLIDE_SCROLL_PER_WIDTH);
            ny = edge;
        }
        int oldX = (int) Math.round(px), oldY = (int) Math.round(py);
        px = nx;
        py = ny;
        int x = (int) Math.round(px), y = (int) Math.round(py);
        if (x != oldX || y != oldY || !sentOnce) {
            sentOnce = true;
            out.moveTo(x, y, w, h);
        }
        int s = whole(scrollRest);
        if (s != 0) {
            scrollRest -= s;
            out.scroll(s);
        }
        int hs = whole(hscrollRest);
        if (hs != 0) {
            hscrollRest -= hs;
            out.hscroll(hs);
        }
    }

    /** The whole part, forgiving rounding error (a tap's 119.99999 is a notch). */
    private static int whole(double v) {
        return (int) (v + Math.signum(v) * 1e-6);
    }

    /** Lets go of everything (the session ends or pauses). */
    public void reset() {
        motion.reset();
        setHoldLeft(false);
        okDown = false;
    }
}
