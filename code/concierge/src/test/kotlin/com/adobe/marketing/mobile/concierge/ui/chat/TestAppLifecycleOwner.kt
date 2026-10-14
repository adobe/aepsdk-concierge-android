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

package com.adobe.marketing.mobile.concierge.ui.chat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Stands in for ProcessLifecycleOwner in unit tests. Starts in the foreground (STARTED). Holds
 * itself strongly, since LifecycleRegistry only keeps a weak reference to its owner.
 */
internal class TestAppLifecycleOwner : LifecycleOwner {
    override val lifecycle: LifecycleRegistry = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.STARTED
    }

    /** Dispatches ON_STOP, as when the whole app leaves the screen. */
    fun moveToBackground() {
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    /** Dispatches ON_START, as when the app returns to the screen. */
    fun moveToForeground() {
        lifecycle.currentState = Lifecycle.State.STARTED
    }
}
