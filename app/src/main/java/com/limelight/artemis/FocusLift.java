package com.limelight.artemis;

import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

/** The focused PC or app in a grid lifts a little (with the gold ring from artemis_grid_selector). */
public final class FocusLift {
    public static final float LIFT = 1.08f;

    private FocusLift() { }

    public static void attach(final AbsListView list) {
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private View lifted;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                drop();
                if (view != null && list.hasFocus()) {
                    view.animate().scaleX(LIFT).scaleY(LIFT).setDuration(120).start();
                    lifted = view;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                drop();
            }

            private void drop() {
                if (lifted != null) {
                    lifted.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    lifted = null;
                }
            }
        });
        list.setOnFocusChangeListener((v, hasFocus) -> {
            View sel = list.getSelectedView();
            if (sel != null) {
                float s = hasFocus ? LIFT : 1f;
                sel.animate().scaleX(s).scaleY(s).setDuration(120).start();
            }
        });
    }
}
