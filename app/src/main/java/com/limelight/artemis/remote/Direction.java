package com.limelight.artemis.remote;

/** A D-pad direction. dx/dy: right and down are positive. */
public enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    public final int dx, dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction opposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            default: return LEFT;
        }
    }

    public boolean vertical() {
        return dy != 0;
    }
}
