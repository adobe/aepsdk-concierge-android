/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.conciergetestapp.fnb

internal object FnbXdm {
    const val NAMESPACE = "_sacramentokings"
    const val CONCIERGE_CONTEXT = "conciergeContext"
    const val ORDERS = "fnbOrders"
    const val CONCIERGE_ACTION = "conciergeAction"

    fun action(name: String, fields: Map<String, Any>): Map<String, Any> =
        mapOf(NAMESPACE to mapOf(CONCIERGE_ACTION to mapOf(name to fields)))

    fun orders(fields: Map<String, Any?>): Map<String, Any?> =
        mapOf(NAMESPACE to mapOf(CONCIERGE_CONTEXT to mapOf(ORDERS to fields)))
}
