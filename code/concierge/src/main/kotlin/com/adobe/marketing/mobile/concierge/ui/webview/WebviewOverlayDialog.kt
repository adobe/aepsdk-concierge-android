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

package com.adobe.marketing.mobile.concierge.ui.webview

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import kotlinx.coroutines.launch

/**
 * Full-height overlay dialog that presents a URL in a WebView.
 * Dismissible by back press, tap outside, or dragging the sheet down past a threshold.
 *
 * @param url The URL to load in the overlay WebView
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
internal fun WebviewOverlayDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val style = ConciergeStyles.webviewStyle
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { style.dismissDragThreshold.toPx() }
    val initialOffsetPx = with(density) { style.slideInInitialOffsetDp.toPx() }
    val offsetY = remember { Animatable(initialOffsetPx) }
    var contentHeightPx by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, animationSpec = tween(style.slideAnimationDurationMs))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            // Lay out edge-to-edge so the sheet sizes from the real window and applies insets itself.
            decorFitsSystemWindows = false
        )
    ) {
        val parentView = LocalView.current.parent as? View
        (parentView as? DialogWindowProvider)?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            // Let the window overlap system bars and cutouts; the insets are applied in Compose instead.
            window.attributes = window.attributes.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fitInsetsTypes = 0
                    fitInsetsSides = 0
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = {
                            scope.launch {
                                val targetY = if (contentHeightPx > 0) contentHeightPx else initialOffsetPx
                                offsetY.animateTo(
                                    targetValue = targetY,
                                    animationSpec = tween(style.slideAnimationDurationMs)
                                )
                                onDismiss()
                            }
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(style.contentHeightFraction)
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size: IntSize -> contentHeightPx = size.height.toFloat() }
                    .graphicsLayer { translationY = offsetY.value }
                    .shadow(
                        elevation = style.contentElevation,
                        shape = RoundedCornerShape(topStart = style.contentCornerRadius, topEnd = style.contentCornerRadius)
                    )
                    .clip(RoundedCornerShape(topStart = style.contentCornerRadius, topEnd = style.contentCornerRadius))
                    .background(style.contentBackgroundColor),
                contentAlignment = Alignment.TopCenter
            ) {
                WebViewSheetContent(
                    url = url,
                    onDismiss = onDismiss,
                    // Bars, cutouts and the IME on the sides and bottom; the sheet background stays edge-to-edge.
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(style.handleWidth)
                        .height(style.handleHeight)
                        .padding(top = style.handleTopPadding)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f))
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetY.value > dismissThresholdPx) {
                                            offsetY.animateTo(
                                                targetValue = contentHeightPx,
                                                animationSpec = tween(style.slideAnimationDurationMs)
                                            )
                                            onDismiss()
                                        } else {
                                            offsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(style.slideAnimationDurationMs)
                                            )
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(style.handlePillWidth, style.handlePillHeight)
                            .clip(RoundedCornerShape(style.handlePillCornerRadius))
                            .background(style.handlePillColor)
                    )
                }
            }
        }
    }
}
