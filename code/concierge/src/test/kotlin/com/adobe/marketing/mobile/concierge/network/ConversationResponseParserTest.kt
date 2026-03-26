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

package com.adobe.marketing.mobile.concierge.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationResponseParserTest {

    @Test
    fun `parseConversationData returns empty list for blank input`() {
        val result = ConversationResponseParser.parseConversationData("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData returns empty list for whitespace input`() {
        val result = ConversationResponseParser.parseConversationData("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData returns empty list for invalid JSON`() {
        val result = ConversationResponseParser.parseConversationData("not valid json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData returns empty list for empty JSON object`() {
        val result = ConversationResponseParser.parseConversationData("{}")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData returns empty list for missing handle field`() {
        val json = """{"someField": "someValue"}"""
        val result = ConversationResponseParser.parseConversationData(json)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData parses simple message`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Hello, how can I help?"
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals("Hello, how can I help?", result[0].messageContent)
        assertEquals(ConversationState.IN_PROGRESS, result[0].state)
    }

    @Test
    fun `parseConversationData parses message with conversationId and interactionId`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "conversationId": "conv-123",
                      "interactionId": "inter-456",
                      "response": {
                        "message": "Test message"
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals("Test message", result[0].messageContent)
        assertEquals("conv-123", result[0].conversationId)
        assertEquals("inter-456", result[0].interactionId)
        assertEquals(ConversationState.COMPLETED, result[0].state)
    }

    @Test
    fun `parseConversationData parses message with prompt suggestions`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "What would you like to know?",
                        "promptSuggestions": ["Tell me more", "How does this work?", "Show me examples"]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(3, result[0].promptSuggestions.size)
        assertEquals("Tell me more", result[0].promptSuggestions[0])
        assertEquals("How does this work?", result[0].promptSuggestions[1])
        assertEquals("Show me examples", result[0].promptSuggestions[2])
    }

    @Test
    fun `parseConversationData handles empty prompt suggestions array`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "promptSuggestions": []
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].promptSuggestions.isEmpty())
    }

    @Test
    fun `parseConversationData handles multiple messages in payload`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": "First message"},
                      "state": "in-progress"
                    },
                    {
                      "response": {"message": "Second message"},
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(2, result.size)
        assertEquals("First message", result[0].messageContent)
        assertEquals("Second message", result[1].messageContent)
    }

    @Test
    fun `parseConversationData ignores non-conversation type handles`() {
        val json = """
            {
              "handle": [
                {
                  "type": "other:type",
                  "payload": [
                    {
                      "response": {"message": "Should be ignored"},
                      "state": "in-progress"
                    }
                  ]
                },
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": "Should be included"},
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals("Should be included", result[0].messageContent)
    }

    @Test
    fun `parseConversationData handles missing response field`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData handles empty message for non-completed state`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": ""},
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseConversationData allows empty message for completed state`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": ""},
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals("", result[0].messageContent)
        assertEquals(ConversationState.COMPLETED, result[0].state)
    }

    @Test
    fun `parseConversationData parses ConversationState correctly`() {
        val testCases = listOf(
            "in-progress" to ConversationState.IN_PROGRESS,
            "completed" to ConversationState.COMPLETED,
            "error" to ConversationState.ERROR,
            "unknown-state" to ConversationState.UNKNOWN,
            "" to ConversationState.UNKNOWN
        )

        testCases.forEach { (stateString, expectedState) ->
            val json = """
                {
                  "handle": [
                    {
                      "type": "brand-concierge:conversation",
                      "payload": [
                        {
                          "response": {"message": "Test"},
                          "state": "$stateString"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val result = ConversationResponseParser.parseConversationData(json)
            assertEquals("Failed for state: $stateString", 1, result.size)
            assertEquals("Failed for state: $stateString", expectedState, result[0].state)
        }
    }

    @Test
    fun `parseConversationData parses multimodal elements with product card`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Check out this product",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "width": 400,
                              "height": 300,
                              "thumbnail_width": 100,
                              "thumbnail_height": 75,
                              "entity_info": {
                                "productName": "Amazing Widget",
                                "productDescription": "Best widget ever",
                                "description": "Product details",
                                "productPageURL": "https://example.com/product",
                                "productImageURL": "https://example.com/image.jpg",
                                "backgroundColor": "#FF0000",
                                "learningResource": "https://example.com/learn",
                                "logo": "https://example.com/logo.png"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)

        val element = result[0].multimodalElements[0]
        assertEquals("product-1", element.id)
        assertEquals("Amazing Widget", element.title)
        assertEquals("Best widget ever", element.transcript)
        assertEquals("Product details", element.caption)
        assertEquals("https://example.com/image.jpg", element.url)
        assertEquals(400, element.width)
        assertEquals(300, element.height)
        assertEquals(100, element.thumbnailWidth)
        assertEquals(75, element.thumbnailHeight)

        // Check content map
        assertEquals("Amazing Widget", element.content["productName"])
        assertEquals("Best widget ever", element.content["productDescription"])
        assertEquals("Product details", element.content["description"])
        assertEquals("https://example.com/product", element.content["productPageURL"])
        assertEquals("https://example.com/image.jpg", element.content["productImageURL"])
        assertEquals("#FF0000", element.content["backgroundColor"])
        assertEquals("https://example.com/learn", element.content["learningResource"])
        assertEquals("https://example.com/logo.png", element.content["logo"])
    }

    @Test
    fun `parseConversationData parses multimodal elements with action buttons`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Product with actions",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-2",
                              "entity_info": {
                                "productName": "Test Product",
                                "productImageURL": "https://example.com/image.jpg",
                                "primary": {
                                  "text": "Buy Now",
                                  "url": "https://example.com/buy"
                                },
                                "secondary": {
                                  "text": "Learn More",
                                  "url": "https://example.com/learn"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)

        val element = result[0].multimodalElements[0]
        assertEquals("Buy Now", element.content["primaryText"])
        assertEquals("https://example.com/buy", element.content["primaryUrl"])
        assertEquals("Learn More", element.content["secondaryText"])
        assertEquals("https://example.com/learn", element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles multimodal elements with missing fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Minimal element",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "minimal-1",
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)

        val element = result[0].multimodalElements[0]
        assertEquals("minimal-1", element.id)
        assertNull(element.url)
        assertNull(element.title)
        assertNull(element.width)
        assertNull(element.height)
    }

    @Test
    fun `parseConversationData handles multimodal element missing id`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Invalid element",
                        "multimodalElements": {
                          "elements": [
                            {
                              "entity_info": {
                                "productName": "No ID Product"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(0, result[0].multimodalElements.size)
    }

    @Test
    fun `parseConversationData parses multimodal element from root when entity_info absent`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Element without entity_info",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "productName": "Flat structure product",
                              "productImageURL": "https://example.com/image.jpg",
                              "primary": {
                                "text": "Buy",
                                "url": "https://example.com/buy"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)
        val element = result[0].multimodalElements[0]
        assertEquals("product-1", element.id)
        assertEquals("Flat structure product", element.title)
        assertEquals("https://example.com/image.jpg", element.url)
        assertEquals("Buy", element.content["primaryText"])
        assertEquals("https://example.com/buy", element.content["primaryUrl"])
    }

    @Test
    fun `parseConversationData falls back to root when entity_info field is empty`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Partial entity_info",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "productName": "From root",
                              "productImageURL": "https://root.com/img.jpg",
                              "entity_info": {
                                "productName": "From entity_info",
                                "productDescription": "Entity description only"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("From entity_info", element.title)
        assertEquals("Entity description only", element.content["productDescription"])
        assertEquals("https://root.com/img.jpg", element.url)
    }

    @Test
    fun `parseConversationData prefers entity_info over root when both have value`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Both sources",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "productName": "Root name",
                              "productImageURL": "https://root.com/img.jpg",
                              "entity_info": {
                                "productName": "Entity name",
                                "productImageURL": "https://entity.com/img.jpg"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Entity name", element.title)
        assertEquals("https://entity.com/img.jpg", element.url)
    }

    @Test
    fun `parseConversationData handles multiple multimodal elements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Multiple products",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "entity_info": { "productName": "Product 1" }
                            },
                            {
                              "id": "product-2",
                              "entity_info": { "productName": "Product 2" }
                            },
                            {
                              "id": "product-3",
                              "entity_info": { "productName": "Product 3" }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(3, result[0].multimodalElements.size)
        assertEquals("Product 1", result[0].multimodalElements[0].title)
        assertEquals("Product 2", result[0].multimodalElements[1].title)
        assertEquals("Product 3", result[0].multimodalElements[2].title)
    }

    @Test
    fun `parseConversationData handles missing multimodalElements field`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "No multimodal"
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].multimodalElements.isEmpty())
    }

    @Test
    fun `parseConversationData handles missing elements array in multimodalElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Empty multimodal",
                        "multimodalElements": {}
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].multimodalElements.isEmpty())
    }

    @Test
    fun `parseConversationData parses sources with all fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Here is information from sources [1][2]",
                        "sources": [
                          {
                            "title": "Documentation",
                            "url": "https://docs.example.com",
                            "citation_number": 1,
                            "start_index": 35,
                            "end_index": 38
                          },
                          {
                            "title": "Tutorial",
                            "url": "https://tutorial.example.com",
                            "citation_number": 2,
                            "start_index": 38,
                            "end_index": 41
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(2, result[0].sources.size)

        val source1 = result[0].sources[0]
        assertEquals("Documentation", source1.title)
        assertEquals("https://docs.example.com", source1.url)
        assertEquals(1, source1.citationNumber)
        assertEquals(35, source1.startIndex)
        assertEquals(38, source1.endIndex)

        val source2 = result[0].sources[1]
        assertEquals("Tutorial", source2.title)
        assertEquals("https://tutorial.example.com", source2.url)
        assertEquals(2, source2.citationNumber)
        assertEquals(38, source2.startIndex)
        assertEquals(41, source2.endIndex)
    }

    @Test
    fun `parseConversationData parses sources with minimal fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Simple source",
                        "sources": [
                          {
                            "title": "Reference",
                            "url": "https://ref.example.com"
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].sources.size)

        val source = result[0].sources[0]
        assertEquals("Reference", source.title)
        assertEquals("https://ref.example.com", source.url)
        assertNull(source.citationNumber)
        assertNull(source.startIndex)
        assertNull(source.endIndex)
    }

    @Test
    fun `parseConversationData ignores source missing title`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Invalid source",
                        "sources": [
                          {
                            "url": "https://example.com"
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `parseConversationData ignores source missing url`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Invalid source",
                        "sources": [
                          {
                            "title": "Reference"
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `parseConversationData handles empty sources array`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "No sources",
                        "sources": []
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `parseConversationData handles missing sources field`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "No sources field"
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `parseConversationData handles complex complete message`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "conversationId": "conv-789",
                      "interactionId": "inter-012",
                      "response": {
                        "message": "Here are some products based on your query [1]",
                        "promptSuggestions": ["Show more", "Compare prices"],
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "prod-1",
                              "entity_info": {
                                "productName": "Widget Pro",
                                "productImageURL": "https://example.com/widget.jpg",
                                "primary": {
                                  "text": "View",
                                  "url": "https://example.com/view"
                                }
                              }
                            }
                          ]
                        },
                        "sources": [
                          {
                            "title": "Product Catalog",
                            "url": "https://catalog.example.com",
                            "citation_number": 1
                          }
                        ]
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)

        val message = result[0]
        assertEquals("Here are some products based on your query [1]", message.messageContent)
        assertEquals(ConversationState.COMPLETED, message.state)
        assertEquals("conv-789", message.conversationId)
        assertEquals("inter-012", message.interactionId)
        assertEquals(2, message.promptSuggestions.size)
        assertEquals(1, message.multimodalElements.size)
        assertEquals(1, message.sources.size)
    }

    @Test
    fun `parseConversationData handles negative dimension values`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "test-1",
                              "width": -100,
                              "height": 0,
                              "thumbnail_width": -1,
                              "thumbnail_height": 0,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)

        val element = result[0].multimodalElements[0]
        assertNull(element.width)
        assertNull(element.height)
        assertNull(element.thumbnailWidth)
        assertNull(element.thumbnailHeight)
    }

    @Test
    fun `parseConversationData handles invalid citation_number`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "sources": [
                          {
                            "title": "Source",
                            "url": "https://example.com",
                            "citation_number": -1
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].sources.size)
        assertNull(result[0].sources[0].citationNumber)
    }

    @Test
    fun `parseConversationData handles zero citation_number`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "sources": [
                          {
                            "title": "Source",
                            "url": "https://example.com",
                            "citation_number": 0
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].sources.size)
        assertNull(result[0].sources[0].citationNumber)
    }

    @Test
    fun `parseConversationData handles source with partial optional fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "sources": [
                          {
                            "title": "Valid Source 1",
                            "url": "https://example1.com",
                            "start_index": 10,
                            "end_index": 20
                          },
                          {
                            "title": "Valid Source 2",
                            "url": "https://example2.com",
                            "citation_number": 5
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(2, result[0].sources.size)

        val source1 = result[0].sources[0]
        assertNull(source1.citationNumber)
        assertNotNull(source1.startIndex)
        assertNotNull(source1.endIndex)

        val source2 = result[0].sources[1]
        assertNotNull(source2.citationNumber)
        assertNull(source2.startIndex)
        assertNull(source2.endIndex)
    }

    // ========== Additional Multimodal Element Edge Cases ==========

    @Test
    fun `parseConversationData handles element with empty string id`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "",
                              "entity_info": { "productName": "Should be ignored" }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(0, result[0].multimodalElements.size)
    }

    @Test
    fun `parseConversationData handles action buttons with only text no url`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Click Here"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Click Here", element.content["primaryText"])
        assertNull(element.content["primaryUrl"])
    }

    @Test
    fun `parseConversationData handles action buttons with only url no text`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "secondary": {
                                  "url": "https://example.com"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertNull(element.content["secondaryText"])
        assertEquals("https://example.com", element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles action buttons with empty strings`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "primary": {
                                  "text": "",
                                  "url": ""
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertNull(element.content["primaryText"])
        assertNull(element.content["primaryUrl"])
    }

    @Test
    fun `parseConversationData handles dimensions with value of 1`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "width": 1,
                              "height": 1,
                              "thumbnail_width": 1,
                              "thumbnail_height": 1,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals(1, element.width)
        assertEquals(1, element.height)
        assertEquals(1, element.thumbnailWidth)
        assertEquals(1, element.thumbnailHeight)
    }

    @Test
    fun `parseConversationData handles dimensions with exactly zero`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "width": 0,
                              "height": 0,
                              "thumbnail_width": 0,
                              "thumbnail_height": 0,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertNull(element.width)
        assertNull(element.height)
        assertNull(element.thumbnailWidth)
        assertNull(element.thumbnailHeight)
    }

    @Test
    fun `parseConversationData handles very large dimension values`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "width": 2147483647,
                              "height": 2147483647,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals(Int.MAX_VALUE, element.width)
        assertEquals(Int.MAX_VALUE, element.height)
    }

    @Test
    fun `parseConversationData handles element with only some product fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "productName": "Product",
                                "backgroundColor": "#FF0000"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Product", element.content["productName"])
        assertEquals("#FF0000", element.content["backgroundColor"])
        assertNull(element.content["productDescription"])
        assertNull(element.content["productPageURL"])
    }

    @Test
    fun `parseConversationData handles element with only dimensions no product fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "width": 800,
                              "height": 600,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals(800, element.width)
        assertEquals(600, element.height)
        assertNull(element.title)
        assertNull(element.url)
        assertTrue(element.content.isEmpty())
    }

    @Test
    fun `parseConversationData handles both primary and secondary buttons with mixed fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Buy Now",
                                  "url": "https://buy.example.com"
                                },
                                "secondary": {
                                  "text": "Learn More"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Buy Now", element.content["primaryText"])
        assertEquals("https://buy.example.com", element.content["primaryUrl"])
        assertEquals("Learn More", element.content["secondaryText"])
        assertNull(element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles element with only secondary button`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "secondary": {
                                  "text": "Secondary Action",
                                  "url": "https://secondary.example.com"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertNull(element.content["primaryText"])
        assertNull(element.content["primaryUrl"])
        assertEquals("Secondary Action", element.content["secondaryText"])
        assertEquals("https://secondary.example.com", element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles element with all product fields present`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Complete element",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "complete-1",
                              "width": 1920,
                              "height": 1080,
                              "thumbnail_width": 320,
                              "thumbnail_height": 240,
                              "entity_info": {
                                "productName": "Complete Product",
                                "productDescription": "Full Description",
                                "description": "Caption Text",
                                "productPageURL": "https://page.example.com",
                                "productImageURL": "https://image.example.com/img.jpg",
                                "backgroundColor": "#0000FF",
                                "learningResource": "https://learn.example.com",
                                "logo": "https://logo.example.com/logo.png",
                                "primary": {
                                  "text": "Primary",
                                  "url": "https://primary.example.com"
                                },
                                "secondary": {
                                  "text": "Secondary",
                                  "url": "https://secondary.example.com"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]

        // Check all fields are populated
        assertEquals("complete-1", element.id)
        assertEquals("https://image.example.com/img.jpg", element.url)
        assertEquals(1920, element.width)
        assertEquals(1080, element.height)
        assertEquals(320, element.thumbnailWidth)
        assertEquals(240, element.thumbnailHeight)
        assertEquals("Complete Product", element.alttext)
        assertEquals("Complete Product", element.title)
        assertEquals("Caption Text", element.caption)
        assertEquals("Full Description", element.transcript)

        // Check content map
        assertEquals(12, element.content.size)
        assertEquals("Complete Product", element.content["productName"])
        assertEquals("Full Description", element.content["productDescription"])
        assertEquals("Caption Text", element.content["description"])
        assertEquals("https://page.example.com", element.content["productPageURL"])
        assertEquals("https://image.example.com/img.jpg", element.content["productImageURL"])
        assertEquals("#0000FF", element.content["backgroundColor"])
        assertEquals("https://learn.example.com", element.content["learningResource"])
        assertEquals("https://logo.example.com/logo.png", element.content["logo"])
        assertEquals("Primary", element.content["primaryText"])
        assertEquals("https://primary.example.com", element.content["primaryUrl"])
        assertEquals("Secondary", element.content["secondaryText"])
        assertEquals("https://secondary.example.com", element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles element with mixed valid and zero dimensions`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "width": 800,
                              "height": 0,
                              "thumbnail_width": 0,
                              "thumbnail_height": 100,
                              "entity_info": {}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals(800, element.width)
        assertNull(element.height)
        assertNull(element.thumbnailWidth)
        assertEquals(100, element.thumbnailHeight)
    }

    @Test
    fun `parseConversationData handles element with empty action button objects`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "primary": {},
                                "secondary": {}
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertNull(element.content["primaryText"])
        assertNull(element.content["primaryUrl"])
        assertNull(element.content["secondaryText"])
        assertNull(element.content["secondaryUrl"])
    }

    @Test
    fun `parseConversationData handles element with special characters in product fields`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Test",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "productName": "Product \"Special\" Name",
                                "productDescription": "Description with 'quotes' and <html>",
                                "backgroundColor": "#FF00FF"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Product \"Special\" Name", element.title)
        assertEquals("Description with 'quotes' and <html>", element.transcript)
        assertEquals("#FF00FF", element.content["backgroundColor"])
    }

    @Test
    fun `parseConversationData parses details from entity_info`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Product with details",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "elem-1",
                              "entity_info": {
                                "productName": "Product",
                                "details": "Additional details from EntityInfo"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("Additional details from EntityInfo", element.content["details"])
    }

    @Test
    fun `parseConversationData handles multimodalElements as array format`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Intermediate response",
                        "multimodalElements": []
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].multimodalElements.isEmpty())
    }

    @Test
    fun `parseConversationData continues when handle has missing payload`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation"
                },
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": "From second handle"},
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals("From second handle", result[0].messageContent)
    }

    @Test
    fun `parseConversationData handles multimodalElements as non-object non-array`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Invalid multimodal type",
                        "multimodalElements": "invalid"
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].multimodalElements.isEmpty())
    }

    @Test
    fun `parseConversationData parses productPrice and productWasPrice from entity_info`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Product with pricing",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "prod-1",
                              "entity_info": {
                                "productName": "Widget",
                                "productPrice": "29.99",
                                "productWasPrice": "39.99"
                              }
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val element = result[0].multimodalElements[0]
        assertEquals("29.99", element.content["productPrice"])
        assertEquals("39.99", element.content["productWasPrice"])
    }

    @Test
    fun `parseConversationData parses mixed valid and invalid multimodal elements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Mixed elements",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "valid-1",
                              "entity_info": {"productName": "Valid Product"}
                            },
                            {
                              "entity_info": {"productName": "No ID"}
                            },
                            {
                              "id": "valid-2",
                              "entity_info": {"productName": "Another Valid"}
                            }
                          ]
                        }
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(2, result[0].multimodalElements.size)
        assertEquals("Valid Product", result[0].multimodalElements[0].title)
        assertEquals("Another Valid", result[0].multimodalElements[1].title)
    }

    @Test
    fun `parseConversationData filters empty strings from prompt suggestions`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Suggestions",
                        "promptSuggestions": ["First", "", "Second", "", "Third"]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(listOf("First", "Second", "Third"), result[0].promptSuggestions)
    }

    @Test
    fun `parseConversationData filters negative citation indices`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Cited",
                        "sources": [
                          {
                            "title": "Doc",
                            "url": "https://example.com",
                            "citation_number": 1,
                            "start_index": -1,
                            "end_index": -1
                          }
                        ]
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val source = result[0].sources[0]
        assertEquals(1, source.citationNumber)
        assertNull(source.startIndex)
        assertNull(source.endIndex)
    }

    // ========== Ordered Elements / CTA Button Tests ==========

    @Test
    fun `parseConversationData parses CTA into orderedElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Need help?",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat now",
                                  "url": "https://www.example.com/live-chat"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].orderedElements.size)
        val cta = result[0].orderedElements[0] as ParsedMultimodalItem.Cta
        assertEquals("Chat now", cta.button.label)
        assertEquals("https://www.example.com/live-chat", cta.button.url)
    }

    @Test
    fun `parseConversationData CTA is excluded from multimodalElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Here is a product and a CTA",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "product-1",
                              "entity_info": {
                                "productName": "My Product",
                                "productImageURL": "https://example.com/img.jpg"
                              }
                            },
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat now",
                                  "url": "https://example.com/chat"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].multimodalElements.size)
        assertEquals("product-1", result[0].multimodalElements[0].id)
        assertEquals(2, result[0].orderedElements.size)
        assertTrue(result[0].orderedElements[0] is ParsedMultimodalItem.Card)
        assertTrue(result[0].orderedElements[1] is ParsedMultimodalItem.Cta)
    }

    @Test
    fun `parseConversationData orderedElements empty when no multimodalElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Regular response"
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].orderedElements.isEmpty())
    }

    @Test
    fun `parseConversationData orderedElements empty when multimodalElements is array format`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Intermediate chunk",
                        "multimodalElements": []
                      },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].orderedElements.isEmpty())
    }

    @Test
    fun `parseConversationData ignores CTA element with missing label`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "CTA missing label",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": {
                                "primary": {
                                  "url": "https://example.com/chat"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].orderedElements.isEmpty())
    }

    @Test
    fun `parseConversationData ignores CTA element with missing url`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "CTA missing url",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat now"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].orderedElements.isEmpty())
    }

    @Test
    fun `parseConversationData preserves interleaved order of CTAs and cards`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Interleaved",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": { "primary": { "text": "Shop All", "url": "https://example.com/shop" } }
                            },
                            {
                              "id": "card-1",
                              "entity_info": { "productName": "Product A" }
                            },
                            {
                              "type": "ctaButton",
                              "id": "cta-2",
                              "entity_info": { "primary": { "text": "Learn More", "url": "https://example.com/learn" } }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val elements = result[0].orderedElements
        assertEquals(3, elements.size)
        assertTrue(elements[0] is ParsedMultimodalItem.Cta)
        assertEquals("Shop All", (elements[0] as ParsedMultimodalItem.Cta).button.label)
        assertTrue(elements[1] is ParsedMultimodalItem.Card)
        assertTrue(elements[2] is ParsedMultimodalItem.Cta)
        assertEquals("Learn More", (elements[2] as ParsedMultimodalItem.Cta).button.label)
    }

    @Test
    fun `parseConversationData parses multiple CTAs as separate Cta items`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Multiple CTAs",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": { "primary": { "text": "First", "url": "https://example.com/first" } }
                            },
                            {
                              "type": "ctaButton",
                              "id": "cta-2",
                              "entity_info": { "primary": { "text": "Second", "url": "https://example.com/second" } }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val elements = result[0].orderedElements
        assertEquals(2, elements.size)
        assertEquals("First", (elements[0] as ParsedMultimodalItem.Cta).button.label)
        assertEquals("Second", (elements[1] as ParsedMultimodalItem.Cta).button.label)
    }

    @Test
    fun `parseConversationData cards-only produces Card items in orderedElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Cards only",
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "card-1",
                              "entity_info": { "productName": "Product A" }
                            },
                            {
                              "id": "card-2",
                              "entity_info": { "productName": "Product B" }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val elements = result[0].orderedElements
        assertEquals(2, elements.size)
        assertTrue(elements[0] is ParsedMultimodalItem.Card)
        assertTrue(elements[1] is ParsedMultimodalItem.Card)
        assertEquals(2, result[0].multimodalElements.size)
    }

    @Test
    fun `parseConversationData parses CTAs alongside product cards and sources`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "conversationId": "conv-abc",
                      "interactionId": "inter-xyz",
                      "response": {
                        "message": "Here is a product [1]",
                        "promptSuggestions": ["Tell me more"],
                        "multimodalElements": {
                          "elements": [
                            {
                              "id": "prod-1",
                              "entity_info": {
                                "productName": "Great Product",
                                "productImageURL": "https://example.com/img.jpg"
                              }
                            },
                            {
                              "type": "ctaButton",
                              "id": "cta-1",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat with us",
                                  "url": "https://example.com/chat"
                                }
                              }
                            }
                          ]
                        },
                        "sources": [
                          {
                            "title": "Product Guide",
                            "url": "https://guide.example.com",
                            "citation_number": 1
                          }
                        ]
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val message = result[0]
        assertEquals("conv-abc", message.conversationId)
        assertEquals(1, message.multimodalElements.size)
        assertEquals("Great Product", message.multimodalElements[0].title)
        assertEquals(1, message.sources.size)
        assertEquals(1, message.promptSuggestions.size)
        assertEquals(2, message.orderedElements.size)
        assertTrue(message.orderedElements[0] is ParsedMultimodalItem.Card)
        val cta = message.orderedElements[1] as ParsedMultimodalItem.Cta
        assertEquals("Chat with us", cta.button.label)
        assertEquals("https://example.com/chat", cta.button.url)
    }

    @Test
    fun `parseConversationData parses CTA from type field`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Need help?",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "service-intent-live-chat",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat now",
                                  "url": "https://example.com/live-chat"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertEquals(1, result[0].orderedElements.size)
        val cta = result[0].orderedElements[0] as ParsedMultimodalItem.Cta
        assertEquals("Chat now", cta.button.label)
        assertEquals("https://example.com/live-chat", cta.button.url)
    }

    @Test
    fun `parseConversationData CTA with type field is excluded from multimodalElements`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Need help?",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "service-intent-live-chat",
                              "entity_info": {
                                "primary": {
                                  "text": "Chat now",
                                  "url": "https://example.com/live-chat"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        assertTrue(result[0].multimodalElements.isEmpty())
    }

    @Test
    fun `parseConversationData parses CTA label from entity_info primary text`() {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "We can help!",
                        "multimodalElements": {
                          "elements": [
                            {
                              "type": "ctaButton",
                              "id": "cta-primary",
                              "entity_info": {
                                "primary": {
                                  "text": "Shop Now",
                                  "url": "https://example.com/shop"
                                }
                              }
                            }
                          ]
                        }
                      },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, result.size)
        val cta = result[0].orderedElements[0] as ParsedMultimodalItem.Cta
        assertEquals("Shop Now", cta.button.label)
        assertEquals("https://example.com/shop", cta.button.url)
    }
}


