package com.limelight.artemis;

import com.limelight.artemis.remote.KeyCombos;
import com.limelight.artemis.remote.RemoteOutput;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

/** The remote's output to the PC, through Moonlight's connection. */
public final class ConnectionOutput implements RemoteOutput {
    /** KeyboardTranslator's prefix for Windows virtual keys. */
    private static final int KEY_PREFIX = 0x80;

    private final NvConnection conn;
    private final Runnable openMenu;
    private final KeyCombos.Modifiers modifiers = new KeyCombos.Modifiers();

    public ConnectionOutput(NvConnection conn, Runnable openMenu) {
        this.conn = conn;
        this.openMenu = openMenu;
    }

    private static short clamp(int v) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
    }

    @Override public void moveTo(int x, int y, int refW, int refH) {
        conn.sendMousePosition(clamp(x), clamp(y), clamp(refW), clamp(refH));
    }

    @Override public void moveBy(int dx, int dy) {
        conn.sendMouseMove(clamp(dx), clamp(dy));
    }

    @Override public void button(boolean down, int button) {
        if (down) {
            conn.sendMouseButtonDown((byte) button);
        } else {
            conn.sendMouseButtonUp((byte) button);
        }
    }

    @Override public void scroll(int amount) {
        conn.sendMouseHighResScroll(clamp(amount));
    }

    @Override public void hscroll(int amount) {
        conn.sendMouseHighResHScroll(clamp(amount));
    }

    @Override public void key(short vk, boolean down) {
        int mods = modifiers.update(vk, down);
        conn.sendKeyboardInput((short) ((KEY_PREFIX << 8) | vk),
                down ? KeyboardPacket.KEY_DOWN : KeyboardPacket.KEY_UP, (byte) mods, (byte) 0);
    }

    @Override public void openMenu() {
        openMenu.run();
    }
}
