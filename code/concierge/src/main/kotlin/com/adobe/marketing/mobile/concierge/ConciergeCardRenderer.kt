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

import androidx.compose.runtime.Composable

/**
 * Renders one transcript card for a response element whose `type` was registered with
 * [Concierge.registerCardRenderer].
 *
 * - `element`: the complete, read-only element dictionary from `multimodalElements.elements`.
 * - `context`: read-only card context, rebuilt on every recomposition: `cardId`, `sessionId`,
 *   `conversationId`, `interactionId` (each may be absent), `isBusy`, `isStale`, and `state`
 *   (this card's last saved state, if any).
 * - `saveState`: replaces this card's private state. Callable from any thread; applied on the main
 *   thread. Never sent to Brand Concierge.
 *
 * Renderers must not throw, and must not start work during composition.
 */
typealias ConciergeCardRenderer = @Composable (
    element: Map<String, Any?>,
    context: Map<String, Any?>,
    saveState: (Map<String, Any?>) -> Unit
) -> Unit
