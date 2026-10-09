/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.ui.stt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
