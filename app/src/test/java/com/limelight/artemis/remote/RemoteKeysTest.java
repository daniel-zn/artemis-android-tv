package com.limelight.artemis.remote;

import static org.junit.Assert.*;

import android.view.KeyEvent;

import org.junit.Test;

import com.limelight.artemis.remote.RemoteController.Key;

public class RemoteKeysTest {
    private static final int VIRTUAL = -1, REMOTE = 7, PAD = 9, KEYBOARD = 11;

    private static Key k(int code, int deviceId, int flags, boolean gamepad, boolean alphabetic) {
        return RemoteKeys.classify(code, deviceId, flags, gamepad, alphabetic);
    }

    @Test public void aRemotesDpadOkAndBackAreTheRemotes() {
        assertEquals(Key.UP, k(KeyEvent.KEYCODE_DPAD_UP, REMOTE, 0, false, false));
        assertEquals(Key.DOWN, k(KeyEvent.KEYCODE_DPAD_DOWN, REMOTE, 0, false, false));
        assertEquals(Key.LEFT, k(KeyEvent.KEYCODE_DPAD_LEFT, REMOTE, 0, false, false));
        assertEquals(Key.RIGHT, k(KeyEvent.KEYCODE_DPAD_RIGHT, REMOTE, 0, false, false));
        assertEquals(Key.OK, k(KeyEvent.KEYCODE_DPAD_CENTER, REMOTE, 0, false, false));
        assertEquals(Key.OK, k(KeyEvent.KEYCODE_ENTER, REMOTE, 0, false, false));
        assertEquals(Key.BACK, k(KeyEvent.KEYCODE_BACK, REMOTE, 0, false, false));
    }

    @Test public void injectedKeysFromTheVirtualKeyboardAreTheRemotes() {
        // The Pi's remote bridge injects keys with `input keyevent`: the virtual keyboard, which is alphabetic.
        assertEquals(Key.UP, k(KeyEvent.KEYCODE_DPAD_UP, VIRTUAL, 0, false, true));
        assertEquals(Key.OK, k(KeyEvent.KEYCODE_DPAD_CENTER, VIRTUAL, 0, false, true));
        assertEquals(Key.BACK, k(KeyEvent.KEYCODE_BACK, VIRTUAL, 0, false, true));
    }

    @Test public void theOnScreenKeyboardsKeysAreNot() {
        assertNull(k(KeyEvent.KEYCODE_ENTER, VIRTUAL, KeyEvent.FLAG_SOFT_KEYBOARD, false, true));
        assertNull(k(KeyEvent.KEYCODE_DPAD_LEFT, VIRTUAL, KeyEvent.FLAG_SOFT_KEYBOARD, false, true));
    }

    @Test public void controllersAndKeyboardsAreNot() {
        assertNull(k(KeyEvent.KEYCODE_DPAD_UP, PAD, 0, true, false));
        assertNull(k(KeyEvent.KEYCODE_BACK, PAD, 0, true, false));
        assertNull(k(KeyEvent.KEYCODE_DPAD_UP, KEYBOARD, 0, false, true));
        assertNull(k(KeyEvent.KEYCODE_ENTER, KEYBOARD, 0, false, true));
    }

    @Test public void aKeyboardsBackStillOpensTheMenu() {
        // Android's Back (a keyboard's Back key, the system Back) never ends the session by accident.
        assertEquals(Key.BACK, k(KeyEvent.KEYCODE_BACK, KEYBOARD, 0, false, true));
    }

    @Test public void otherKeysAreNot() {
        assertNull(k(KeyEvent.KEYCODE_A, REMOTE, 0, false, false));
        assertNull(k(KeyEvent.KEYCODE_VOLUME_UP, REMOTE, 0, false, false));
        assertNull(k(KeyEvent.KEYCODE_HOME, REMOTE, 0, false, false));
    }
}
