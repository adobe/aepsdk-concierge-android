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

package com.adobe.marketing.mobile.concierge.utils

/**
 * Projects a card element's raw content map down to the whitelisted product fields used for
 * `CardClicked` / `CardsRendered` tracking. Unknown keys are dropped; absent keys are omitted.
 */
internal fun buildCardElementDict(content: Map<String, Any>): Map<String, Any> {
    val dict = mutableMapOf<String, Any>()
    content["productName"]?.let { dict["productName"] = it }
    content["productDescription"]?.let { dict["productDescription"] = it }
    content["productPageURL"]?.let { dict["productPageURL"] = it }
    content["productPrice"]?.let { dict["productPrice"] = it }
    content["productBadge"]?.let { dict["productBadge"] = it }
    return dict
}
