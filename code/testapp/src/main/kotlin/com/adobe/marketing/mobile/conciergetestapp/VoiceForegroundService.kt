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

package com.adobe.marketing.mobile.conciergetestapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.conciergeapp.R

/**
 * Keeps a voice conversation running while the app is in the background.
 *
 * Android stops giving an app the microphone soon after it leaves the screen unless the app runs a
 * foreground service of type `microphone`. [LiveKitTransport] starts this service when Brand
 * Concierge connects a conversation and stops it when the conversation ends, however it ends.
 *
 * While it runs, the service shows an ongoing notification. Tapping it opens the app, and its
 * **End** action ends the conversation normally, as if the user had stopped it in the chat. Removing
 * the app from the recent apps screen ends the conversation too. If the user has turned off the
 * app's notifications, the service runs without the notification; the app does not ask for the
 * notification permission.
 *
 * Declare the service in the manifest with `android:foregroundServiceType="microphone"`, together
 * with the `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` and `POST_NOTIFICATIONS`
 * permissions.
 */
class VoiceForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            // Ending the conversation stops the service through stop(); without a conversation,
            // the service has nothing to do.
            endConversation?.invoke() ?: stopSelf()
            return START_NOT_STICKY
        }
        // A service started with startForegroundService must call startForeground, even when the
        // conversation ended before the service got here.
        if (!startInForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (endConversation == null) {
            leaveForeground()
        } else {
            running = this
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        endConversation?.invoke()
        leaveForeground()
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    // ServiceCompat passes the type only on Android versions that support it.
    @SuppressLint("InlinedApi")
    private fun startInForeground(): Boolean {
        val notifications = NotificationManagerCompat.from(this)
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.voice_notification_channel))
                .build()
        )
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val end = PendingIntent.getService(
            this,
            0,
            Intent(this, VoiceForegroundService::class.java).setAction(ACTION_END),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_voice_notification)
            .setContentTitle(getString(R.string.voice_notification_title))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.voice_notification_end), end)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            true
        } catch (e: Exception) {
            // For example, the app left the screen before the service started, or the microphone
            // permission was revoked. The conversation goes on but may lose the microphone in the
            // background.
            Log.w(TAG, "Could not run the voice conversation in the foreground: ${e.message}")
            false
        }
    }

    private fun leaveForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "VoiceForegroundService"
        private const val CHANNEL_ID = "voice_conversation"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_END = "com.adobe.marketing.mobile.conciergetestapp.action.END_VOICE_CONVERSATION"

        // Both fields are confined to the main thread, where services and the transport run.

        /** Ends the current conversation; set from [start] until [stop]. */
        private var endConversation: (() -> Unit)? = null

        /** The service once it is in the foreground, until it stops. */
        private var running: VoiceForegroundService? = null

        /**
         * Starts the service for a conversation. [endConversation] is called when the user ends the
         * conversation from the notification or removes the app from the recent apps screen; it
         * should leave the room and report the conversation as ended.
         *
         * Call this on the main thread while the app is in the foreground and has the microphone
         * permission.
         */
        fun start(context: Context, endConversation: () -> Unit) {
            this.endConversation = endConversation
            try {
                ContextCompat.startForegroundService(context, Intent(context, VoiceForegroundService::class.java))
            } catch (e: Exception) {
                // Android 12+ refuses to start a foreground service from the background.
                Log.w(TAG, "Could not start the voice foreground service: ${e.message}")
            }
        }

        /** Stops the service and forgets the conversation. Call this on the main thread. */
        fun stop() {
            if (endConversation == null) return
            endConversation = null
            // A service that has not reached onStartCommand yet stops itself there, because
            // stopping it before it calls startForeground crashes the app.
            running?.leaveForeground()
        }
    }
}
