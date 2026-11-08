package com.limelight.artemis.remote;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class KeyCombosTest {
    @Test public void escIsOneKey() {
        RecordingOutput out = new RecordingOutput();
        KeyCombos.send(KeyCombos.Combo.ESC, out);
        assertEquals(Arrays.asList("key 1b down", "key 1b up"), out.events);
    }

    @Test public void altTabHoldsAltAroundTab() {
        RecordingOutput out = new RecordingOutput();
        KeyCombos.send(KeyCombos.Combo.ALT_TAB, out);
        assertEquals(Arrays.asList("key 12 down", "key 9 down", "key 9 up", "key 12 up"), out.events);
    }

    @Test public void ctrlAltDeleteAndWindows() {
        RecordingOutput out = new RecordingOutput();
        KeyCombos.send(KeyCombos.Combo.CTRL_ALT_DELETE, out);
        assertEquals(Arrays.asList("key 11 down", "key 12 down", "key 2e down", "key 2e up", "key 12 up", "key 11 up"), out.events);
        out.events.clear();
        KeyCombos.send(KeyCombos.Combo.WINDOWS, out);
        assertEquals(Arrays.asList("key 5b down", "key 5b up"), out.events);
    }

    @Test public void modifiersFollowTheHeldKeys() {
        KeyCombos.Modifiers m = new KeyCombos.Modifiers();
        assertEquals(4, m.update((short) 0x12, true));
        // Moonlight's modifier bits: shift 1, ctrl 2, alt 4, meta 8. A key's own down already counts.
        assertEquals(4, m.current());
        assertEquals(4 | 2, m.update((short) 0x11, true));
        assertEquals(4 | 2, m.update((short) 0x2E, true));
        assertEquals(2, m.update((short) 0x12, false));
        assertEquals(0, m.update((short) 0x11, false));
        assertEquals(8, m.update((short) 0x5B, true));
        assertEquals(0, m.update((short) 0x5B, false));
        assertEquals(1, m.update((short) 0x10, true));
    }
}
