package com.limelight.artemis;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.widget.TextView;

/**
 * Artemis's font, Orbix (assets/fonts, SIL OFL), with digits in the system font as in the iPhone and Mac apps
 * (Orbix's 1 looks like a 7).
 */
public final class ArtemisFont {
    private static Typeface orbix;

    private ArtemisFont() {}

    public static synchronized Typeface orbix(Context c) {
        if (orbix == null) {
            try {
                orbix = Typeface.createFromAsset(c.getApplicationContext().getAssets(), "fonts/Orbix-Regular.otf");
            } catch (RuntimeException e) {
                orbix = Typeface.DEFAULT;
            }
        }
        return orbix;
    }

    private static final class FontSpan extends MetricAffectingSpan {
        private final Typeface tf;

        FontSpan(Typeface tf) {
            this.tf = tf;
        }

        private void apply(Paint p) {
            p.setTypeface(tf);
        }

        @Override public void updateDrawState(TextPaint tp) { apply(tp); }
        @Override public void updateMeasureState(TextPaint tp) { apply(tp); }
    }

    /** The text in Orbix, its digits left in the view's own font. */
    public static CharSequence styled(Context c, CharSequence text) {
        if (text == null) {
            return null;
        }
        SpannableString s = new SpannableString(text);
        Typeface tf = orbix(c);
        int start = -1;
        for (int i = 0; i <= s.length(); i++) {
            boolean orbixChar = i < s.length() && !Character.isDigit(s.charAt(i));
            if (orbixChar && start < 0) {
                start = i;
            } else if (!orbixChar && start >= 0) {
                s.setSpan(new FontSpan(tf), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = -1;
            }
        }
        return s;
    }

    public static void apply(TextView v) {
        v.setText(styled(v.getContext(), v.getText()));
    }

    public static void setText(TextView v, CharSequence text) {
        v.setText(styled(v.getContext(), text));
    }
}
