package com.limelight.artemis.remote;

/** What the remote sends to the PC (MoonBridge in a session, a log in the demo session). */
public interface RemoteOutput {
    /** Moonlight's mouse button numbers. */
    int BUTTON_LEFT = 1, BUTTON_MIDDLE = 2, BUTTON_RIGHT = 3;

    /** The pointer to (x, y) of a refW × refH screen (the stream's). */
    void moveTo(int x, int y, int refW, int refH);
    /** A relative move, in the stream's pixels. */
    void moveBy(int dx, int dy);
    void button(boolean down, int button);
    /** High-resolution scroll: +120 is one notch up. */
    void scroll(int amount);
    /** High-resolution sideways scroll: +120 is one notch right. */
    void hscroll(int amount);
    /** A Windows virtual-key code down or up. */
    void key(short vk, boolean down);
    /** Back: open the session menu. */
    void openMenu();
}
