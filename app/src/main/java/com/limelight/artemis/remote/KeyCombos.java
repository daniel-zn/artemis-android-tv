package com.limelight.artemis.remote;

/** The session menu's keys (Esc, Windows, Alt+Tab, Ctrl+Alt+Delete), and the modifier state keys send with. */
public final class KeyCombos {
    public enum Combo { ESC, WINDOWS, ALT_TAB, CTRL_ALT_DELETE }

    static final short VK_TAB = 0x09, VK_SHIFT = 0x10, VK_CONTROL = 0x11, VK_MENU = 0x12, VK_ESCAPE = 0x1B,
            VK_DELETE = 0x2E, VK_LWIN = 0x5B, VK_RWIN = 0x5C, VK_LSHIFT = 0xA0, VK_RSHIFT = 0xA1,
            VK_LCONTROL = 0xA2, VK_RCONTROL = 0xA3, VK_LMENU = 0xA4, VK_RMENU = 0xA5;

    private KeyCombos() {}

    private static short[] keys(Combo c) {
        switch (c) {
            case ESC: return new short[]{VK_ESCAPE};
            case WINDOWS: return new short[]{VK_LWIN};
            case ALT_TAB: return new short[]{VK_MENU, VK_TAB};
            default: return new short[]{VK_CONTROL, VK_MENU, VK_DELETE};
        }
    }

    /** Presses the combo's keys in order and lets go of them in reverse. */
    public static void send(Combo c, RemoteOutput out) {
        short[] k = keys(c);
        for (short vk : k) {
            out.key(vk, true);
        }
        for (int i = k.length - 1; i >= 0; i--) {
            out.key(k[i], false);
        }
    }

    /** Moonlight's modifier bits for the keys held so far. */
    public static final class Modifiers {
        public static final int SHIFT = 0x01, CTRL = 0x02, ALT = 0x04, META = 0x08;
        private int held;

        private static int bit(short vk) {
            switch (vk) {
                case VK_SHIFT: case VK_LSHIFT: case VK_RSHIFT: return SHIFT;
                case VK_CONTROL: case VK_LCONTROL: case VK_RCONTROL: return CTRL;
                case VK_MENU: case VK_LMENU: case VK_RMENU: return ALT;
                case VK_LWIN: case VK_RWIN: return META;
                default: return 0;
            }
        }

        /** The modifiers to send with this key event, counting the key itself on its down. */
        public int update(short vk, boolean down) {
            int b = bit(vk);
            if (down) {
                held |= b;
            } else {
                held &= ~b;
            }
            return held;
        }

        public int current() {
            return held;
        }
    }
}
