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

package com.adobe.marketing.mobile.concierge.network

import java.io.IOException

/**
 * Thrown when the conversation service answers with a non-2xx HTTP status.
 *
 * It remains an [IOException] for existing callers, but lets them tell a response the server
 * deliberately rejected apart from a dropped or timed-out connection.
 */
internal class ConciergeHttpStatusException(
    val statusCode: Int,
    message: String
) : IOException(message)
