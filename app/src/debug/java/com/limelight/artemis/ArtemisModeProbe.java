package com.limelight.artemis;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Debug builds only: the mode a session's launch request would ask for, read the way Game does (the stored
 * settings, "Match this TV" reading this display) and put in the request the way NvHTTP does. Logged under
 * "ArtemisRemote" for scripts/emulator-check.sh. Extra "seed": "old" stores upstream's default first (an install
 * from before Match this TV), "manual" a chosen 1280x720 at 30 FPS.
 */
public class ArtemisModeProbe extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String seed = getIntent().getStringExtra("seed");
        if ("old".equals(seed)) {
            prefs.edit().clear().putString("list_resolution", "1280x720").putString("list_fps", "60")
                    .putInt("seekbar_bitrate_kbps", 10000).commit();
        } else if ("manual".equals(seed)) {
            prefs.edit().clear().putString("list_resolution", "1280x720").putString("list_fps", "30")
                    .putBoolean("artemis_match_tv_migrated", true).commit();
        } else if ("fresh".equals(seed)) {
            prefs.edit().clear().commit();
        }
        PreferenceConfiguration pc = PreferenceConfiguration.readPreferences(this);
        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(pc.width, pc.height)
                .setLaunchRefreshRate(pc.fps)
                .setRefreshRate(pc.fps)
                .setBitrate(pc.bitrate)
                .build();
        Log.i(ArtemisDemoSession.TAG, "launch mode=" + NvHTTP.launchMode(config.getWidth(), config.getHeight(),
                config.getLaunchRefreshRate()) + " bitrate=" + config.getBitrate());
        finish();
    }
}
