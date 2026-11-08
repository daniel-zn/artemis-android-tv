package com.limelight.artemis;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.artemis.remote.KeyCombos;
import com.limelight.artemis.remote.RemoteController;
import com.limelight.artemis.remote.RemoteOutput;

/**
 * Debug builds only: the session's remote and menu without a PC. What would go to the PC is logged under the tag
 * "ArtemisRemote" (the emulator checks read it) and the pointer is drawn.
 */
public class ArtemisDemoSession extends Activity {
    static final String TAG = "ArtemisRemote";
    static final int W = 1920, H = 1080;

    private SessionRemote remote;
    private SessionMenu menu;
    private PointerView pointer;

    static final class PointerView extends View {
        float x = -1, y = -1;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        PointerView(Activity a) {
            super(a);
            setBackgroundColor(Color.rgb(20, 24, 32));
            p.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (x >= 0) {
                float sx = x / W * getWidth(), sy = y / H * getHeight();
                c.drawCircle(sx, sy, 12, p);
            }
        }
    }

    private void log(String s) {
        Log.i(TAG, s);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        pointer = new PointerView(this);
        setContentView(pointer, new FrameLayout.LayoutParams(-1, -1));
        RemoteOutput out = new RemoteOutput() {
            @Override public void moveTo(int x, int y, int refW, int refH) {
                log("moveTo " + x + " " + y);
                pointer.x = x;
                pointer.y = y;
                pointer.invalidate();
            }
            @Override public void moveBy(int dx, int dy) { log("moveBy " + dx + " " + dy); }
            @Override public void button(boolean down, int button) {
                String name = button == BUTTON_LEFT ? "left" : button == BUTTON_RIGHT ? "right" : "middle";
                log(name + (down ? " down" : " up"));
            }
            @Override public void scroll(int amount) { log("scroll " + amount); }
            @Override public void hscroll(int amount) { log("hscroll " + amount); }
            @Override public void key(short vk, boolean down) {
                log("key " + Integer.toHexString(vk) + (down ? " down" : " up"));
            }
            @Override public void openMenu() {
                log("menu");
                SessionMenu.State st = new SessionMenu.State();
                st.pcName = "Demo PC";
                st.appName = "Desktop";
                st.mode = remote.controller().getMode();
                st.holdingLeft = remote.controller().isHoldingLeft();
                menu.show(st);
            }
        };
        final RemoteOutput output = out;
        menu = new SessionMenu(this, new SessionMenu.Actions() {
            @Override public void onMenuShown() { log("menu shown"); }
            @Override public void onMenuHidden() { log("menu hidden"); pointer.requestFocus(); }
            @Override public void keyboard() { log("action keyboard"); }
            @Override public void setMode(RemoteController.Mode m) { log("action mode " + m); remote.controller().setMode(m); }
            @Override public void setHoldLeft(boolean h) { log("action hold " + h); remote.controller().setHoldLeft(h); }
            @Override public void sendKeys(KeyCombos.Combo c) { log("action keys " + c); KeyCombos.send(c, output); }
            @Override public void setMicMuted(boolean m) { log("action mic " + m); }
            @Override public void setStatsShown(boolean s) { log("action stats " + s); }
            @Override public void disconnect() { log("action disconnect"); finish(); }
            @Override public void stopApp() { log("action stop"); finish(); }
        });
        remote = new SessionRemote(out, menu, W, H);
        pointer.setFocusable(true);
        pointer.requestFocus();
        if (getIntent().getBooleanExtra("captured", false)) {
            remote.controller().setCursorCaptured(true);
        }
        log("demo ready");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (remote.onKeyDown(event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (remote.onKeyUp(event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        remote.stop();
    }
}
