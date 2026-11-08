package com.limelight.artemis;

/**
 * "Match this TV": the stream's resolution and frame rate from the TV's own display mode, so the PC's virtual
 * display (Apollo makes one per session at the size and rate the launch request asks for) matches the TV, as the
 * iPhone and Mac apps do (docs/ARTEMIS-TV.md, "Stream mode"). Pure, so the unit tests can feed it odd TVs.
 */
public final class TvMode {
    private TvMode() { }

    /** A display mode as the TV reports it (any orientation, any refresh rate). */
    public static final class Mode {
        public final int width, height;
        public final float refreshRate;

        public Mode(int width, int height, float refreshRate) {
            this.width = width;
            this.height = height;
            this.refreshRate = refreshRate;
        }
    }

    /** What the stream asks for. */
    public static final class Choice {
        public final int width, height, fps;

        public Choice(int width, int height, int fps) {
            this.width = width;
            this.height = height;
            this.fps = fps;
        }

        @Override
        public String toString() {
            return width + "x" + height + "x" + fps;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Choice && ((Choice) o).width == width && ((Choice) o).height == height
                    && ((Choice) o).fps == fps;
        }

        @Override
        public int hashCode() {
            return (width * 31 + height) * 31 + fps;
        }
    }

    /** The sizes a stream snaps to, largest first: 4K is the most (Moonlight's own limit for TVs). */
    static final int[][] STANDARD_SIZES = {{3840, 2160}, {2560, 1440}, {1920, 1080}, {1280, 720}};
    /** Refresh rates a reading snaps to when it's within 2% (59.94 is 60, 23.976 is 24), as the Mac app does. */
    static final int[] STANDARD_RATES = {24, 25, 30, 48, 50, 60, 72, 75, 90, 100, 120};
    static final int MAX_FPS = 120, FALLBACK_FPS = 60;

    /**
     * The mode for the stream.
     * @param current the display's active mode, null if unknown
     * @param supported all the display's modes (may be empty)
     * @param decoderMax the largest {width, height} a hardware decoder takes, null if unknown (some decoders report
     *                   nonsense, so a decoder that doesn't even take 720p counts as unknown)
     */
    public static Choice pick(Mode current, Mode[] supported, int[] decoderMax) {
        Mode base = valid(current) ? current : largest(supported);
        int w = base != null ? Math.max(base.width, base.height) : 1920;
        int h = base != null ? Math.min(base.width, base.height) : 1080;
        int[] size = snapSize(w, h, decoderMax);
        return new Choice(size[0], size[1], pickFps(current, supported, base));
    }

    private static boolean valid(Mode m) {
        if (m == null) {
            return false;
        }
        int w = Math.max(m.width, m.height), h = Math.min(m.width, m.height);
        return w >= 640 && h >= 360 && w <= 16384 && h <= 16384;
    }

    private static Mode largest(Mode[] modes) {
        Mode best = null;
        if (modes != null) {
            for (Mode m : modes) {
                if (valid(m) && (best == null || (long) m.width * m.height > (long) best.width * best.height)) {
                    best = m;
                }
            }
        }
        return best;
    }

    /** The largest standard size that fits in the TV's (an odd 1920x1088 or 1366x768 snaps down) and the decoder. */
    static int[] snapSize(int w, int h, int[] decoderMax) {
        for (int[] s : STANDARD_SIZES) {
            boolean fitsTv = s[0] <= w && s[1] <= h;
            boolean fitsDecoder = decoderMax == null || (s[0] <= decoderMax[0] && s[1] <= decoderMax[1]);
            if (fitsTv && fitsDecoder) {
                return s.clone();
            }
        }
        return STANDARD_SIZES[STANDARD_SIZES.length - 1].clone();
    }

    /**
     * The TV's current refresh rate, snapped. Below 48 Hz (a TV that matched a film's 24 or 30 Hz) the best rate up
     * to 60 at the same size is used instead, else 60; above 120, 120.
     */
    static int pickFps(Mode current, Mode[] supported, Mode base) {
        int fps = current != null ? snapRate(current.refreshRate) : 0;
        if (fps >= 48) {
            return Math.min(fps, MAX_FPS);
        }
        int best = 0;
        if (supported != null && base != null) {
            for (Mode m : supported) {
                int r = m != null ? snapRate(m.refreshRate) : 0;
                if (m != null && sameSize(m, base) && r >= 48 && r <= 60 && r > best) {
                    best = r;
                }
            }
        }
        return best != 0 ? best : FALLBACK_FPS;
    }

    private static boolean sameSize(Mode a, Mode b) {
        return Math.max(a.width, a.height) == Math.max(b.width, b.height)
                && Math.min(a.width, a.height) == Math.min(b.width, b.height);
    }

    /** 0 for a reading that isn't a refresh rate. */
    static int snapRate(float hz) {
        if (!(hz > 1) || Float.isInfinite(hz) || hz > 1000) {
            return 0;
        }
        for (int r : STANDARD_RATES) {
            if (Math.abs(hz - r) <= r * 0.02f) {
                return r;
            }
        }
        return Math.round(hz);
    }

    // --- Moving installs off upstream's default ---

    /** The values the settings store for "Match this TV". */
    public static final String MATCH = "auto";
    /** Upstream's defaults, which every install got written at its first start. */
    public static final String OLD_DEFAULT_RESOLUTION = "1280x720", OLD_DEFAULT_FPS = "60";

    /** A resolution still on upstream's default (or never set) moves to Match this TV, once. */
    public static String migrateResolution(String stored) {
        return stored == null || stored.equals(OLD_DEFAULT_RESOLUTION) ? MATCH : stored;
    }

    /** Likewise the frame rate. */
    public static String migrateFps(String stored) {
        return stored == null || stored.equals(OLD_DEFAULT_FPS) ? MATCH : stored;
    }
}
