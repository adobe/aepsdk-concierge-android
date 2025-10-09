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
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Data class representing a feedback category
 */
data class FeedbackCategory(
    val id: String,
    val label: String
)

/**
 * Data class for the submitted feedback
 */
data class FeedbackSubmission(
    val interactionId: String,
    val feedbackType: FeedbackType,
    val selectedCategories: List<String>,
    val notes: String
)

/**
 * Enum representing the type of feedback
 */
enum class FeedbackType {
    POSITIVE,
    NEGATIVE
}

/**
 * Feedback dialog component that captures user feedback with selectable categories
 * and optional notes.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param interactionId The interaction ID for the feedback.
 * @param feedbackType The type of feedback (positive or negative).
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onSubmit Callback invoked when feedback is submitted.
 */
@Composable
internal fun FeedbackDialog(
    modifier: Modifier = Modifier,
    interactionId: String,
    feedbackType: FeedbackType,
    onDismiss: () -> Unit,
    onSubmit: (FeedbackSubmission) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle
    val focusManager = LocalFocusManager.current

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var notesText by remember { mutableStateOf("") }

    val categories = when (feedbackType) {
        FeedbackType.POSITIVE -> listOf(
            FeedbackCategory("helpful", "Helpful and relevant recommendations"),
            FeedbackCategory("clear", "Clear and easy to understand"),
            FeedbackCategory("friendly", "Friendly and conversational tone"),
            FeedbackCategory("visual", "Visually appealing presentation"),
            FeedbackCategory("other", "Other")
        )
        FeedbackType.NEGATIVE -> listOf(
            FeedbackCategory("unclear", "Didn't understand my request"),
            FeedbackCategory("irrelevant", "Unhelpful or irrelevant information"),
            FeedbackCategory("vague", "Too vague or lacking detail"),
            FeedbackCategory("errors", "Errors or poor quality response"),
            FeedbackCategory("other", "Other")
        )
    }
    
    val questionText = when (feedbackType) {
        FeedbackType.POSITIVE -> "What went well? Select all that apply."
        FeedbackType.NEGATIVE -> "What went wrong? Select all that apply."
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
                text = "Your feedback is appreciated",
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCategories.contains(category.id),
                            onCheckedChange = { isChecked ->
                                selectedCategories = if (isChecked) {
                                    selectedCategories + category.id
                                } else {
                                    selectedCategories - category.id
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = style.checkboxCheckedColor,
                                uncheckedColor = style.checkboxUncheckedColor
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(style.checkboxSpacing))
                        
                        Text(
                            text = category.label,
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
                text = "Notes",
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
                        text = "Add any additional comments...",
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
                        text = "Cancel",
                        style = style.buttonTextStyle
                    )
                }
                
                Spacer(modifier = Modifier.width(style.buttonSpacing))
                
                Button(
                    onClick = {
                        onSubmit(
                            FeedbackSubmission(
                                interactionId = interactionId,
                                feedbackType = feedbackType,
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
                        text = "Submit",
                        style = style.buttonTextStyle,
                        color = style.submitButtonTextColor
                    )
                }
            }
        }
    }
}
