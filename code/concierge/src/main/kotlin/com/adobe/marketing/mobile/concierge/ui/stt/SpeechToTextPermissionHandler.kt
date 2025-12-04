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

package com.adobe.marketing.mobile.concierge.ui.stt

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

/**
 * Simple permission handler for audio recording.
 * 
 * Flow:
 * 1. User clicks mic button (shouldRequestPermission = true)
 * 2. Show rationale dialog explaining mic recording and Google speech recognition
 * 3. Launch system permission dialog
 * 4. Handle result:
 *    - If allowed (once or while using) → Start recording
 *    - If denied → Show settings dialog
 */
@Composable
internal fun SpeechPermissionHandler(
    hasPermission: Boolean,
    shouldRequestPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onPermissionHandled: () -> Unit
) {
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isInPermissionFlow by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity

    // System permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - notify parent
            onPermissionResult(true)
        } else {
            // Permission denied - check if we can ask again
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.RECORD_AUDIO)
            } ?: false

            if (canAskAgain) {
                onPermissionResult(false)
            } else {
                // User denied with must go to settings next time
                showSettingsDialog = true
                onPermissionResult(false)
            }
        }
        isInPermissionFlow = false
        onPermissionHandled()
    }

    // Rationale dialog - shown before system permission dialog
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showRationaleDialog = false
                isInPermissionFlow = false
                onPermissionHandled()
            },
            title = { Text("Microphone & Speech Recognition Permission Required") },
            text = { 
                Text("This app needs access to your microphone to enable voice input. \nYour audio will be recorded and sent to Google servers for speech recognition processing.")
            },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    // Launch system permission dialog
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    isInPermissionFlow = false
                    onPermissionHandled()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings dialog - shown after permission denial
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
                isInPermissionFlow = false
            },
            title = { Text("Permission Required") },
            text = { 
                Text("Microphone & Speech Recognition is essential for using voice input. \n" +
                        "Please enable it in the app settings to use this feature.")
            },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog = false
                    isInPermissionFlow = false
                    // Navigate to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showSettingsDialog = false
                    isInPermissionFlow = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Trigger: When mic button is clicked and permission not granted
    // Don't trigger if we're already in the permission flow
    if (shouldRequestPermission && !hasPermission && !isInPermissionFlow) {
        isInPermissionFlow = true
        showRationaleDialog = true
    }
}