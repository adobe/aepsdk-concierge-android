/*
 * Copyright 2025 Adobe. All rights reserved.
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

import android.app.Application
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap

class ChatApp : Application() {
    companion object {
        private const val LOG_TAG = "ChatApp"
        private const val APP_ID = "/staging/a0ff8cfcfb87/9d363ab429c8/launch-481f6ff16749-development"
    }

    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.initialize(this, APP_ID) {
            Concierge.setEdgeTrackingEnabled(true)
            val identityMap = IdentityMap().apply {
                addItem(
                    IdentityItem("22e8b663dd7d0ca003fea59bd163d97bf2f4c84538bfb3875b9a86b30aefc9cd"),
                    "Email_LC_SHA256"
                )
            }
            Identity.updateIdentities(identityMap)
        }
        Concierge.updateXDMContext(
            mapOf(
                "_sacramentokings" to mapOf(
                    "conciergeContext" to mapOf(
                        "userContext" to mapOf(
                            "loggedIn" to true,
                            "hasAssignedSeat" to true,
                            "seatSection" to "114",
                            "isCourtside" to false,
                            "email" to "visampath+Diego@adobe.com",
                            "nbaid" to "abc123",
                            "tmid" to "rvt122"
                        ),
                        "currentGame" to game(
                            "2026-04-14T23:30:00.000Z", "Finished", "2025-26", "PlayIn",
                            home = "Hornets" to "CHA", away = "Heat" to "MIA"
                        ),
                        "upcomingGame" to game(
                            "2027-04-11T22:00:00.000Z", "Scheduled", "2026-27", "Regular Season",
                            home = "Heat" to "MIA", away = "Magic" to "ORL"
                        )
                    )
                )
            )
        )
    }

    private fun game(
        dateTimeUtc: String,
        status: String,
        seasonCode: String,
        seasonType: String,
        home: Pair<String, String>,
        away: Pair<String, String>
    ): Map<String, Any> = mapOf(
        "gameDateTimeUtc" to dateTimeUtc,
        "gameState" to "active",
        "gameStatus" to status,
        "seasonCode" to seasonCode,
        "seasonType" to seasonType,
        "homeTeam" to mapOf("name" to home.first, "teamAbbreviation" to home.second),
        "awayTeam" to mapOf("name" to away.first, "teamAbbreviation" to away.second)
    )
}
