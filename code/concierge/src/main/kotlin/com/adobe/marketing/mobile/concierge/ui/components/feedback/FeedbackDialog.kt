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

package com.adobe.marketing.mobile.concierge.ui.components.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/**
 * Positive feedback categories
 */
private val POSITIVE_CATEGORIES = listOf(
    "Helpful and relevant recommendations",
    "Clear and easy to understand",
    "Friendly and conversational tone",
    "Visually appealing presentation",
    "Other"
)

/**
 * Negative feedback categories
 */
private val NEGATIVE_CATEGORIES = listOf(
    "Didn't understand my request",
    "Unhelpful or irrelevant information",
    "Too vague or lacking detail",
    "Errors or poor quality response",
    "Other"
)

/**
 * Feedback dialog component that captures user feedback with selectable categories
 * and optional notes.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param feedback Feedback data comprised of the interaction ID and sentiment.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onSubmit Callback invoked when feedback is submitted.
 */
@Composable
internal fun FeedbackDialog(
    modifier: Modifier = Modifier,
    feedback: Feedback,
    onDismiss: () -> Unit,
    onSubmit: (Feedback) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle
    val focusManager = LocalFocusManager.current
    val themeText = ConciergeTheme.text
    val themeConfig = ConciergeTheme.config

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var notesText by remember { mutableStateOf("") }

    val categories = if (feedback.feedbackType == FeedbackType.POSITIVE) {
        themeConfig?.feedbackPositiveOptions ?: POSITIVE_CATEGORIES
    } else {
        themeConfig?.feedbackNegativeOptions ?: NEGATIVE_CATEGORIES
    }
    
    val titleText = if (feedback.feedbackType == FeedbackType.POSITIVE) {
        themeText?.feedbackDialogTitlePositive ?: "Your feedback is appreciated"
    } else {
        themeText?.feedbackDialogTitleNegative ?: "Your feedback is appreciated"
    }
    
    val questionText = if (feedback.feedbackType == FeedbackType.POSITIVE) {
        themeText?.feedbackDialogQuestionPositive ?: "What went well? Select all that apply."
    } else {
        themeText?.feedbackDialogQuestionNegative ?: "What went wrong? Select all that apply."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.padding),
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
        shape = style.shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = titleText,
                style = style.titleStyle,
                color = style.titleColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(style.titleSpacing))
            
            // Question
            Text(
                text = questionText,
                style = style.questionStyle,
                color = style.questionColor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(style.questionSpacing))
            
            // Categories
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategories = if (category in selectedCategories) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = category in selectedCategories,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = style.checkboxCheckedColor,
                                checkmarkColor = style.checkboxCheckmarkColor,
                                uncheckedColor = style.checkboxUncheckedColor
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(style.checkboxSpacing))
                        
                        Text(
                            text = category,
                            style = style.categoryTextStyle,
                            color = style.categoryTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(style.categoriesNotesSpacing))
            
            // Notes section
            Text(
                text = themeText?.feedbackDialogNotes ?: "Notes",
                style = style.notesLabelStyle,
                color = style.notesLabelColor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(style.notesLabelSpacing))
            
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = themeText?.feedbackDialogNotesPlaceholder ?: "Add any additional comments...",
                        style = style.notesPlaceholderStyle,
                        color = style.notesPlaceholderColor
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = style.textFieldBorderColor,
                    unfocusedBorderColor = style.textFieldBorderColor.copy(alpha = 0.5f),
                    focusedTextColor = style.textFieldTextColor,
                    unfocusedTextColor = style.textFieldTextColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(style.notesButtonsSpacing))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = style.cancelButtonColor
                    )
                ) {
                    Text(
                        text = themeText?.feedbackDialogCancel ?: "Cancel",
                        style = style.buttonTextStyle
                    )
                }
                
                Spacer(modifier = Modifier.width(style.buttonSpacing))
                
                Button(
                    onClick = {
                        onSubmit(
                            feedback.copy(
                                selectedCategories = selectedCategories.toList(),
                                notes = notesText.trim()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = style.submitButtonColor
                    ),
                    enabled = selectedCategories.isNotEmpty() || notesText.isNotBlank()
                ) {
                    Text(
                        text = themeText?.feedbackDialogSubmit ?: "Submit",
                        style = style.buttonTextStyle,
                        color = style.submitButtonTextColor
                    )
                }
            }
        }
    }
}
