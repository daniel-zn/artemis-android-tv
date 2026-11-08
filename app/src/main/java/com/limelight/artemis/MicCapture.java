package com.limelight.artemis;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;

/**
 * The microphone to an Apollo host (its v2 microphone stream): 48 kHz mono 16-bit from Android's
 * voice-communication source (with its echo cancellation and noise suppression where the TV has them), in 20 ms
 * frames, Opus-encoded natively and sent while the host has the stream open. Muting stops sending, not capture,
 * so unmuting is instant.
 */
public final class MicCapture {
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SAMPLES = 960;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean muted;

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isRunning() {
        return running;
    }

    /** Starts capturing (the caller checked RECORD_AUDIO). False if the microphone can't be opened. */
    @SuppressLint("MissingPermission")
    public synchronized boolean start() {
        if (running) {
            return true;
        }
        if (!MoonBridge.micStart()) {
            return false;
        }
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            MoonBridge.micStop();
            return false;
        }
        final AudioRecord rec;
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, FRAME_SAMPLES * 2 * 4));
        } catch (RuntimeException e) {
            LimeLog.warning("Artemis mic: " + e);
            MoonBridge.micStop();
            return false;
        }
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
            rec.release();
            MoonBridge.micStop();
            return false;
        }
        AcousticEchoCanceler aec = AcousticEchoCanceler.isAvailable() ? AcousticEchoCanceler.create(rec.getAudioSessionId()) : null;
        NoiseSuppressor ns = NoiseSuppressor.isAvailable() ? NoiseSuppressor.create(rec.getAudioSessionId()) : null;
        if (aec != null) aec.setEnabled(true);
        if (ns != null) ns.setEnabled(true);

        running = true;
        thread = new Thread(() -> {
            short[] frame = new short[FRAME_SAMPLES];
            try {
                rec.startRecording();
                while (running) {
                    int got = 0;
                    while (running && got < FRAME_SAMPLES) {
                        int n = rec.read(frame, got, FRAME_SAMPLES - got);
                        if (n < 0) {
                            LimeLog.warning("Artemis mic: read " + n);
                            running = false;
                            break;
                        }
                        got += n;
                    }
                    if (running && !muted) {
                        MoonBridge.micSendFrame(frame);
                    }
                }
            } catch (RuntimeException e) {
                LimeLog.warning("Artemis mic: " + e);
            } finally {
                running = false;
                try { rec.stop(); } catch (RuntimeException ignored) { }
                rec.release();
                if (aec != null) aec.release();
                if (ns != null) ns.release();
                MoonBridge.micStop();
            }
        }, "Artemis mic");
        thread.start();
        return true;
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }
}
