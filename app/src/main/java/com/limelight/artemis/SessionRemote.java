package com.limelight.artemis;

import android.view.Choreographer;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.limelight.artemis.remote.RemoteController;
import com.limelight.artemis.remote.RemoteController.Key;
import com.limelight.artemis.remote.RemoteKeys;
import com.limelight.artemis.remote.RemoteOutput;
import com.limelight.binding.input.ControllerHandler;

/**
 * The TV remote during a session: picks the remote's keys out of Game's key events, hands them to the
 * RemoteController and ticks it once a frame while the pointer glides. While the session menu is open, the
 * D-pad and OK are left to Android's focus (the menu's buttons) and Back closes the menu.
 */
public final class SessionRemote implements Choreographer.FrameCallback {
    public interface Menu {
        boolean isShowing();
        void hide();
    }

    private final RemoteController controller;
    private final Menu menu;
    private boolean ticking;
    // A Back whose down came while the menu was open (its up closes the menu, not reopens it)
    private boolean backDownInMenu;

    public SessionRemote(RemoteOutput out, Menu menu, int streamWidth, int streamHeight) {
        this.controller = new RemoteController(out, streamWidth, streamHeight);
        this.menu = menu;
    }

    public RemoteController controller() {
        return controller;
    }

    private static Key classify(KeyEvent e) {
        InputDevice dev = e.getDevice();
        boolean gamepad = ControllerHandler.isGameControllerDevice(dev);
        boolean alphabetic = dev != null && dev.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;
        return RemoteKeys.classify(e.getKeyCode(), e.getDeviceId(), e.getFlags(), gamepad, alphabetic);
    }

    /** True when it was the remote's (and so handled). */
    public boolean onKeyDown(KeyEvent e) {
        Key k = classify(e);
        if (k == null) {
            return false;
        }
        if (menu.isShowing()) {
            if (k == Key.BACK) {
                backDownInMenu = true;
                return true;
            }
            return false;
        }
        backDownInMenu = false;
        controller.keyDown(k, e.getEventTime(), e.getRepeatCount(), (e.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0);
        startTicking();
        return true;
    }

    public boolean onKeyUp(KeyEvent e) {
        Key k = classify(e);
        if (k == null) {
            return false;
        }
        if (menu.isShowing()) {
            if (k == Key.BACK) {
                backDownInMenu = false;
                if ((e.getFlags() & KeyEvent.FLAG_CANCELED) == 0) {
                    menu.hide();
                }
                return true;
            }
            return false;
        }
        if (k == Key.BACK && backDownInMenu) {
            // The menu closed between this Back's down and up some other way: don't reopen it.
            backDownInMenu = false;
            return true;
        }
        controller.keyUp(k, e.getEventTime(), (e.getFlags() & KeyEvent.FLAG_CANCELED) != 0);
        startTicking();
        return true;
    }

    private void startTicking() {
        if (!ticking && controller.needsTicks(android.os.SystemClock.uptimeMillis())) {
            ticking = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        long now = android.os.SystemClock.uptimeMillis();
        controller.tick(now);
        if (controller.needsTicks(now)) {
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            ticking = false;
        }
    }

    /** The session ends or the app goes to the background: let go of everything. */
    public void stop() {
        if (ticking) {
            Choreographer.getInstance().removeFrameCallback(this);
            ticking = false;
        }
        controller.reset();
    }
}
