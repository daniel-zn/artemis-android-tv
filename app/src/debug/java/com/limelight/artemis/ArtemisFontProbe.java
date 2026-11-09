package com.limelight.artemis;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug builds only, for scripts/emulator-check.sh: logs, for each of the app's windows (activities, dialogs, toasts),
 * how many of its visible texts are drawn in Orbix and which aren't ("font <window> orbix=N other=M | text | ...").
 * A text counts as Orbix when it measures as wide as the same text in Orbix with its digits in the system font
 * (ArtemisFont's rule). Reads the windows through WindowManagerGlobal, so the check relaxes hidden-API checks on the
 * emulator first.
 */
public class ArtemisFontProbe extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String toast = intent.getStringExtra("toast");
        if (toast != null) {
            // As the app shows one: from the activity in front (the probe's own context is the application's)
            Activity front = resumedActivity();
            ArtemisToast.makeText(front != null ? front : context, toast, Toast.LENGTH_LONG).show();
            return;
        }
        List<View> roots = new ArrayList<>();
        try {
            Class<?> wmg = Class.forName("android.view.WindowManagerGlobal");
            Object global = wmg.getMethod("getInstance").invoke(null);
            Field views = wmg.getDeclaredField("mViews");
            views.setAccessible(true);
            synchronized (global) {
                roots.addAll((List<View>) views.get(global));
            }
        } catch (Exception e) {
            Log.i(ArtemisDemoSession.TAG, "font probe failed: " + e);
            return;
        }
        for (View root : roots) {
            if (!root.isShown()) {
                continue;
            }
            int[] counts = new int[2];
            StringBuilder others = new StringBuilder();
            walk(context, root, counts, others);
            String title = root.getLayoutParams() instanceof WindowManager.LayoutParams
                    ? String.valueOf(((WindowManager.LayoutParams) root.getLayoutParams()).getTitle()) : "?";
            Log.i(ArtemisDemoSession.TAG, "font " + title.substring(title.lastIndexOf('/') + 1).replace(' ', '_')
                    + " orbix=" + counts[0] + " other=" + counts[1] + others);
        }
    }

    /** The app's resumed activity, from ActivityThread (hidden API; the check relaxes the checks). */
    private static Activity resumedActivity() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            Field f = at.getDeclaredField("mActivities");
            f.setAccessible(true);
            for (Object record : ((java.util.Map<?, ?>) f.get(thread)).values()) {
                Field paused = record.getClass().getDeclaredField("paused");
                Field activity = record.getClass().getDeclaredField("activity");
                paused.setAccessible(true);
                activity.setAccessible(true);
                if (!paused.getBoolean(record)) {
                    return (Activity) activity.get(record);
                }
            }
        } catch (Exception e) {
            Log.i(ArtemisDemoSession.TAG, "font probe: no activity: " + e);
        }
        return null;
    }

    private static void walk(Context context, View v, int[] counts, StringBuilder others) {
        if (v.getVisibility() != View.VISIBLE) {
            return;
        }
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            CharSequence text = tv.getText().length() > 0 ? tv.getText() : tv.getHint();
            if (text != null && letters(text) >= 2 && v.getWidth() > 0) {
                if (inOrbix(context, tv, text)) {
                    counts[0]++;
                } else {
                    counts[1]++;
                    if (counts[1] <= 4) {
                        String s = text.toString().replace('\n', ' ');
                        others.append(" | ").append(s.length() > 40 ? s.substring(0, 40) : s);
                    }
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(context, g.getChildAt(i), counts, others);
            }
        }
    }

    private static int letters(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                n++;
            }
        }
        return n;
    }

    private static boolean inOrbix(Context context, TextView tv, CharSequence text) {
        TextPaint actual = new TextPaint(tv.getPaint());
        TextPaint reference = new TextPaint(tv.getPaint());
        Typeface tf = tv.getTypeface();
        reference.setTypeface(Typeface.create(Typeface.DEFAULT, tf != null ? tf.getStyle() : Typeface.NORMAL));
        float a = Layout.getDesiredWidth(text, actual);
        float r = Layout.getDesiredWidth(ArtemisFont.styled(context, text.toString()), reference);
        return r > 0 && Math.abs(a - r) <= r * 0.02f;
    }
}
