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

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.VoiceTransportListener
import com.adobe.marketing.mobile.concierge.VoiceTransportState
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.DisconnectReason
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.AudioTrackPublishDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Brand Concierge's [VoiceTransport] built on the LiveKit Android SDK.
 *
 * Copy this file into your app and add `io.livekit:livekit-android` to your app's dependencies.
 * Create one instance with the application context and pass it to every `ConciergeChat(...)` call
 * and `ConciergeChatView`.
 *
 * Brand Concierge calls this class on the main thread, and the class keeps its own state on the
 * main thread too: LiveKit callbacks hop to main and are dropped unless they come from the current
 * room. That is how a connection reports nothing after [disconnect]. Creating and releasing a room
 * block (WebRTC initialization and teardown), so all room work runs on one background thread.
 *
 * Audio routing and playback are left to LiveKit's default audio handler, which prefers headsets,
 * then the speaker, over the earpiece. When another app or a phone call takes audio focus, the
 * transport reports an audio interruption and Brand Concierge mutes the microphone until it ends.
 */
class LiveKitTransport(context: Context) : VoiceTransport {

    @Volatile
    override var listener: VoiceTransportListener? = null

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    // The fields below are confined to the main thread.

    /**
     * Moves on with every [connect] and [disconnect], so a room created for an earlier connection
     * can tell it has been superseded.
     */
    private var connectionId = 0

    /** The current connection's room, set once it is created. */
    private var room: Room? = null
    private var roomFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    /** Runs the current connection's room work; cancelled on [disconnect]. */
    private var scope: CoroutineScope? = null

    /** Microphone changes run one after another, so a quick mute and unmute land in order. */
    private val microphoneLock = Mutex()

    override fun connect(url: String, token: String) {
        disconnect()
        val id = connectionId
        val connectionScope = CoroutineScope(SupervisorJob() + roomDispatcher)
        scope = connectionScope
        connectionScope.launch {
            val newRoom = try {
                LiveKit.create(
                    appContext,
                    RoomOptions(
                        // Records the microphone while the room connects and sends that audio to
                        // the agent once it joins, so the user can start speaking right away.
                        audioTrackPublishDefaults = AudioTrackPublishDefaults(preconnect = true)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onMain { if (id == connectionId) fail(e) }
                return@launch
            }
            onMain { adopt(newRoom, id) }
            // Collects until the scope is cancelled; events hop to main and are checked there.
            launch(start = CoroutineStart.UNDISPATCHED) {
                newRoom.events.collect { event -> onMain(ifCurrent = newRoom) { handle(event) } }
            }
            val error = try {
                newRoom.connect(url, token, ConnectOptions(audio = true))
                val participant = newRoom.localParticipant
                if (!participant.isMicrophoneEnabled && !participant.setMicrophoneEnabled(true)) {
                    throw IllegalStateException("Failed to publish the microphone track.")
                }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }
            onMain(ifCurrent = newRoom) {
                if (error != null) fail(error) else report(VoiceTransportState.Connected)
            }
        }
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        val currentRoom = room ?: return
        scope?.launch {
            microphoneLock.withLock {
                try {
                    if (!currentRoom.localParticipant.setMicrophoneEnabled(enabled)) {
                        Log.w(TAG, "Could not set the microphone enabled=$enabled.")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set the microphone enabled=$enabled: ${e.message}")
                }
            }
        }
    }

    override fun disconnect() {
        connectionId++
        // Cancelling first stops pending connect and microphone work from touching the room; the
        // release task is queued behind any room work already running on the room thread.
        scope?.cancel()
        scope = null
        val currentRoom = room ?: return
        val focusListener = roomFocusListener
        room = null
        roomFocusListener = null
        release(currentRoom, focusListener)
    }

    /** Makes [newRoom] the current room, unless its connection was superseded while it was created. */
    private fun adopt(newRoom: Room, id: Int) {
        if (id != connectionId) {
            release(newRoom, null)
            return
        }
        val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
            onMain(ifCurrent = newRoom) { handleFocusChange(change) }
        }
        (newRoom.audioHandler as? AudioSwitchHandler)?.registerOnAudioFocusChangeListener(focusListener)
        room = newRoom
        roomFocusListener = focusListener
    }

    private fun handle(event: RoomEvent) {
        when (event) {
            is RoomEvent.Disconnected -> {
                Log.d(TAG, "Room disconnected: ${event.reason}")
                disconnect()
                val error = event.error
                when {
                    error != null -> report(VoiceTransportState.Failed(error))
                    event.reason in NORMAL_DISCONNECT_REASONS -> report(VoiceTransportState.Disconnected)
                    else -> report(VoiceTransportState.Failed(IllegalStateException("The voice room disconnected (${event.reason}).")))
                }
            }
            is RoomEvent.DataReceived ->
                listener?.onDataReceived(this, event.data, event.topic?.takeIf { it.isNotEmpty() })
            is RoomEvent.TrackSubscribed ->
                Log.i(TAG, "Subscribed to ${event.track.kind} track from ${event.participant.identity?.value ?: "unknown"}.")
            else -> Unit
        }
    }

    private fun handleFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> listener?.onAudioInterruptionBegan(this)
            AudioManager.AUDIOFOCUS_GAIN -> listener?.onAudioInterruptionEnded(this)
            else -> Unit
        }
    }

    private fun fail(error: Exception) {
        disconnect()
        report(VoiceTransportState.Failed(error))
    }

    private fun report(state: VoiceTransportState) {
        listener?.onStateChanged(this, state)
    }

    private fun release(oldRoom: Room, focusListener: AudioManager.OnAudioFocusChangeListener?) {
        roomExecutor.execute {
            try {
                focusListener?.let { (oldRoom.audioHandler as? AudioSwitchHandler)?.unregisterOnAudioFocusChangeListener(it) }
                oldRoom.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release the voice room: ${e.message}")
            }
        }
    }

    private fun onMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    /** Runs [action] on main if [ifCurrent] is still the current connection's room. */
    private fun onMain(ifCurrent: Room, action: () -> Unit) {
        mainHandler.post { if (ifCurrent === room) action() }
    }

    private companion object {
        const val TAG = "LiveKitTransport"

        /**
         * Leaving, the room being deleted or the user being removed ends the conversation
         * normally; any other reason is a failure. Matches the iOS reference transport.
         */
        val NORMAL_DISCONNECT_REASONS = setOf(
            DisconnectReason.CLIENT_INITIATED,
            DisconnectReason.ROOM_DELETED,
            DisconnectReason.PARTICIPANT_REMOVED
        )

        // At most one thread, which exits when idle, so no thread outlives voice usage.
        val roomExecutor = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue()) { runnable ->
            Thread(runnable, "LiveKitTransport").apply { isDaemon = true }
        }
        val roomDispatcher = roomExecutor.asCoroutineDispatcher()
    }
}
