package com.limelight.artemis;

import static org.junit.Assert.*;

import org.junit.Test;

public class WakeFlowTest {
    @Test
    public void opensWhenThePolledPcComesUp() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 0);
        assertTrue(w.isWaking());
        assertEquals("pc", w.waking());
        assertEquals(WakeFlow.Result.NONE, w.onPoll("pc", false, 5_000));
        assertEquals(WakeFlow.Result.OPEN, w.onPoll("pc", true, 20_000));
        assertFalse(w.isWaking());
        // Opened once only
        assertEquals(WakeFlow.Result.NONE, w.onPoll("pc", true, 21_000));
    }

    @Test
    public void otherPcsDontCount() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 0);
        assertEquals(WakeFlow.Result.NONE, w.onPoll("other", true, 1_000));
        assertTrue(w.isWaking());
    }

    @Test
    public void givesUpAfterNinetySeconds() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 1_000);
        assertEquals(WakeFlow.Result.NONE, w.tick(90_999));
        assertEquals(WakeFlow.Result.GAVE_UP, w.tick(91_000));
        assertFalse(w.isWaking());
        assertEquals(WakeFlow.Result.NONE, w.tick(95_000));
    }

    @Test
    public void anOfflinePollPastTheTimeoutGivesUp() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 0);
        assertEquals(WakeFlow.Result.GAVE_UP, w.onPoll("pc", false, 90_000));
    }

    @Test
    public void cancelStopsWaiting() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 0);
        w.cancel();
        assertFalse(w.isWaking());
        assertEquals(WakeFlow.Result.NONE, w.onPoll("pc", true, 1_000));
    }

    @Test
    public void wakingAgainRestartsTheWait() {
        WakeFlow w = new WakeFlow();
        w.start("pc", 0);
        w.start("pc", 60_000);
        assertEquals(WakeFlow.Result.NONE, w.tick(100_000));
        assertEquals(WakeFlow.Result.GAVE_UP, w.tick(150_000));
    }
}
