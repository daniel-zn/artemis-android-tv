package com.limelight.artemis;

import static org.junit.Assert.assertEquals;

import com.limelight.artemis.TvMode.Choice;
import com.limelight.artemis.TvMode.Mode;

import org.junit.Test;

/** "Match this TV": the stream's mode from the TV's (docs/ARTEMIS-TV.md, "Stream mode"). */
public class TvModeTest {
    private static final int[] DECODER_4K = {3840, 2160}, DECODER_1080 = {1920, 1080};

    private static Mode m(int w, int h, float hz) {
        return new Mode(w, h, hz);
    }

    private static Choice pick(Mode current, int[] decoder, Mode... supported) {
        return TvMode.pick(current, supported, decoder);
    }

    @Test
    public void aFullHdProjectorStreamsAtItsOwnMode() {
        Mode now = m(1920, 1080, 60f);
        assertEquals(new Choice(1920, 1080, 60), pick(now, DECODER_4K, now, m(1280, 720, 60f)));
    }

    @Test
    public void a4kTvStreams4kWhenItsDecoderTakesIt() {
        Mode now = m(3840, 2160, 60f);
        assertEquals(new Choice(3840, 2160, 60), pick(now, DECODER_4K, now, m(1920, 1080, 60f)));
    }

    @Test
    public void a4kTvWithA1080pDecoderStreams1080p() {
        Mode now = m(3840, 2160, 60f);
        assertEquals(new Choice(1920, 1080, 60), pick(now, DECODER_1080, now));
    }

    @Test
    public void anUnknownDecoderDoesNotCapTheMode() {
        Mode now = m(3840, 2160, 60f);
        assertEquals(new Choice(3840, 2160, 60), pick(now, null, now));
    }

    @Test
    public void ntscRatesSnapToWholeOnes() {
        assertEquals(60, pick(m(1920, 1080, 59.94f), DECODER_4K).fps);
        assertEquals(50, pick(m(1920, 1080, 50.0f), DECODER_4K).fps);
        assertEquals(120, pick(m(1920, 1080, 119.88f), DECODER_4K).fps);
    }

    @Test
    public void aTvThatMatchedAFilmsRateStreamsAtItsBestRateUpTo60() {
        Mode now = m(3840, 2160, 23.976f);
        assertEquals(new Choice(3840, 2160, 60),
                pick(now, DECODER_4K, now, m(3840, 2160, 50f), m(3840, 2160, 59.94f), m(1920, 1080, 120f)));
        assertEquals(50, pick(now, DECODER_4K, now, m(3840, 2160, 50f)).fps);
    }

    @Test
    public void aTvWithNoUsableRateStreamsAt60() {
        assertEquals(60, pick(m(1920, 1080, 0f), DECODER_4K).fps);
        assertEquals(60, pick(m(1920, 1080, Float.NaN), DECODER_4K).fps);
        assertEquals(60, pick(m(1920, 1080, -1f), DECODER_4K).fps);
    }

    @Test
    public void veryHighRatesAreCappedAt120() {
        assertEquals(120, pick(m(1920, 1080, 144f), DECODER_4K).fps);
    }

    @Test
    public void oddSizesSnapDownToTheNearestStandardOne() {
        assertEquals(new Choice(1920, 1080, 60), pick(m(1920, 1088, 60f), DECODER_4K));
        assertEquals(new Choice(1280, 720, 60), pick(m(1366, 768, 60f), DECODER_4K));
        assertEquals(new Choice(3840, 2160, 60), pick(m(4096, 2160, 60f), DECODER_4K));
        assertEquals(new Choice(1920, 1080, 60), pick(m(2560, 1080, 60f), DECODER_4K));
    }

    @Test
    public void an8kTvStreamsAtMost4k() {
        assertEquals(new Choice(3840, 2160, 60), pick(m(7680, 4320, 60f), null));
    }

    @Test
    public void aModeReportedUprightIsTurnedOnItsSide() {
        assertEquals(new Choice(1920, 1080, 60), pick(m(1080, 1920, 60f), DECODER_4K));
    }

    @Test
    public void aNonsenseCurrentModeFallsBackToTheLargestSupportedOne() {
        assertEquals(new Choice(1920, 1080, 60),
                pick(m(0, 0, 60f), DECODER_4K, m(1280, 720, 60f), m(1920, 1080, 60f), m(64, 32, 60f)));
    }

    @Test
    public void noModesAtAllGive1080pAt60() {
        assertEquals(new Choice(1920, 1080, 60), pick(null, DECODER_4K));
    }

    @Test
    public void tinyModesNeverGoBelow720p() {
        assertEquals(new Choice(1280, 720, 60), pick(m(1024, 600, 60f), DECODER_4K));
    }

    @Test
    public void upstreamsDefaultMovesToMatchThisTv() {
        assertEquals(TvMode.MATCH, TvMode.migrateResolution(null));
        assertEquals(TvMode.MATCH, TvMode.migrateResolution("1280x720"));
        assertEquals(TvMode.MATCH, TvMode.migrateFps(null));
        assertEquals(TvMode.MATCH, TvMode.migrateFps("60"));
    }

    @Test
    public void aChosenModeStays() {
        assertEquals("1920x1080", TvMode.migrateResolution("1920x1080"));
        assertEquals("3840x2160", TvMode.migrateResolution("3840x2160"));
        assertEquals("30", TvMode.migrateFps("30"));
        assertEquals("120", TvMode.migrateFps("120"));
        assertEquals(TvMode.MATCH, TvMode.migrateResolution(TvMode.MATCH));
    }

    @Test
    public void theDefaultBitrateFollowsTheChosenMode() {
        assertEquals(10000, com.limelight.preferences.PreferenceConfiguration.getDefaultBitrate(1280, 720, 60));
        assertEquals(20000, com.limelight.preferences.PreferenceConfiguration.getDefaultBitrate(1920, 1080, 60));
        assertEquals(80000, com.limelight.preferences.PreferenceConfiguration.getDefaultBitrate(3840, 2160, 60));
    }
}
