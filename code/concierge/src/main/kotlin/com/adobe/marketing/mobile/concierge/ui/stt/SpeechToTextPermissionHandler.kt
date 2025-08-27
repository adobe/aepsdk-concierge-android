package com.adobe.marketing.mobile.concierge.ui.stt

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun SpeechPermissionHandler(
    hasPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            showPermissionDialog = true
        }
    }
}

@Composable
private fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Microphone Permission Required") },
        text = { Text("This app needs access to your microphone to enable speech recognition. Would you like to grant permission?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}