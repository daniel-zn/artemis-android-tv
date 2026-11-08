package com.limelight.artemis;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

import com.limelight.LimeLog;

/** Reads the TV's display modes and its video decoders for {@link TvMode} ("Match this TV"). */
public final class TvDisplay {
    private TvDisplay() { }

    private static int[] decoderMax;
    private static boolean decoderRead;
    private static String lastLogged;

    /** The stream's mode for this TV, as the display shows it now. */
    public static TvMode.Choice choose(Context context) {
        TvMode.Mode current = null;
        TvMode.Mode[] supported = new TvMode.Mode[0];
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Display.Mode now = display.getMode();
                current = new TvMode.Mode(now.getPhysicalWidth(), now.getPhysicalHeight(), now.getRefreshRate());
                Display.Mode[] modes = display.getSupportedModes();
                supported = new TvMode.Mode[modes.length];
                for (int i = 0; i < modes.length; i++) {
                    supported[i] = new TvMode.Mode(modes[i].getPhysicalWidth(), modes[i].getPhysicalHeight(),
                            modes[i].getRefreshRate());
                }
            } else {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics(metrics);
                current = new TvMode.Mode(metrics.widthPixels, metrics.heightPixels, display.getRefreshRate());
            }
        }
        TvMode.Choice choice = TvMode.pick(current, supported, decoderMax());
        String line = "Match this TV: " + choice + " (display " + (current != null
                ? current.width + "x" + current.height + "@" + current.refreshRate : "unknown") + ", "
                + supported.length + " modes, decoder max "
                + (decoderMax != null ? decoderMax[0] + "x" + decoderMax[1] : "unknown") + ")";
        if (!line.equals(lastLogged)) {
            lastLogged = line;
            LimeLog.info(line);
        }
        return choice;
    }

    /** The largest standard size a hardware HEVC or H.264 decoder takes; null if none even takes 720p. */
    static synchronized int[] decoderMax() {
        if (decoderRead) {
            return decoderMax;
        }
        decoderRead = true;
        try {
            MediaCodecInfo[] codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
            for (int[] size : TvMode.STANDARD_SIZES) {
                for (MediaCodecInfo info : codecs) {
                    if (info.isEncoder() || isSoftware(info)) {
                        continue;
                    }
                    for (String type : info.getSupportedTypes()) {
                        if (!type.equalsIgnoreCase("video/hevc") && !type.equalsIgnoreCase("video/avc")) {
                            continue;
                        }
                        MediaCodecInfo.VideoCapabilities video = info.getCapabilitiesForType(type).getVideoCapabilities();
                        if (video != null && video.isSizeSupported(size[0], size[1])) {
                            decoderMax = size.clone();
                            return decoderMax;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LimeLog.warning("Match this TV: couldn't read the decoders: " + e);
        }
        return null;
    }

    private static boolean isSoftware(MediaCodecInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isSoftwareOnly();
        }
        String n = info.getName().toLowerCase();
        return n.startsWith("omx.google.") || n.startsWith("c2.android.");
    }
}
