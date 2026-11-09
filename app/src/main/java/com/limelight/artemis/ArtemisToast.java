package com.limelight.artemis;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

/**
 * The app's toasts: a navy pill in Orbix, sized for the sofa. Since Android 11 the system draws a plain text toast
 * itself (in its own font), so an activity's toasts carry their own view; anything else (a service) gets the
 * system's. Every toast in the app goes through here (ArtemisToastTest checks).
 */
public final class ArtemisToast {
    private ArtemisToast() { }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        if (!(context instanceof Activity)) {
            return Toast.makeText(context, text, duration);
        }
        float density = context.getResources().getDisplayMetrics().density;
        TextView view = new TextView(context);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        view.setTextColor(context.getResources().getColor(R.color.artemis_text));
        view.setBackgroundResource(R.drawable.artemis_toast);
        view.setGravity(Gravity.CENTER);
        view.setMaxWidth((int) (720 * density));
        int h = (int) (28 * density), v = (int) (14 * density);
        view.setPadding(h, v, h, v);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ArtemisFont.setText(view, text);
        } else {
            view.setText(text);
        }
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, (int) (64 * density));
        return toast;
    }

    public static Toast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getText(resId), duration);
    }
}
