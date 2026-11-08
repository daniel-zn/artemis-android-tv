package com.limelight.artemis;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.artemis.remote.KeyCombos;
import com.limelight.artemis.remote.RemoteController.Mode;

/** The session menu: Back during a session (docs/ARTEMIS-TV.md, "The session menu"). */
public final class SessionMenu implements SessionRemote.Menu {
    public interface Actions {
        void onMenuShown();
        void onMenuHidden();
        void keyboard();
        void setMode(Mode m);
        void setHoldLeft(boolean hold);
        void sendKeys(KeyCombos.Combo c);
        void setMicMuted(boolean muted);
        void setStatsShown(boolean shown);
        void disconnect();
        void stopApp();
    }

    /** What the menu shows when it opens. */
    public static final class State {
        public String pcName, appName;
        public Mode mode = Mode.POINTER;
        public boolean holdingLeft;
        public boolean micAvailable, micMuted;
        public boolean statsShown;
    }

    private final Activity activity;
    private final Actions actions;
    private final View root;
    private final TextView title, subtitle, resume, holdLeft, mic, stats, stop;
    private final TextView[] modes = new TextView[3];
    private State state = new State();
    private AlertDialog confirm;

    public SessionMenu(Activity activity, Actions actions) {
        this.activity = activity;
        this.actions = actions;
        root = LayoutInflater.from(activity).inflate(R.layout.artemis_session_menu, null);
        activity.addContentView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        title = root.findViewById(R.id.artemisMenuTitle);
        subtitle = root.findViewById(R.id.artemisMenuSubtitle);
        resume = root.findViewById(R.id.artemisMenuResume);
        holdLeft = root.findViewById(R.id.artemisMenuHoldLeft);
        mic = root.findViewById(R.id.artemisMenuMic);
        stats = root.findViewById(R.id.artemisMenuStats);
        stop = root.findViewById(R.id.artemisMenuStop);
        modes[0] = root.findViewById(R.id.artemisMenuModePointer);
        modes[1] = root.findViewById(R.id.artemisMenuModeScroll);
        modes[2] = root.findViewById(R.id.artemisMenuModeArrows);

        resume.setOnClickListener(v -> hide());
        root.findViewById(R.id.artemisMenuKeyboard).setOnClickListener(v -> {
            hide();
            actions.keyboard();
        });
        for (int i = 0; i < 3; i++) {
            final Mode m = Mode.values()[i];
            modes[i].setOnClickListener(v -> {
                state.mode = m;
                actions.setMode(m);
                hide();
            });
        }
        holdLeft.setOnClickListener(v -> {
            state.holdingLeft = !state.holdingLeft;
            actions.setHoldLeft(state.holdingLeft);
            hide();
        });
        root.findViewById(R.id.artemisMenuKeyEsc).setOnClickListener(v -> keys(KeyCombos.Combo.ESC));
        root.findViewById(R.id.artemisMenuKeyWin).setOnClickListener(v -> keys(KeyCombos.Combo.WINDOWS));
        root.findViewById(R.id.artemisMenuKeyAltTab).setOnClickListener(v -> keys(KeyCombos.Combo.ALT_TAB));
        root.findViewById(R.id.artemisMenuKeyCad).setOnClickListener(v -> keys(KeyCombos.Combo.CTRL_ALT_DELETE));
        mic.setOnClickListener(v -> {
            state.micMuted = !state.micMuted;
            actions.setMicMuted(state.micMuted);
            refresh();
        });
        stats.setOnClickListener(v -> {
            state.statsShown = !state.statsShown;
            actions.setStatsShown(state.statsShown);
            refresh();
        });
        root.findViewById(R.id.artemisMenuDisconnect).setOnClickListener(v -> {
            hide();
            actions.disconnect();
        });
        stop.setOnClickListener(v -> confirmStop());

        // A click on the dimmed stream area (a mouse, a touch) closes the menu too.
        root.setOnClickListener(v -> hide());
        root.findViewById(R.id.artemisMenuPanel).setOnClickListener(v -> { });

        applyFont(root);
    }

    private static void applyFont(View v) {
        if (v instanceof TextView) {
            ArtemisFont.apply((TextView) v);
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                applyFont(g.getChildAt(i));
            }
        }
    }

    private void keys(KeyCombos.Combo c) {
        hide();
        actions.sendKeys(c);
    }

    private void confirmStop() {
        String app = state.appName != null ? state.appName : "";
        confirm = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.artemis_menu_stop_confirm_title, app))
                .setMessage(activity.getString(R.string.artemis_menu_stop_confirm, app))
                .setPositiveButton(R.string.artemis_menu_stop_confirm_ok, (d, w) -> {
                    hide();
                    actions.stopApp();
                })
                .setNegativeButton(R.string.artemis_menu_cancel, null)
                .create();
        confirm.setOnShowListener(d -> {
            // Cancel is focused first: OK twice doesn't stop the app by accident.
            View cancel = confirm.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (cancel != null) {
                cancel.requestFocus();
            }
        });
        confirm.show();
    }

    private void refresh() {
        String app = state.appName != null ? state.appName : "";
        ArtemisFont.setText(title, app);
        ArtemisFont.setText(subtitle, state.pcName != null ? state.pcName : "");
        for (int i = 0; i < 3; i++) {
            modes[i].setSelected(state.mode.ordinal() == i);
        }
        ArtemisFont.setText(holdLeft, activity.getString(state.holdingLeft ? R.string.artemis_menu_holding_left
                : R.string.artemis_menu_hold_left));
        mic.setVisibility(state.micAvailable ? View.VISIBLE : View.GONE);
        ArtemisFont.setText(mic, activity.getString(state.micMuted ? R.string.artemis_menu_mic_off
                : R.string.artemis_menu_mic_on));
        ArtemisFont.setText(stats, activity.getString(state.statsShown ? R.string.artemis_menu_stats_on
                : R.string.artemis_menu_stats_off));
        ArtemisFont.setText(stop, activity.getString(R.string.artemis_menu_stop, app));
    }

    public void show(State s) {
        state = s;
        refresh();
        root.setVisibility(View.VISIBLE);
        root.bringToFront();
        resume.requestFocus();
        actions.onMenuShown();
    }

    @Override
    public boolean isShowing() {
        return root.getVisibility() == View.VISIBLE;
    }

    @Override
    public void hide() {
        if (confirm != null && confirm.isShowing()) {
            confirm.dismiss();
        }
        if (!isShowing()) {
            return;
        }
        root.setVisibility(View.GONE);
        actions.onMenuHidden();
    }
}
