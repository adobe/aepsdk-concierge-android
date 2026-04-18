/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests visibility defaults and overrides for the feedback close (X) and Cancel buttons.
 * Mirrors the iOS `ConciergeFeedbackBehaviorTests` coverage.
 */
class ConciergeFeedbackBehaviorTest {

    // ========== Default resolution by displayMode ==========

    @Test
    fun `defaults modal close hidden cancel shown`() {
        val behavior = ConciergeFeedbackBehavior(displayMode = FeedbackDisplayMode.MODAL)

        assertFalse(behavior.resolvedShowCloseButton())
        assertTrue(behavior.resolvedShowCancelButton())
    }

    @Test
    fun `defaults action close shown cancel hidden`() {
        val behavior = ConciergeFeedbackBehavior(displayMode = FeedbackDisplayMode.ACTION)

        assertTrue(behavior.resolvedShowCloseButton())
        assertFalse(behavior.resolvedShowCancelButton())
    }

    // ========== Explicit overrides ==========

    @Test
    fun `explicit overrides modal close true cancel false honored`() {
        val behavior = ConciergeFeedbackBehavior(
            displayMode = FeedbackDisplayMode.MODAL,
            showCloseButton = true,
            showCancelButton = false
        )

        assertTrue(behavior.resolvedShowCloseButton())
        assertFalse(behavior.resolvedShowCancelButton())
    }

    @Test
    fun `explicit overrides action close false cancel true honored`() {
        val behavior = ConciergeFeedbackBehavior(
            displayMode = FeedbackDisplayMode.ACTION,
            showCloseButton = false,
            showCancelButton = true
        )

        assertFalse(behavior.resolvedShowCloseButton())
        assertTrue(behavior.resolvedShowCancelButton())
    }

    /**
     * Both `false` is respected -- neither button is shown. Submit always exits;
     * action mode also allows drag-down.
     */
    @Test
    fun `explicit overrides both false honored without auto flipping`() {
        for (displayMode in listOf(FeedbackDisplayMode.MODAL, FeedbackDisplayMode.ACTION)) {
            val behavior = ConciergeFeedbackBehavior(
                displayMode = displayMode,
                showCloseButton = false,
                showCancelButton = false
            )

            assertFalse(
                "displayMode=$displayMode should honor showCloseButton=false without auto-flipping",
                behavior.resolvedShowCloseButton()
            )
            assertFalse(
                "displayMode=$displayMode should honor showCancelButton=false without auto-flipping",
                behavior.resolvedShowCancelButton()
            )
        }
    }

    @Test
    fun `default nullable overrides remain null`() {
        val behavior = ConciergeFeedbackBehavior()

        assertNull(behavior.showCloseButton)
        assertNull(behavior.showCancelButton)
    }

    // ========== ConciergeFeedbackComponent per-sentiment flags ==========

    @Test
    fun `ConciergeFeedbackComponent defaults both sentiments to enabled`() {
        val component = ConciergeFeedbackComponent()

        assertTrue(component.positiveNotesEnabled)
        assertTrue(component.negativeNotesEnabled)
    }

    @Test
    fun `ConciergeFeedbackComponent supports disabling per sentiment`() {
        val positiveOnly = ConciergeFeedbackComponent(
            positiveNotesEnabled = true,
            negativeNotesEnabled = false
        )
        val negativeOnly = ConciergeFeedbackComponent(
            positiveNotesEnabled = false,
            negativeNotesEnabled = true
        )

        assertTrue(positiveOnly.positiveNotesEnabled)
        assertFalse(positiveOnly.negativeNotesEnabled)
        assertFalse(negativeOnly.positiveNotesEnabled)
        assertTrue(negativeOnly.negativeNotesEnabled)
    }

    @Test
    fun `copy preserves feedback behavior overrides`() {
        val original = ConciergeFeedbackBehavior(
            displayMode = FeedbackDisplayMode.ACTION,
            showCloseButton = false,
            showCancelButton = true
        )
        val updated = original.copy(displayMode = FeedbackDisplayMode.MODAL)

        assertEquals(FeedbackDisplayMode.MODAL, updated.displayMode)
        assertFalse(updated.showCloseButton!!)
        assertTrue(updated.showCancelButton!!)
    }
}
