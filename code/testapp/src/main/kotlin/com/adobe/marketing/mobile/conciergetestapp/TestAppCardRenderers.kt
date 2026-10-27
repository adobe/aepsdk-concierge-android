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

package com.adobe.marketing.mobile.conciergetestapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.fnbActionSender
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.registerFnbCardRenderers
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.parking.ParkingCardTypes
import com.adobe.marketing.mobile.conciergetestapp.parking.parkingEventsRenderer

/**
 * Registers the test app's native card renderers. Renderers are process-wide, so checkout opens
 * through the application context rather than an Activity.
 */
fun registerTestAppCardRenderers(context: Context) {
    val appContext = context.applicationContext
    val cartOptions = CartOptions.STAGE
    val openCheckout: (String) -> Boolean = { url ->
        runCatching {
            appContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
    registerFnbCardRenderers(
        menuOptions = MenuOptions(),
        cartOptions = cartOptions,
        openCheckout = openCheckout
    )
    Concierge.registerCardRenderer(
        ParkingCardTypes.EVENTS,
        parkingEventsRenderer(
            actions = fnbActionSender(ParkingCardTypes.EVENTS, cartOptions.allowedCheckoutHosts, openCheckout)
        )
    )
}
