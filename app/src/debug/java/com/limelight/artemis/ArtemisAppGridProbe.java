package com.limelight.artemis;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;

import com.limelight.AppView;
import com.limelight.R;
import com.limelight.grid.AppGridAdapter;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.utils.CacheHelper;

import java.io.OutputStream;

/**
 * Debug builds only: a PC's app grid with no PC, for scripts/emulator-check.sh. Two apps with flat box art in the
 * art cache, cyan "Steam" and magenta "Virtual Desktop" (running and focused, as in the projector photo of
 * 2026-11-26), shown by the app grid's own adapter, layout and focus lift. "cards ready" goes to the log under
 * "ArtemisRemote" once the focused card has lifted.
 */
public class ArtemisAppGridProbe extends Activity {
    private static final String UUID = "artemis-app-grid-probe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        seedArt(1, Color.CYAN);
        seedArt(2, Color.MAGENTA);

        ComputerDetails pc = new ComputerDetails();
        pc.uuid = UUID;
        pc.name = "Probe";
        PreferenceConfiguration prefs = PreferenceConfiguration.readPreferences(this);
        AppGridAdapter adapter = new AppGridAdapter(this, prefs, pc, "probe", false);
        AppView.AppObject steam = new AppView.AppObject(new NvApp("Steam", 1, false));
        AppView.AppObject desktop = new AppView.AppObject(new NvApp("Virtual Desktop", 2, false));
        desktop.isRunning = true;
        adapter.addApp(steam);
        adapter.addApp(desktop);

        setContentView(prefs.smallIconMode ? R.layout.app_grid_view_small : R.layout.app_grid_view);
        GridView grid = findViewById(R.id.fragmentView);
        grid.setAdapter(adapter);
        FocusLift.attach(grid);
        grid.requestFocus();
        grid.setSelection(1);
        grid.postDelayed(() -> Log.i(ArtemisDemoSession.TAG, "cards ready"), 1500);
    }

    private void seedArt(int appId, int color) {
        Bitmap art = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888);
        art.eraseColor(color);
        try (OutputStream out = CacheHelper.openCacheFileForOutput(getCacheDir(), "boxart", UUID, appId + ".png")) {
            art.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.e(ArtemisDemoSession.TAG, "card art not seeded", e);
        }
    }
}
