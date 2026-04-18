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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeFeedbackBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.FeedbackDisplayMode

private val POSITIVE_CATEGORIES = listOf(
    "Helpful and relevant recommendations",
    "Clear and easy to understand",
    "Friendly and conversational tone",
    "Visually appealing presentation",
    "Other"
)

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
 * Renders as a modal card overlay (`"modal"`) or a ModalBottomSheet (`"action"`) per
 * `behavior.feedback.displayMode` in the theme JSON.
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
    val displayMode = ConciergeTheme.behavior?.feedback?.displayMode ?: FeedbackDisplayMode.MODAL

    when (displayMode) {
        FeedbackDisplayMode.MODAL -> FeedbackDialogCard(
            modifier = modifier,
            feedback = feedback,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
        FeedbackDisplayMode.ACTION -> FeedbackDialogBottomSheet(
            modifier = modifier,
            feedback = feedback,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

// --- Resolvers for theme-driven text and categories ---

@Composable
private fun resolveCategories(feedbackType: FeedbackType): List<String> {
    val themeConfig = ConciergeTheme.config
    return if (feedbackType == FeedbackType.POSITIVE) {
        themeConfig?.feedbackPositiveOptions ?: POSITIVE_CATEGORIES
    } else {
        themeConfig?.feedbackNegativeOptions ?: NEGATIVE_CATEGORIES
    }
}

@Composable
private fun resolveTitle(feedbackType: FeedbackType): String {
    val themeText = ConciergeTheme.text
    return if (feedbackType == FeedbackType.POSITIVE) {
        themeText?.feedbackDialogTitlePositive ?: "Your feedback is appreciated"
    } else {
        themeText?.feedbackDialogTitleNegative ?: "Your feedback is appreciated"
    }
}

@Composable
private fun resolveQuestion(feedbackType: FeedbackType): String {
    val themeText = ConciergeTheme.text
    return if (feedbackType == FeedbackType.POSITIVE) {
        themeText?.feedbackDialogQuestionPositive ?: "What went well? Select all that apply."
    } else {
        themeText?.feedbackDialogQuestionNegative ?: "What went wrong? Select all that apply."
    }
}

/** Notes field visibility: `showNotes` when set, otherwise per-sentiment `positive/negativeNotesEnabled`. */
@Composable
private fun resolveNotesEnabled(feedbackType: FeedbackType): Boolean {
    val componentsFeedback = ConciergeTheme.tokens?.components?.feedback
    val sentimentFallback = if (feedbackType == FeedbackType.POSITIVE) {
        componentsFeedback?.positiveNotesEnabled ?: true
    } else {
        componentsFeedback?.negativeNotesEnabled ?: true
    }
    val behavior: ConciergeFeedbackBehavior? = ConciergeTheme.behavior?.feedback
    return behavior?.resolvedShowNotes(sentimentFallback) ?: sentimentFallback
}

/** Close (X) button visibility: `true` for `"action"`, `false` for `"modal"` by default. */
@Composable
private fun resolveShowCloseButton(): Boolean {
    val behavior = ConciergeTheme.behavior?.feedback
    return behavior?.resolvedShowCloseButton()
        ?: (ConciergeTheme.behavior?.feedback?.displayMode == FeedbackDisplayMode.ACTION)
}

/** Cancel button visibility: `true` for `"modal"`, `false` for `"action"` by default. */
@Composable
private fun resolveShowCancelButton(): Boolean {
    val behavior = ConciergeTheme.behavior?.feedback
    return behavior?.resolvedShowCancelButton()
        ?: (ConciergeTheme.behavior?.feedback?.displayMode != FeedbackDisplayMode.ACTION)
}

// --- Shared UI components ---

/**
 * Selectable category checkbox list used by both Card and BottomSheet modes.
 */
@Composable
private fun CategoryCheckboxList(
    categories: List<String>,
    selectedCategories: Set<String>,
    onToggle: (String) -> Unit,
    style: ConciergeStyles.FeedbackDialogStyle
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(style.categorySpacing)
    ) {
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(category) },
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
}

/**
 * Dialog title row with an optional trailing X close button. Shared by card and sheet.
 */
@Composable
private fun FeedbackTitleRow(
    titleText: String,
    showCloseButton: Boolean,
    onDismiss: () -> Unit,
    style: ConciergeStyles.FeedbackDialogStyle
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        val titlePadding = if (showCloseButton) Modifier.padding(end = 40.dp) else Modifier
        Text(
            text = titleText,
            style = style.titleStyle,
            color = style.titleColor,
            textAlign = style.titleTextAlign,
            modifier = Modifier
                .fillMaxWidth()
                .then(titlePadding)
        )
        if (showCloseButton) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Close",
                    tint = style.closeIconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Cancel + Submit button row, honoring `showCancelButton` and the resolved button styles. */
@Composable
private fun FeedbackActionButtons(
    showCancelButton: Boolean,
    submitEnabled: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    style: ConciergeStyles.FeedbackDialogStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(style.buttonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val themeText = ConciergeTheme.text
        if (showCancelButton) {
            OutlinedButton(
                onClick = onCancel,
                shape = style.cancelButtonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = style.cancelButtonFill,
                    contentColor = style.cancelButtonColor
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = style.cancelButtonBorderWidth,
                    color = style.cancelButtonBorderColor
                )
            ) {
                Text(
                    text = themeText?.feedbackDialogCancel ?: "Cancel",
                    style = style.buttonTextStyle.copy(fontWeight = style.cancelButtonFontWeight)
                )
            }
        }

        Button(
            onClick = onSubmit,
            enabled = submitEnabled,
            shape = style.submitButtonShape,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = style.submitButtonColor,
                contentColor = style.submitButtonTextColor
            )
        ) {
            Text(
                text = themeText?.feedbackDialogSubmit ?: "Submit",
                style = style.buttonTextStyle.copy(fontWeight = style.submitButtonFontWeight),
                color = style.submitButtonTextColor
            )
        }
    }
}

/**
 * Themable drag-handle capsule used in action (bottom sheet) mode.
 */
@Composable
private fun FeedbackDragHandle(style: ConciergeStyles.FeedbackDialogStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(color = style.dragHandleColor, shape = RoundedCornerShape(2.dp))
        )
    }
}

// --- Card mode ---

@Composable
private fun FeedbackDialogCard(
    modifier: Modifier = Modifier,
    feedback: Feedback,
    onDismiss: () -> Unit,
    onSubmit: (Feedback) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.padding),
        colors = CardDefaults.cardColors(containerColor = style.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
        shape = style.shape
    ) {
        FeedbackCardContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.contentPadding)
                .verticalScroll(rememberScrollState()),
            feedback = feedback,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

/**
 * Card content: title, optional close X, question, categories, optional notes field,
 * and the Cancel/Submit action row. Visibility toggles honor `behavior.feedback.show*`.
 */
@Composable
private fun FeedbackCardContent(
    modifier: Modifier = Modifier,
    feedback: Feedback,
    onDismiss: () -> Unit,
    onSubmit: (Feedback) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle
    val focusManager = LocalFocusManager.current
    val themeText = ConciergeTheme.text
    val titleText = resolveTitle(feedback.feedbackType)
    val questionText = resolveQuestion(feedback.feedbackType)
    val categories = resolveCategories(feedback.feedbackType)
    val showNotes = resolveNotesEnabled(feedback.feedbackType)
    val showCloseButton = resolveShowCloseButton()
    val showCancelButton = resolveShowCancelButton()

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var notesText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        FeedbackTitleRow(
            titleText = titleText,
            showCloseButton = showCloseButton,
            onDismiss = onDismiss,
            style = style
        )

        Spacer(modifier = Modifier.height(style.titleSpacing))

        Text(
            text = questionText,
            style = style.questionStyle,
            color = style.questionColor,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(style.questionSpacing))

        CategoryCheckboxList(
            categories = categories,
            selectedCategories = selectedCategories,
            onToggle = { category ->
                selectedCategories = if (category in selectedCategories) {
                    selectedCategories - category
                } else {
                    selectedCategories + category
                }
            },
            style = style
        )

        if (showNotes) {
            Spacer(modifier = Modifier.height(style.categoriesNotesSpacing))

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
                shape = style.checkboxShape,
                colors = OutlinedTextFieldDefaults.colors(
                    // Notes fill matches the sheet/card surface.
                    focusedContainerColor = style.backgroundColor,
                    unfocusedContainerColor = style.backgroundColor,
                    // Notes outline matches the checkbox outline when themed.
                    focusedBorderColor = style.textFieldBorderColor,
                    unfocusedBorderColor = style.textFieldBorderColor,
                    focusedTextColor = style.textFieldTextColor,
                    unfocusedTextColor = style.textFieldTextColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                maxLines = 3
            )
        }

        Spacer(modifier = Modifier.height(style.notesButtonsSpacing))

        FeedbackActionButtons(
            showCancelButton = showCancelButton,
            submitEnabled = selectedCategories.isNotEmpty() || (showNotes && notesText.isNotBlank()),
            onCancel = onDismiss,
            onSubmit = {
                onSubmit(
                    feedback.copy(
                        selectedCategories = selectedCategories.toList(),
                        notes = if (showNotes) notesText.trim() else ""
                    )
                )
            },
            style = style
        )
    }
}

// --- Bottom sheet mode ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDialogBottomSheet(
    modifier: Modifier = Modifier,
    feedback: Feedback,
    onDismiss: () -> Unit,
    onSubmit: (Feedback) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = style.backgroundColor,
        // Themable capsule handle replaces Material3's default.
        dragHandle = { FeedbackDragHandle(style) },
        modifier = modifier
    ) {
        FeedbackBottomSheetContent(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            feedback = feedback,
            onDismiss = onDismiss,
            onSubmit = onSubmit
        )
    }
}

/**
 * Bottom sheet content: title (with optional X close), question, categories, and action buttons.
 * The notes field is intentionally not rendered in action mode; submitted `notes` is always `""`.
 */
@Composable
private fun FeedbackBottomSheetContent(
    modifier: Modifier = Modifier,
    feedback: Feedback,
    onDismiss: () -> Unit,
    onSubmit: (Feedback) -> Unit
) {
    val style = ConciergeStyles.feedbackDialogStyle
    val titleText = resolveTitle(feedback.feedbackType)
    val questionText = resolveQuestion(feedback.feedbackType)
    val categories = resolveCategories(feedback.feedbackType)
    val showCloseButton = resolveShowCloseButton()
    val showCancelButton = resolveShowCancelButton()

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = modifier.padding(horizontal = style.contentPadding)) {
        FeedbackTitleRow(
            titleText = titleText,
            showCloseButton = showCloseButton,
            onDismiss = onDismiss,
            style = style
        )

        Spacer(modifier = Modifier.height(style.titleSpacing))

        Text(
            text = questionText,
            style = style.questionStyle,
            color = style.questionColor,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(style.questionSpacing))

        CategoryCheckboxList(
            categories = categories,
            selectedCategories = selectedCategories,
            onToggle = { category ->
                selectedCategories = if (category in selectedCategories) {
                    selectedCategories - category
                } else {
                    selectedCategories + category
                }
            },
            style = style
        )

        Spacer(modifier = Modifier.height(style.notesButtonsSpacing))

        FeedbackActionButtons(
            showCancelButton = showCancelButton,
            submitEnabled = selectedCategories.isNotEmpty(),
            onCancel = onDismiss,
            onSubmit = {
                onSubmit(
                    feedback.copy(
                        selectedCategories = selectedCategories.toList(),
                        notes = ""
                    )
                )
            },
            style = style
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
