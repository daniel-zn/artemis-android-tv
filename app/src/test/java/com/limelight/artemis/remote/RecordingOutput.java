package com.limelight.artemis.remote;

import java.util.ArrayList;
import java.util.List;

/** Records what the controller sends to the PC, as short strings. */
final class RecordingOutput implements RemoteOutput {
    final List<String> events = new ArrayList<>();
    int x = -1, y = -1, relX, relY, scroll, hscroll;
    int menus;

    @Override public void moveTo(int x, int y, int refW, int refH) {
        this.x = x; this.y = y;
        events.add("moveTo");
    }
    @Override public void moveBy(int dx, int dy) {
        relX += dx; relY += dy;
        events.add("moveBy");
    }
    @Override public void button(boolean down, int button) {
        events.add((button == RemoteOutput.BUTTON_LEFT ? "left" : button == RemoteOutput.BUTTON_RIGHT ? "right" : "b" + button)
                + (down ? " down" : " up"));
    }
    @Override public void scroll(int amount) { scroll += amount; events.add("scroll " + amount); }
    @Override public void hscroll(int amount) { hscroll += amount; events.add("hscroll " + amount); }
    @Override public void key(short vk, boolean down) { events.add("key " + Integer.toHexString(vk) + (down ? " down" : " up")); }
    @Override public void openMenu() { menus++; events.add("menu"); }

    List<String> without(String prefix) {
        List<String> l = new ArrayList<>();
        for (String e : events) if (!e.startsWith(prefix)) l.add(e);
        return l;
    }
}
