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

package com.adobe.marketing.mobile.concierge

/**
 * Supplies the app-minted authentication token that the Brand Concierge SDK attaches to each
 * conversation turn.
 *
 * Register an implementation with [Concierge.setAuthTokenProvider].
 */
fun interface ConciergeAuthTokenProvider {

    /**
     * Called immediately before building each turn's request (both chat and feedback).
     * Return the current opaque, app-minted token, or null to send the turn without one.
     *
     * The app owns minting and refresh.
     *
     * This is invoked on a background thread and may block briefly to refresh the token; the SDK
     * bounds the wait (see [Concierge.setAuthTokenProvider]).
     */
    fun provideToken(): String?
}
