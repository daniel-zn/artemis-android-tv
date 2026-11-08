package com.limelight.artemis;

/**
 * "Waking up <PC>…": after Wake-on-LAN is sent, waits for the PC's status polls to see it online, then opens it;
 * gives up after TIMEOUT_MS ("Couldn't wake"). Plain Java: times are milliseconds from any clock. Only the status
 * polls decide whether a PC is online; a failed open never marks it offline.
 */
public final class WakeFlow {
    public static final long TIMEOUT_MS = 90_000;

    public enum Result { NONE, OPEN, GAVE_UP }

    private String uuid;
    private long startedAt;

    /** Wake-on-LAN went out for this PC (a second wake restarts the wait). */
    public void start(String uuid, long t) {
        this.uuid = uuid;
        this.startedAt = t;
    }

    public void cancel() {
        uuid = null;
    }

    public boolean isWaking() {
        return uuid != null;
    }

    public String waking() {
        return uuid;
    }

    /** A status poll's result for a PC. OPEN: it's the PC being woken, and it's up: open it now. */
    public Result onPoll(String pcUuid, boolean online, long t) {
        if (uuid == null || !uuid.equals(pcUuid)) {
            return Result.NONE;
        }
        if (online) {
            uuid = null;
            return Result.OPEN;
        }
        return tick(t);
    }

    /** GAVE_UP once the PC hasn't come up within TIMEOUT_MS. */
    public Result tick(long t) {
        if (uuid != null && t - startedAt >= TIMEOUT_MS) {
            uuid = null;
            return Result.GAVE_UP;
        }
        return Result.NONE;
    }
}
