package com.limelight.artemis.remote;

import android.view.KeyEvent;

import com.limelight.artemis.remote.RemoteController.Key;

/**
 * Which key events are the TV remote's (docs/ARTEMIS-TV.md, "Which keys are the remote's"): the D-pad, OK and
 * Back of anything that isn't a game controller or a full keyboard. Keys injected with `input keyevent` come
 * from Android's virtual keyboard (device -1), which says it's alphabetic; they count as the remote's. The
 * on-screen keyboard's keys (FLAG_SOFT_KEYBOARD) don't. Back from anything but a controller opens the menu.
 */
public final class RemoteKeys {
    /** KeyCharacterMap.VIRTUAL_KEYBOARD */
    public static final int VIRTUAL_KEYBOARD = -1;

    private RemoteKeys() {}

    public static Key classify(int keyCode, int deviceId, int flags, boolean gameController, boolean alphabeticKeyboard) {
        if ((flags & KeyEvent.FLAG_SOFT_KEYBOARD) != 0) {
            return null;
        }
        boolean injected = deviceId == VIRTUAL_KEYBOARD;
        if (!injected && gameController) {
            return null;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return Key.BACK;
        }
        if (!injected && alphabeticKeyboard) {
            return null;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return Key.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return Key.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return Key.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return Key.RIGHT;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return Key.OK;
            default:
                return null;
        }
    }
}
