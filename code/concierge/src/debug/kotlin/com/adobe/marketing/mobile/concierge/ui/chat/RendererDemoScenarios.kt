/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.concierge.ui.chat

import com.adobe.marketing.mobile.concierge.cards.CardJson
import org.json.JSONObject

internal enum class DemoRoute { NATIVE, BUILT_IN, CTA, TEXT }
internal enum class DemoDelivery { DELIVERED, EMPTY_RESPONSE, DELIVERY_FAILED }

internal data class RendererDemoScenario(
    val id: String,
    val group: String,
    val title: String,
    val expectation: String,
    val routes: List<DemoRoute>,
    val delivery: DemoDelivery = if (routes.isEmpty()) DemoDelivery.EMPTY_RESPONSE else DemoDelivery.DELIVERED,
    val registrations: List<Pair<String, String>> = listOf("demo.card" to "Custom renderer"),
    val frames: () -> List<String>,
    val throwAfterFrames: Boolean = false
) {
    override fun toString() = id
}

/** Wire fixtures are built on demand; the two node-budget probes are capped at 100,001 leaves. */
internal object RendererDemoScenarios {
    private const val TYPE = "demo.card"
    private val native = listOf(DemoRoute.NATIVE)
    private val builtIn = listOf(DemoRoute.BUILT_IN)
    private val text = listOf(DemoRoute.TEXT)

    fun card(type: Any? = TYPE, id: String? = "card-1", vararg fields: Pair<String, Any?>): Map<String, Any?> =
        linkedMapOf<String, Any?>("type" to type).apply {
            id?.let { put("id", it) }
            putAll(fields)
        }

    fun product(type: Any? = "productCard", id: String = "product-1"): Map<String, Any?> = card(
        type, id,
        "entity_info" to mapOf(
            "productName" to "Offline trail shoes",
            "productDescription" to "Static fixture. No image download or checkout.",
            "productPrice" to "$49",
            "primary" to mapOf("text" to "Inspect action", "url" to "https://example.invalid/demo"),
            "secondary" to mapOf("text" to "Another action", "url" to "https://example.invalid/secondary")
        )
    )

    private fun cta() = card(
        "ctaButton", "cta-1",
        "entity_info" to mapOf("primary" to mapOf("text" to "Offline CTA", "url" to "https://example.invalid/cta"))
    )

    fun frame(
        elements: List<Any?> = emptyList(),
        message: String = "",
        state: String = "completed",
        multimodal: Any? = mapOf("elements" to elements)
    ): String = json(
        mapOf(
            "handle" to listOf(
                mapOf(
                    "type" to "brand-concierge:conversation",
                    "payload" to listOf(
                        mapOf(
                            "state" to state,
                            "conversationId" to "offline-conversation",
                            "interactionId" to "offline-interaction",
                            "response" to mapOf("message" to message, "multimodalElements" to multimodal)
                        )
                    )
                )
            )
        )
    )

    private fun json(value: Map<String, Any?>) = (CardJson.toJsonValue(value) as JSONObject).toString()

    fun deep(levels: Int, fallback: Boolean = false): Map<String, Any?> {
        var nested: Any? = "leaf"
        repeat(levels - 1) { nested = mapOf("nested" to nested) }
        return card(id = if (fallback) "fallback" else null, fields = arrayOf("data" to nested))
    }

    private fun budget(leaves: Int, fallback: Boolean = false) =
        card(id = if (fallback) "fallback" else null, fields = arrayOf("data" to List(leaves) { null }))

    private fun simple(
        id: String,
        group: String,
        title: String,
        expected: String,
        routes: List<DemoRoute>,
        elements: () -> List<Any?>
    ) = RendererDemoScenario(id, group, title, expected, routes, frames = { listOf(frame(elements())) })

    val all: List<RendererDemoScenario> = listOf(
        simple("product", "Built-in routing", "productCard", "One real OOTB product-card row; action taps are captured locally.", builtIn) { listOf(product()) },
        simple("horizontal", "Built-in routing", "horizontalCard", "One real OOTB row through the SDK built-in path, not a custom horizontal preview.", builtIn) { listOf(product("horizontalCard")) },
        simple("cta", "Built-in routing", "ctaButton", "One standalone SDK CTA; no browser is opened.", listOf(DemoRoute.CTA)) { listOf(cta()) },
        simple("untyped", "Built-in routing", "Untyped legacy card", "Missing type retains the legacy built-in card fallback.", builtIn) { listOf(product() - "type") },
        simple("registered", "Native routing", "Registered custom type", "One native row. Save state, then repeat the turn to inspect independent state.", native) { listOf(card()) },
        simple("unknown", "Native routing", "Unregistered type, with / without id", "Unknown type WITH id uses built-in fallback; without id is dropped.", builtIn) { listOf(product("demo.unknown"), card("demo.unknown", null)) },
        RendererDemoScenario(
            "override", "Native routing", "Built-in type override",
            "The isolated productCard registration takes precedence over built-in rendering.", native,
            registrations = listOf("productCard" to "Isolated product override"),
            frames = { listOf(frame(listOf(product()))) }
        ),
        RendererDemoScenario(
            "last-wins", "Native routing", "Last registration wins",
            "Only the SECOND renderer label appears. Private lookup replacement simulation; the process-wide public registration API is deliberately not called.",
            native, registrations = listOf(TYPE to "FIRST renderer", TYPE to "SECOND renderer"),
            frames = { listOf(frame(listOf(card()))) }
        ),
        simple("json", "Payload values", "Full JSON / missing id", "Native renderer receives nulls, maps, arrays, booleans, numbers, strings and unknown fields. SDK generates cardId despite missing element id.", native) {
            listOf(card(id = null, fields = arrayOf(
                "unknown" to mapOf("array" to listOf(null, true, false, 42, -7, 2.5, "", "日本語 👟"), "empty" to emptyMap<String, Any?>()),
                "entity_info" to listOf(mapOf("product" to mapOf("id" to 1364015, "extra" to null)))
            )))
        },
        simple("null", "Payload values", "Null attributes", "Native activation accepts JSON null; the DEMO renderer shows a safe placeholder, not SDK validation.", native) { listOf(card(fields = arrayOf("entity_info" to null, "title" to null))) },
        simple("missing", "Payload values", "Missing attributes", "Native activation accepts a dictionary without entity_info; demo renderer uses its placeholder.", native) { listOf(card(id = null)) },
        simple("empty", "Payload values", "Empty dictionaries / arrays / strings", "Empty JSON values survive native activation unchanged.", native) { listOf(card(fields = arrayOf("entity_info" to emptyMap<String, Any?>(), "items" to emptyList<Any?>(), "title" to ""))) },
        simple("scalar-attributes", "Payload values", "Wrong-shaped attributes: scalar", "The SDK passes a string through; schema-specific validation belongs to the renderer.", native) { listOf(card(fields = arrayOf("entity_info" to "not a dictionary"))) },
        simple("array-attributes", "Payload values", "Wrong-shaped attributes: array", "The SDK passes an array through; demo renderer displays a safe placeholder.", native) { listOf(card(fields = arrayOf("entity_info" to listOf(1, null, false)))) },
        simple("malformed-built-in", "Payload values", "Built-in fields have wrong types", "Valid id permits built-in routing; unsupported field types are omitted by the parser, not treated as a native card.", builtIn) {
            listOf(card("productCard", "wrong-fields", "entity_info" to mapOf("productName" to 7, "primary" to false, "productPrice" to listOf(1)), "width" to -1))
        },
        simple("type-values", "Payload values", "Null / numeric / boolean type", "Non-string types cannot match a renderer; three valid ids fall back to one built-in carousel row.", builtIn) {
            listOf(product(null, "null-type"), product(7, "number-type"), product(false, "bool-type"))
        },
        simple("case", "Native routing", "Type matching is case-sensitive", "DEMO.CARD does not match demo.card; id enables built-in fallback.", builtIn) { listOf(product("DEMO.CARD")) },
        RendererDemoScenario(
            "mixed", "Ordering and duplicates", "Text + card + CTA + native + card",
            "Text is first; built-in cards are grouped at their first position, then CTA, then native. All taps stay local.",
            listOf(DemoRoute.TEXT, DemoRoute.BUILT_IN, DemoRoute.CTA, DemoRoute.NATIVE),
            frames = { listOf(frame(listOf(product(), cta(), card(), product("horizontalCard", "product-2")), "Mixed offline response")) }
        ),
        simple("duplicate-fallback", "Ordering and duplicates", "Duplicate id with fallback", "First element activates natively; second reuses id at a different position and falls back to OOTB.", listOf(DemoRoute.NATIVE, DemoRoute.BUILT_IN)) { listOf(card(), card()) },
        simple("duplicate-no-fallback", "Ordering and duplicates", "Duplicate id without fallback", "ctaButton override: first activates natively; second is rejected and has no valid CTA fallback.", native) {
            listOf(card("ctaButton", "same"), card("ctaButton", "same"))
        }.copy(registrations = listOf("ctaButton" to "Isolated CTA override")),
        simple("depth64", "Bounded validation", "Depth 64: accepted boundary", "Element root plus 63 nested maps is accepted by CardJson.", native) { listOf(deep(64)) },
        simple("depth65", "Bounded validation", "Depth 65: no fallback", "Native activation rejects the over-depth payload; no id means no fallback; EMPTY_RESPONSE.", emptyList()) { listOf(deep(65)) },
        simple("depth65-fallback", "Bounded validation", "Depth 65: built-in fallback", "Native validation rejects the payload; id restores the built-in row.", builtIn) { listOf(deep(65, true)) },
        simple("budget-boundary", "Bounded validation", "100,000 nodes: accepted boundary", "Root + type + data array + 99,997 null leaves = 100,000 copied values; one native row.", native) { listOf(budget(99_997)) },
        simple("budget-over", "Bounded validation", "Node budget exceeded: no fallback", "100,001 null leaves exceed 100,000 copied values; bounded rejection and EMPTY_RESPONSE.", emptyList()) { listOf(budget(100_001)) },
        simple("budget-fallback", "Bounded validation", "Node budget exceeded: fallback", "Same bounded rejection, but a valid id restores OOTB rendering.", builtIn) { listOf(budget(100_001, true)) },
        simple("accepted-rejected", "Bounded validation", "Accepted and rejected native candidates", "Rejected over-depth candidate does not hide the accepted native card.", native) { listOf(deep(65), card(id = null)) },
        RendererDemoScenario("envelope", "Malformed wire", "Missing / null / wrong envelope", "Missing, null, empty and wrong-shaped envelopes do not emit responses; EMPTY_RESPONSE.", emptyList(), frames = {
            listOf(
                "{}", "null", """{"handle":null}""", """{"handle":[]}""", """{"handle":"wrong"}""",
                """{"handle":[{"type":"brand-concierge:conversation","payload":null}]}""",
                """{"handle":[{"type":"brand-concierge:conversation","payload":[{"state":"completed","response":null}]}]}"""
            )
        }),
        RendererDemoScenario("invalid-json", "Malformed wire", "Invalid JSON", "Parser ignores malformed JSON; no synthetic completion is appended; EMPTY_RESPONSE.", emptyList(), frames = { listOf("{broken JSON") }),
        RendererDemoScenario("multimodal-array", "Malformed wire", "Missing / null / wrong multimodal container", "Missing, null and array-shaped multimodalElements produce no rows; EMPTY_RESPONSE.", emptyList(), frames = {
            listOf(frame(multimodal = listOf(card())), frame(multimodal = null), frame(multimodal = emptyMap<String, Any?>()))
        }),
        RendererDemoScenario("elements-map", "Malformed wire", "Wrong elements container", "An object instead of elements array is ignored; EMPTY_RESPONSE.", emptyList(), frames = { listOf(frame(multimodal = mapOf("elements" to card()))) }),
        simple("non-map-elements", "Malformed wire", "Null / scalar elements mixed with a card", "A non-map entry makes typed-list parsing reject the whole elements array, including its valid card; EMPTY_RESPONSE.", emptyList()) { listOf(null, 7, "bad", card()) },
        simple("invalid-cta", "Malformed wire", "CTA null / missing / malformed actions", "Invalid CTA actions have no built-in representation; EMPTY_RESPONSE.", emptyList()) {
            listOf(card("ctaButton", null), card("ctaButton", null, "entity_info" to null), card("ctaButton", null, "entity_info" to mapOf("primary" to mapOf("text" to 7, "url" to false))))
        },
        RendererDemoScenario("no-response", "Completion and rollback", "No response frames", "Service completes without emitting anything; EMPTY_RESPONSE.", emptyList(), frames = { emptyList() }),
        RendererDemoScenario("whitespace", "Completion and rollback", "Whitespace-only completion", "Whitespace is not visible content; EMPTY_RESPONSE.", emptyList(), frames = { listOf(frame(message = " \n\t ")) }),
        RendererDemoScenario("final-text", "Completion and rollback", "Final-only text survives native rejection", "Rejected native candidate cannot discard final response text; delivered text row.", text, frames = { listOf(frame(listOf(deep(65)), "Final text retained")) }),
        RendererDemoScenario("stream-text", "Completion and rollback", "Streamed text survives native rejection", "Previously streamed text remains when the completion contains only a rejected candidate.", text, frames = {
            listOf(frame(message = "Streamed text retained", state = "in-progress"), frame(listOf(deep(65))))
        }),
        RendererDemoScenario("stream-error", "Completion and rollback", "ERROR after visible content", "Real VM rolls back partial content; only the local handoff note remains; DELIVERY_FAILED.", emptyList(), DemoDelivery.DELIVERY_FAILED, frames = {
            listOf(frame(message = "This partial text must be removed", state = "in-progress"), frame(message = "synthetic error", state = "error"))
        }),
        RendererDemoScenario("io-error", "Completion and rollback", "IOException after visible content", "Real VM rolls back partial content and reports DELIVERY_FAILED; no network call occurs.", emptyList(), DemoDelivery.DELIVERY_FAILED, frames = {
            listOf(frame(message = "This partial text must be removed", state = "in-progress"))
        }, throwAfterFrames = true),
        simple("lifecycle", "Local lifecycle", "Repeat / save state / stale / unavailable", "Save state, Repeat to get a new cardId and independent state. Mark stale then discard controller to show SDK unavailable row. Local simulation only; no identity event.", native) { listOf(card()) }
    )
}
