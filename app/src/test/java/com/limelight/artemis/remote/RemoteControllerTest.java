package com.limelight.artemis.remote;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.limelight.artemis.remote.RemoteController.Key;
import com.limelight.artemis.remote.RemoteController.Mode;

public class RemoteControllerTest {
    private static final int W = 1920, H = 1080;
    private RecordingOutput out;
    private RemoteController rc;
    private long t = 10_000;

    @Before public void setUp() {
        out = new RecordingOutput();
        rc = new RemoteController(out, W, H);
    }

    /** An injected tap: down and up together. */
    private void tap(Key k) {
        rc.keyDown(k, t, 0, false);
        rc.keyUp(k, t + 1, false);
        rc.tick(t + 1);
        t += 1000;
    }

    @Test public void startsInPointerModeAndADpadTapMovesThePointerFromTheCenter() {
        assertEquals(Mode.POINTER, rc.getMode());
        tap(Key.RIGHT);
        assertEquals(W / 2 + Math.round(PointerMotion.TAP_STEP * W), out.x);
        assertEquals(H / 2, out.y);
        tap(Key.DOWN);
        assertEquals(H / 2 + Math.round(PointerMotion.TAP_STEP * W), out.y);
    }

    @Test public void okTapIsALeftClickOnKeyUp() {
        rc.keyDown(Key.OK, t, 0, false);
        assertTrue(out.events.isEmpty());
        rc.keyUp(Key.OK, t + 80, false);
        assertEquals(Arrays.asList("left down", "left up"), out.events);
    }

    @Test public void longPressOkIsARightClickAndNoLeftClick() {
        // `input keyevent --longpress`: a down, a repeat flagged long-press, an up.
        rc.keyDown(Key.OK, t, 0, false);
        rc.keyDown(Key.OK, t + 500, 1, true);
        rc.keyUp(Key.OK, t + 520, false);
        assertEquals(Arrays.asList("right down", "right up"), out.events);
    }

    @Test public void aRealHoldOfOkPast500msIsARightClick() {
        rc.keyDown(Key.OK, t, 0, false);
        rc.keyDown(Key.OK, t + 450, 1, false);
        assertTrue(out.events.isEmpty());
        rc.keyDown(Key.OK, t + 500, 2, false);
        rc.keyDown(Key.OK, t + 550, 3, false);
        rc.keyUp(Key.OK, t + 600, false);
        assertEquals(Arrays.asList("right down", "right up"), out.events);
    }

    @Test public void aCanceledOkDoesNothing() {
        rc.keyDown(Key.OK, t, 0, false);
        rc.keyUp(Key.OK, t + 50, true);
        assertTrue(out.events.isEmpty());
    }

    @Test public void backOpensTheMenuOnKeyUpAndSendsNothingToThePc() {
        assertTrue(rc.keyDown(Key.BACK, t, 0, false));
        assertEquals(0, out.menus);
        assertTrue(rc.keyUp(Key.BACK, t + 1, false));
        assertEquals(1, out.menus);
        assertEquals(Arrays.asList("menu"), out.events);
    }

    @Test public void pushingAgainstTheTopEdgeScrollsUpOneNotchPerTap() {
        for (int i = 0; i < 40; i++) tap(Key.UP); // 40 × 1.5 % of 1920 ≫ 540
        assertEquals(0, out.y);
        int notches = out.scroll / 120;
        assertTrue("scrolled up: " + out.scroll, notches >= 1);
        out.events.clear(); out.scroll = 0;
        tap(Key.UP);
        assertEquals(120, out.scroll);
        tap(Key.DOWN);
        assertEquals(120, out.scroll);
        assertTrue(out.y > 0);
    }

    @Test public void pushingAgainstTheBottomEdgeScrollsDownAndTheSidesScrollSideways() {
        for (int i = 0; i < 40; i++) tap(Key.DOWN);
        out.scroll = 0;
        tap(Key.DOWN);
        assertEquals(H - 1, out.y);
        assertEquals(-120, out.scroll);
        for (int i = 0; i < 60; i++) tap(Key.RIGHT);
        out.hscroll = 0;
        tap(Key.RIGHT);
        assertEquals(W - 1, out.x);
        assertEquals(120, out.hscroll);
        for (int i = 0; i < 80; i++) tap(Key.LEFT);
        out.hscroll = 0;
        tap(Key.LEFT);
        assertEquals(0, out.x);
        assertEquals(-120, out.hscroll);
    }

    @Test public void aGlideIntoTheEdgeScrollsSmoothlyNotAFloodOfNotches() {
        for (int i = 0; i < 40; i++) tap(Key.UP);
        out.scroll = 0;
        // An injected hold of one second against the edge.
        rc.keyDown(Key.UP, t, 0, false); rc.keyUp(Key.UP, t + 1, false);
        for (long dt = 400; dt <= 1400; dt += 100) {
            rc.keyDown(Key.UP, t + dt, 0, false); rc.keyUp(Key.UP, t + dt + 1, false);
            for (long s = 0; s < 100; s += 10) rc.tick(t + dt + s);
        }
        // Somewhere between a few and ~25 notches in about a second.
        assertTrue("scroll " + out.scroll, out.scroll >= 3 * 120 && out.scroll <= 25 * 120);
    }

    @Test public void scrollModeScrollsWithTheDpad() {
        rc.setMode(Mode.SCROLL);
        tap(Key.UP);
        tap(Key.UP);
        tap(Key.DOWN);
        tap(Key.RIGHT);
        assertEquals(120, out.scroll);
        assertEquals(120, out.hscroll);
        assertEquals(-1, out.x);
        tap(Key.OK);
        assertTrue(out.events.contains("left down"));
    }

    @Test public void arrowModeSendsArrowKeysAndEnter() {
        rc.setMode(Mode.ARROWS);
        rc.keyDown(Key.LEFT, t, 0, false);
        rc.keyUp(Key.LEFT, t + 1, false);
        rc.keyDown(Key.UP, t, 0, false);
        rc.keyDown(Key.UP, t + 500, 1, false);
        rc.keyUp(Key.UP, t + 550, false);
        rc.keyDown(Key.OK, t, 0, false);
        rc.keyUp(Key.OK, t + 50, false);
        assertEquals(Arrays.asList("key 25 down", "key 25 up", "key 26 down", "key 26 down", "key 26 up",
                "key d down", "key d up"), out.events);
        rc.tick(t + 2000);
        assertEquals(-1, out.x);
    }

    @Test public void arrowModeLongPressOkIsStillARightClick() {
        rc.setMode(Mode.ARROWS);
        rc.keyDown(Key.OK, t, 0, false);
        rc.keyDown(Key.OK, t + 500, 1, true);
        rc.keyUp(Key.OK, t + 501, false);
        assertEquals(Arrays.asList("right down", "right up"), out.events);
    }

    @Test public void holdLeftButtonDragsUntilTheNextOk() {
        rc.setHoldLeft(true);
        assertEquals(Arrays.asList("left down"), out.events);
        assertTrue(rc.isHoldingLeft());
        tap(Key.RIGHT);
        tap(Key.OK);
        assertEquals("left up", out.without("move").get(1));
        assertEquals(2, out.without("move").size());
        assertFalse(rc.isHoldingLeft());
    }

    @Test public void whileAGameCapturesTheCursorTheDpadMovesRelativelyAndEdgesDontScroll() {
        rc.setCursorCaptured(true);
        for (int i = 0; i < 50; i++) tap(Key.UP);
        assertEquals(-1, out.y);
        assertEquals(0, out.scroll);
        assertEquals(-50 * Math.round(PointerMotion.TAP_STEP * W), out.relY, 50);
        rc.setCursorCaptured(false);
        out.events.clear();
        tap(Key.RIGHT);
        assertEquals("moveTo", out.events.get(0));
    }

    @Test public void resetLetsGoOfAHeldButtonAndStopsMotion() {
        rc.setHoldLeft(true);
        rc.keyDown(Key.RIGHT, t, 0, false);
        rc.keyDown(Key.RIGHT, t + 400, 0, false);
        rc.reset();
        assertFalse(rc.isHoldingLeft());
        assertTrue(out.events.contains("left up"));
        assertFalse(rc.needsTicks(t + 450));
    }

    @Test public void theStreamSizeCanChange() {
        rc.setStreamSize(1280, 720);
        tap(Key.LEFT);
        assertEquals(640 - Math.round(PointerMotion.TAP_STEP * 1280), out.x);
        assertEquals(360, out.y);
    }
}
