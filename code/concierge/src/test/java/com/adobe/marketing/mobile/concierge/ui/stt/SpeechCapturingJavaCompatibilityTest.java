/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package com.adobe.marketing.mobile.concierge.ui.stt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpeechCapturingJavaCompatibilityTest {
    @Test
    public void legacyJavaImplementationCompilesAndInvokesOriginalContract() {
        LegacySpeechCapturing legacy = new LegacySpeechCapturing();
        SpeechCapturing capture = legacy;

        assertTrue(capture.isAvailable());
        capture.setListener(null);
        capture.startCapture();
        capture.endCapture();
        capture.release();

        assertEquals(1, legacy.started);
        assertEquals(1, legacy.stopped);
        assertEquals(1, legacy.released);
    }

    private static final class LegacySpeechCapturing implements SpeechCapturing {
        private int started;
        private int stopped;
        private int released;

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void setListener(SpeechCaptureListener listener) {}

        @Override
        public void startCapture() {
            started++;
        }

        @Override
        public void endCapture() {
            stopped++;
        }

        @Override
        public void release() {
            released++;
        }
    }
}
