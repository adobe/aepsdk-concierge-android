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

package com.adobe.marketing.mobile.concierge.ui.config

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeConfigTest {

    // ========== SuggestedPrompt Tests ==========

    @Test
    fun `SuggestedPrompt creates with text only`() {
        val prompt = SuggestedPrompt(text = "How can I help you?")
        
        assertEquals("How can I help you?", prompt.text)
        assertNull(prompt.imageUrl)
        assertNull(prompt.backgroundColor)
    }

    @Test
    fun `SuggestedPrompt creates with all fields`() {
        val prompt = SuggestedPrompt(
            text = "Product recommendations",
            imageUrl = "https://example.com/icon.png",
            backgroundColor = "#FF5733"
        )
        
        assertEquals("Product recommendations", prompt.text)
        assertEquals("https://example.com/icon.png", prompt.imageUrl)
        assertEquals("#FF5733", prompt.backgroundColor)
    }

    @Test
    fun `SuggestedPrompt creates with text and imageUrl`() {
        val prompt = SuggestedPrompt(
            text = "Browse catalog",
            imageUrl = "https://example.com/catalog.png"
        )
        
        assertEquals("Browse catalog", prompt.text)
        assertEquals("https://example.com/catalog.png", prompt.imageUrl)
        assertNull(prompt.backgroundColor)
    }

    @Test
    fun `SuggestedPrompt creates with text and backgroundColor`() {
        val prompt = SuggestedPrompt(
            text = "Contact support",
            backgroundColor = "#0066CC"
        )
        
        assertEquals("Contact support", prompt.text)
        assertNull(prompt.imageUrl)
        assertEquals("#0066CC", prompt.backgroundColor)
    }

    @Test
    fun `SuggestedPrompt supports copy`() {
        val original = SuggestedPrompt("Original text")
        val updated = original.copy(text = "Updated text")
        
        assertEquals("Original text", original.text)
        assertEquals("Updated text", updated.text)
    }

    @Test
    fun `SuggestedPrompt with same values are equal`() {
        val prompt1 = SuggestedPrompt(
            text = "test",
            imageUrl = "url",
            backgroundColor = "#FF0000"
        )
        val prompt2 = SuggestedPrompt(
            text = "test",
            imageUrl = "url",
            backgroundColor = "#FF0000"
        )
        
        assertEquals(prompt1, prompt2)
    }

    @Test
    fun `SuggestedPrompt with different text are not equal`() {
        val prompt1 = SuggestedPrompt("text1")
        val prompt2 = SuggestedPrompt("text2")
        
        assertNotEquals(prompt1, prompt2)
    }

    @Test
    fun `SuggestedPrompt handles empty text`() {
        val prompt = SuggestedPrompt("")
        
        assertEquals("", prompt.text)
    }

    @Test
    fun `SuggestedPrompt handles long text`() {
        val longText = "A".repeat(500)
        val prompt = SuggestedPrompt(longText)
        
        assertEquals(longText, prompt.text)
    }

    @Test
    fun `SuggestedPrompt handles multiline text`() {
        val text = "Line 1\nLine 2"
        val prompt = SuggestedPrompt(text)
        
        assertEquals(text, prompt.text)
    }

    @Test
    fun `SuggestedPrompt handles special characters`() {
        val text = "What's your favorite product? #sale"
        val prompt = SuggestedPrompt(text)
        
        assertEquals(text, prompt.text)
    }

    @Test
    fun `SuggestedPrompt handles unicode characters`() {
        val text = "こんにちは 🎉"
        val prompt = SuggestedPrompt(text)
        
        assertEquals(text, prompt.text)
    }

    // ========== WelcomeConfig Tests ==========

    @Test
    fun `WelcomeConfig creates with default values`() {
        val config = WelcomeConfig()
        
        assertTrue(config.showWelcomeCard)
        assertEquals("BrandName", config.brandName)
        assertNull(config.firstTimeWelcomeMessage)
        assertNull(config.returningUserWelcomeMessage)
        assertEquals(ConciergeConstants.WelcomeCard.DEFAULT_HEADING, config.welcomeHeader)
        assertEquals(ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING, config.subHeader)
        assertTrue(config.suggestedPrompts.isEmpty())
    }

    @Test
    fun `WelcomeConfig creates with custom brand name`() {
        val config = WelcomeConfig(brandName = "Acme Corp")
        
        assertEquals("Acme Corp", config.brandName)
    }

    @Test
    fun `WelcomeConfig creates with showWelcomeCard false`() {
        val config = WelcomeConfig(showWelcomeCard = false)
        
        assertEquals(false, config.showWelcomeCard)
    }

    @Test
    fun `WelcomeConfig creates with custom welcome messages`() {
        val config = WelcomeConfig(
            firstTimeWelcomeMessage = "Welcome for the first time!",
            returningUserWelcomeMessage = "Welcome back!"
        )
        
        assertEquals("Welcome for the first time!", config.firstTimeWelcomeMessage)
        assertEquals("Welcome back!", config.returningUserWelcomeMessage)
    }

    @Test
    fun `WelcomeConfig creates with custom headers`() {
        val config = WelcomeConfig(
            welcomeHeader = "How can we help?",
            subHeader = "Choose a topic below"
        )
        
        assertEquals("How can we help?", config.welcomeHeader)
        assertEquals("Choose a topic below", config.subHeader)
    }

    @Test
    fun `WelcomeConfig creates with suggested prompts`() {
        val prompts = listOf(
            SuggestedPrompt("Prompt 1"),
            SuggestedPrompt("Prompt 2"),
            SuggestedPrompt("Prompt 3")
        )
        val config = WelcomeConfig(suggestedPrompts = prompts)
        
        assertEquals(3, config.suggestedPrompts.size)
        assertEquals("Prompt 1", config.suggestedPrompts[0].text)
        assertEquals("Prompt 2", config.suggestedPrompts[1].text)
        assertEquals("Prompt 3", config.suggestedPrompts[2].text)
    }

    @Test
    fun `WelcomeConfig creates with all fields`() {
        val prompts = listOf(
            SuggestedPrompt("Product info", "icon1.png", "#FF0000"),
            SuggestedPrompt("Support", "icon2.png", "#00FF00")
        )
        
        val config = WelcomeConfig(
            showWelcomeCard = true,
            brandName = "Test Brand",
            firstTimeWelcomeMessage = "First time message",
            returningUserWelcomeMessage = "Welcome back message",
            welcomeHeader = "Custom Header",
            subHeader = "Custom Subheader",
            suggestedPrompts = prompts
        )
        
        assertTrue(config.showWelcomeCard)
        assertEquals("Test Brand", config.brandName)
        assertEquals("First time message", config.firstTimeWelcomeMessage)
        assertEquals("Welcome back message", config.returningUserWelcomeMessage)
        assertEquals("Custom Header", config.welcomeHeader)
        assertEquals("Custom Subheader", config.subHeader)
        assertEquals(2, config.suggestedPrompts.size)
    }

    @Test
    fun `WelcomeConfig supports copy`() {
        val original = WelcomeConfig(brandName = "Original")
        val updated = original.copy(brandName = "Updated")
        
        assertEquals("Original", original.brandName)
        assertEquals("Updated", updated.brandName)
    }

    @Test
    fun `WelcomeConfig with same values are equal`() {
        val config1 = WelcomeConfig(
            brandName = "Test",
            welcomeHeader = "Header"
        )
        val config2 = WelcomeConfig(
            brandName = "Test",
            welcomeHeader = "Header"
        )
        
        assertEquals(config1, config2)
    }

    @Test
    fun `WelcomeConfig with different brand names are not equal`() {
        val config1 = WelcomeConfig(brandName = "Brand1")
        val config2 = WelcomeConfig(brandName = "Brand2")
        
        assertNotEquals(config1, config2)
    }

    @Test
    fun `WelcomeConfig handles empty brand name`() {
        val config = WelcomeConfig(brandName = "")
        
        assertEquals("", config.brandName)
    }

    @Test
    fun `WelcomeConfig handles empty suggested prompts list`() {
        val config = WelcomeConfig(suggestedPrompts = emptyList())
        
        assertTrue(config.suggestedPrompts.isEmpty())
    }

    @Test
    fun `WelcomeConfig handles single suggested prompt`() {
        val prompts = listOf(SuggestedPrompt("Only prompt"))
        val config = WelcomeConfig(suggestedPrompts = prompts)
        
        assertEquals(1, config.suggestedPrompts.size)
        assertEquals("Only prompt", config.suggestedPrompts[0].text)
    }

    @Test
    fun `WelcomeConfig handles many suggested prompts`() {
        val prompts = (1..10).map { SuggestedPrompt("Prompt $it") }
        val config = WelcomeConfig(suggestedPrompts = prompts)
        
        assertEquals(10, config.suggestedPrompts.size)
        assertEquals("Prompt 1", config.suggestedPrompts[0].text)
        assertEquals("Prompt 10", config.suggestedPrompts[9].text)
    }

    @Test
    fun `WelcomeConfig handles long brand name`() {
        val longName = "A".repeat(100)
        val config = WelcomeConfig(brandName = longName)
        
        assertEquals(longName, config.brandName)
    }

    @Test
    fun `WelcomeConfig handles long header text`() {
        val longHeader = "This is a very long welcome header text that might span multiple lines"
        val config = WelcomeConfig(welcomeHeader = longHeader)
        
        assertEquals(longHeader, config.welcomeHeader)
    }

    @Test
    fun `WelcomeConfig handles multiline header`() {
        val header = "Line 1\nLine 2"
        val config = WelcomeConfig(welcomeHeader = header)
        
        assertEquals(header, config.welcomeHeader)
    }

    @Test
    fun `WelcomeConfig handles special characters in brand name`() {
        val brandName = "Acme Corp™ & Co."
        val config = WelcomeConfig(brandName = brandName)
        
        assertEquals(brandName, config.brandName)
    }

    @Test
    fun `WelcomeConfig handles unicode in text fields`() {
        val config = WelcomeConfig(
            brandName = "ブランド",
            welcomeHeader = "こんにちは 🎉",
            subHeader = "サブヘッダー"
        )
        
        assertEquals("ブランド", config.brandName)
        assertEquals("こんにちは 🎉", config.welcomeHeader)
        assertEquals("サブヘッダー", config.subHeader)
    }

    @Test
    fun `WelcomeConfig can disable welcome card`() {
        val config = WelcomeConfig(
            showWelcomeCard = false,
            brandName = "Test",
            suggestedPrompts = listOf(SuggestedPrompt("Test"))
        )
        
        assertEquals(false, config.showWelcomeCard)
        // Other fields should still be set even if card is disabled
        assertEquals("Test", config.brandName)
        assertEquals(1, config.suggestedPrompts.size)
    }

    @Test
    fun `WelcomeConfig can be updated via copy to add prompts`() {
        val config1 = WelcomeConfig()
        val config2 = config1.copy(
            suggestedPrompts = listOf(
                SuggestedPrompt("New prompt 1"),
                SuggestedPrompt("New prompt 2")
            )
        )
        
        assertTrue(config1.suggestedPrompts.isEmpty())
        assertEquals(2, config2.suggestedPrompts.size)
    }

    @Test
    fun `WelcomeConfig can be updated via copy to change messages`() {
        val config1 = WelcomeConfig()
        val config2 = config1.copy(
            firstTimeWelcomeMessage = "New first time message",
            returningUserWelcomeMessage = "New returning message"
        )
        
        assertNull(config1.firstTimeWelcomeMessage)
        assertNull(config1.returningUserWelcomeMessage)
        assertEquals("New first time message", config2.firstTimeWelcomeMessage)
        assertEquals("New returning message", config2.returningUserWelcomeMessage)
    }

    @Test
    fun `WelcomeConfig preserves default constants`() {
        val config = WelcomeConfig()
        
        // Verify default values come from ConciergeConstants
        assertEquals(ConciergeConstants.WelcomeCard.DEFAULT_HEADING, config.welcomeHeader)
        assertEquals(ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING, config.subHeader)
    }
}
