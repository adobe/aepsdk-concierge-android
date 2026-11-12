# Brand Concierge Implementation Guide

## Overview

The Brand Concierge extension provides an in-app conversational UI (a chat surface) that can be embedded into a host app with minimal UI wiring. The UI is connected to the Adobe Experience Platform by using AEP SDK shared state (Configuration + Edge Identity) to derive the service configuration needed to run a session and enable brand controlled experiences through configuration and theming.

The Brand Concierge UI is presented through two integration approaches:

- **Managed Integration**: Provide a drop-in entry point to the chat interface and let the Brand Concierge extension automatically manage it.
- **Custom Integration**: Embed and manage the chat UI directly into your app's view hierarchy for dedicated chat screens or custom layouts.

Both approaches are available for Compose and XML/Views-based apps.

---

## Prerequisites

### Required SDK modules
Your app needs these AEP SDK's available and registered:

- [Mobile Core](https://github.com/adobe/aepsdk-core-android)
- [Edge Identity](https://github.com/adobe/aepsdk-edgeidentity-android)
- [Brand Concierge](https://github.com/adobe/aepsdk-concierge-android)

### Android version
- Minimum Android API level 24 (Android 7.0) or higher

### Permissions (for speech input)
Speech to text uses Android Speech Recognition API + microphone API's for voice input functionality. Add this permission to your app `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

The SDK handles permission requests internally when users interact with the microphone button.

### Voice (optional)
Spoken conversations with the agent need a Brand Concierge version that includes `VoiceTransport` (3.10.2 and earlier don't), a LiveKit dependency in your app, and voice enabled for your Brand Concierge by Adobe. See [Voice conversations](#voice-conversations-optional) for the setup steps and limitations.

---

## Installation

Add the dependencies to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.adobe.marketing.mobile:concierge:3.+")
    implementation("com.adobe.marketing.mobile:core:3.5.0")
    implementation("com.adobe.marketing.mobile:edgeidentity:3.0.0")
}
```

Then sync your project with the Gradle files.

---

## Configuration

### Step 1: Register the Brand Concierge extension

Import and register the extensions in your `Application` class `onCreate()`:

```kotlin
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.edge.identity.Identity as EdgeIdentity
import android.app.Application

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.initialize(this, "YOUR_APP_ID")
    }
}
```

Replace `YOUR_APP_ID` with your mobile property App ID from Adobe Data Collection. For full setup instructions see the [Adobe Experience Platform Mobile SDK getting started guide](https://developer.adobe.com/client-sdks/home/getting-started/).

### Step 2: Validate the Brand Concierge configuration keys
If you set the Mobile SDK log level to trace:

```kotlin
MobileCore.setLogLevel(LoggingMode.VERBOSE)
```

you can then inspect the app logs to confirm that extension shared states are being set with the expected values.

Brand Concierge expects the following keys to be present in the Configuration shared state:

- **`concierge.server`**: String (server host or base domain used by Brand Concierge requests)
- **`concierge.configId`**: String (datastream ID)
- **`concierge.region`**: String, optional (region identifier, e.g. `va6`, inserted into the Brand Concierge request path; omit to use the default unqualified endpoint)

The full Edge Identity `identityMap` (including the ECID) is read from Edge Identity shared state and forwarded to Brand Concierge requests.

Another option for validation is to use Adobe Assurance. Refer to the [Mobile SDK validation guide](https://developer.adobe.com/client-sdks/home/getting-started/validate/).

---

## Identities

Brand Concierge forwards the full Edge Identity `identityMap` on every chat and feedback request. The ECID is always included automatically. To send additional identities (e.g. a hashed email, `CRMID`, or a custom namespace), set them via the Edge Identity extension's [`updateIdentities`](https://developer.adobe.com/client-sdks/edge/identity-for-edge-network/api-reference/#updateidentities) API — they are forwarded verbatim, so lowercasing/hashing is the app's responsibility.

Namespace priority and identity-graph rules are configured server-side in Adobe Experience Platform; the SDK does not interpret or relabel namespaces.

### Resetting identities

Call the following Core API when the host app changes users or signs out:

```kotlin
MobileCore.resetIdentities()
```

Concierge then ends the current conversation locally:

- **Cleared:** backend session, transcript, unsent draft, pending XDM context, image cache and in-flight work (chat, data handoff, feedback and speech capture). Nothing is retried under the new identity.
- **Retained:** token-provider registration, configuration, consent, surfaces and server-side conversation history. The host remains responsible for sign-out and clearing its auth token.
- **UI:** a visible chat stays open with input disabled until Concierge is ready again; a hidden chat stays hidden. Open calls made during reset are ignored, and data handoffs during reset complete with `NO_ACTIVE_SESSION`.

Sending resumes, with a new `sessionId`, once Edge Identity reports reset complete and refreshed identity and configuration are available. This requires **Edge Identity 3.0.0 or later**. If reset is still pending after five seconds, Concierge logs an error describing what it is waiting on.

**Custom components:**
- Custom image providers should ensure `clear()` also prevents in-progress downloads from repopulating the cache.
- Custom `SpeechCapturing` engines should drop old recognition results and queued callbacks when `setListener(null)` is called.

After reset, if a conversation was in progress, Concierge dispatches a **Brand Concierge Conversation Ended** event (`com.adobe.eventType.concierge` / `com.adobe.eventSource.notification`, `conciergeEventType: concierge:conversation:ended`) for Event Hub/Assurance diagnostics. It includes `reason: identity_reset`, `epochTime` (Unix ms), `hadActiveTurn`, and the previous `sessionId`/`conversationId` when available. It contains no transcript, XDM, identity or token data, and is not sent to Edge.

## XDM context

Use `Concierge.updateXDMContext(...)` to hold app-provided XDM data. The SDK sends it with every
later chat message (typed, dictated, or a tapped prompt suggestion) and every data handoff. Feedback
requests don't include it, and neither do voice conversations.

```kotlin
Concierge.updateXDMContext(
    mapOf(
        "loyalty" to mapOf("tier" to "gold"),
        "commerce" to mapOf("currencyCode" to "USD")
    )
)

// Remove a key
Concierge.updateXDMContext(mapOf("loyalty" to mapOf("tier" to null)))
```

- **Merging:** Each update is applied as an RFC 7396 JSON Merge Patch. Maps merge recursively,
  lists and scalar values replace the existing value, and a `null` map value removes that key.
  There is no reset API. To remove context, send `null` values.
- **Allowed values:** strings, booleans, finite `Byte`, `Short`, `Int`, `Long`, `Float`, or
  `Double` values, maps with string keys, and lists of these values. A `null` inside a list is
  sent as JSON null. Nesting is limited to 20 levels below each top-level value. The top-level
  `identityMap` key is reserved for the SDK. Invalid input, including cyclic input, throws
  `IllegalArgumentException`.
- **Lifetime:** Context is held in memory for the current Concierge session (30 minutes of
  inactivity). Updating it doesn't start or extend a session. If no session exists yet, the next
  request adopts the context. When a session expires, its context is not sent with later requests,
  and the next update starts from an empty context. `MobileCore.resetIdentities()` also clears it.
- **Timing:** A chat message or handoff captures the context when it's submitted. Updates made
  after that don't change requests that are already queued.
- **Data handoffs:** The handoff's `xdmFields` are deep-merged over the held context using the same
  rules, and the handoff values win. The held context itself isn't changed.
- **Visibility:** The context is included in the **Brand Concierge Query Submitted** Event Hub
  event, so Assurance and other listening extensions can read it. The SDK doesn't forward it to
  Edge tracking and doesn't write it to the SDK log.

---

## Authentication

If your backend requires proof of the user's identity, register a `ConciergeAuthTokenProvider` to supply an app-minted authentication token. Brand Concierge attaches it to every chat and feedback request until the provider is cleared. Voice conversations don't send it.

```kotlin
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeAuthTokenProvider

Concierge.setAuthTokenProvider(ConciergeAuthTokenProvider {
    // Return the current token, or null to send the turn without one
    myAuthTokenCache.getCurrentToken()
})
```

Register the provider once, typically alongside extension registration in your `Application.onCreate()`. Pass `null` to `setAuthTokenProvider` to clear a previously registered provider.

- `provideToken()` is called once per turn (chat and feedback) on a background thread — the token is never cached, so refreshed or rotated tokens are picked up on the next turn. It may block briefly to refresh the token; the SDK bounds the wait (3 seconds by default) and sends the turn without a token if `provideToken()` doesn't return in time.
- Returning `null` or a blank value, or throwing, sends the turn without a token rather than failing it — the token is never merged into the identity payload or sent as a request header.
- To use a different wait budget than the 3-second default (for example if your token mint is consistently slower or faster), pass `timeoutMillis` to `setAuthTokenProvider`:

```kotlin
Concierge.setAuthTokenProvider(
    ConciergeAuthTokenProvider { myAuthTokenCache.getCurrentToken() },
    timeoutMillis = 5000L
)
```

---

## Data handoff

Use `Concierge.sendDataHandoff(...)` when your app needs to hand the SDK data that did not
originate in the chat UI—for example, the result of a native checkout flow that completed outside
of chat. The SDK forwards the data to the Brand Concierge agent pipeline and renders its response
through the active chat transcript without requiring the user to type or say a chat message.

> **Prerequisite:** Keep a configured `ConciergeChat` or `ConciergeChatView` rendered with a
> non-empty `surfaces` list while calling this API. The active chat session provides both the
> routing surfaces and the transcript that receives the service response. A handoff made without
> an active chat session fails with `NO_ACTIVE_SESSION`.

```kotlin
import com.adobe.marketing.mobile.concierge.Concierge

Concierge.sendDataHandoff(
    routingHint = "successful-checkout",
    xdmFields = mapOf(
        "commerce" to mapOf(
            "order" to mapOf(
                "purchaseID" to orderId,
                "priceTotal" to 129.99,
                "currencyCode" to "USD"
            )
        )
    ),
    localMessage = "Your order is confirmed!"
) { accepted, rejectReason ->
    // accepted == true  -> Brand Concierge completed and rendered the response.
    // accepted == false -> validation or delivery failed; inspect rejectReason before retrying.
}
```

### `Concierge.sendDataHandoff(routingHint, xdmFields, localMessage, completion)`

- **`routingHint`**: A string consumed only by Brand Concierge's current phrase-based
  router (for example, `"successful-checkout"`). The end user never sees it, and it is not
  conversational content. Defaults to an empty string; pass an empty or blank string when the XDM
  fields alone determine routing, and the SDK forwards it as an empty service query. Because it is
  the first parameter, `@JvmOverloads` generates no Java overload that omits it — Java callers pass
  `""` explicitly, Kotlin callers use named arguments.
- **`xdmFields`** *(required)*: Arbitrary XDM-shaped data merged into the root of the outbound XDM
  object alongside the SDK-owned identity map. Use nested Kotlin maps and lists, for example
  `mapOf("commerce" to mapOf("order" to mapOf("purchaseID" to "123")))`. The map must be non-empty;
  every key must be a `String`; and values follow the [XDM context](#xdm-context) value rules,
  except that `null` is allowed only inside lists. Do not use `identityMap` as a top-level key
  because the SDK owns and populates it. These fields are deep-merged over any held XDM context.
- **`localMessage`**: Optional text for a local, non-networked chat message distinct from the data
  forwarded to Brand Concierge. The SDK renders it immediately before an accepted handoff starts,
  as an agent-attributed message rather than a user message.
- **`completion`**: Optional `ConciergeDataHandoffCallback`, called exactly once on a background
  thread. `accepted` is `true` only after Brand Concierge completes a response stream with
  renderable content. When `accepted` is `false`, `rejectReason` is a typed
  `ConciergeDataHandoffRejectReason`:

  | Reject reason | Meaning |
  | --- | --- |
  | `MISSING_EVENT_DATA` | No payload reached the extension. This indicates an internal wiring issue and is not normally caller-triggered. |
  | `INVALID_ROUTING_HINT_TYPE` | `routingHint` was not a string in the underlying event payload. A missing or blank `routingHint` is accepted, not rejected (see above). |
  | `MISSING_XDM_FIELDS` | `xdmFields` was missing from the underlying event payload. |
  | `INVALID_XDM_FIELDS_TYPE` | `xdmFields` was not a map in the underlying event payload. |
  | `EMPTY_XDM_FIELDS` | `xdmFields` was empty. |
  | `INVALID_XDM_FIELD_KEY` | `xdmFields` contained a key that was not a string. |
  | `RESERVED_KEY_COLLISION` | `xdmFields` used an SDK-reserved top-level key such as `identityMap`. |
  | `INVALID_XDM_FIELD_VALUE` | `xdmFields` contained a value that cannot be serialized as JSON. |
  | `NO_ACTIVE_SESSION` | No rendered Concierge chat session was available to receive the handoff. |
  | `CHAT_IN_PROGRESS` | A chat turn, a voice conversation, or another handoff is active or waiting. Retry after it completes. |
  | `DELIVERY_FAILED` | Brand Concierge returned an error or the service request could not complete. |
  | `EMPTY_RESPONSE` | Brand Concierge completed without any text, cards, or CTAs to render. |
  | `DELIVERY_TIMEOUT` | Brand Concierge did not complete within the handoff delivery timeout. |
  | `NO_RESPONSE` | The extension did not respond, for example because the request timed out. |

Chat messages use a finite FIFO queue. Data handoffs never join that queue: if a chat message or
another handoff is active or waiting, the SDK immediately reports `CHAT_IN_PROGRESS` and does not
render `localMessage` or call the service. Retry the handoff after the active request completes.
After an accepted handoff starts, delivery failures, empty responses, and timeouts do not add an
error message to the transcript. An already-rendered `localMessage` remains visible, and the host
app owns any failure UI based on the completion result.

---

## Voice conversations (optional)

Brand Concierge can hold a spoken conversation with the agent. The SDK runs the conversation (credentials, microphone permission, session lifecycle, transcript and UI), and your app supplies the audio connection by implementing the `VoiceTransport` interface. Brand Concierge has no WebRTC dependency, so apps that don't use voice ship no WebRTC code.

The test app's [`LiveKitTransport.kt`](../code/testapp/src/main/kotlin/com/adobe/marketing/mobile/conciergetestapp/LiveKitTransport.kt) is a complete transport built on the [LiveKit Android SDK](https://github.com/livekit/client-sdk-android). To add voice to your app:

#### Before you start

- **Brand Concierge version.** Voice needs a Brand Concierge version that includes `VoiceTransport`; 3.10.2 and earlier don't.
- **Voice enabled for your Brand Concierge.** Ask your Adobe contact to confirm it. The agent's voice and language are set there, not in the app.
- **A physical Android device** for the checks in [Audio quality](#audio-quality).

#### 1. Add LiveKit to your app

```kotlin
dependencies {
    implementation("io.livekit:livekit-android:2.29.1")
}
```

LiveKit is your app's dependency: you choose and update its version, and Brand Concierge does not constrain it. The test app pins 2.29.1. LiveKit depends on artifacts published to JitPack, so add `maven { url = uri("https://jitpack.io") }` to the repositories in your `settings.gradle.kts`.

#### 2. Set up the manifest permissions

Declare `RECORD_AUDIO` in your app's `AndroidManifest.xml` (see [Permissions](#permissions-for-speech-input)). Brand Concierge requests it at runtime when the user taps the voice button and starts the conversation once it is granted.

The reference transport keeps the conversation running when the app moves to the background, through the test app's `VoiceForegroundService` (see [Background conversations](#background-conversations)). Declare that service with `android:foregroundServiceType="microphone"`, and the permissions it needs:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application>
    <service
        android:name=".VoiceForegroundService"
        android:exported="false"
        android:foregroundServiceType="microphone" />
</application>
```

LiveKit's manifest is merged into your app's. Besides `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO` and `FOREGROUND_SERVICE`, it adds `CAMERA` and `FOREGROUND_SERVICE_MEDIA_PROJECTION`, and a screen-sharing service; LiveKit's audio routing library also adds `MODIFY_AUDIO_SETTINGS`. Voice uses none of the camera or screen-sharing entries, so unless your app needs them for something else, remove them with `tools:node="remove"`. Keep `FOREGROUND_SERVICE`, which the voice service needs:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.CAMERA"
        tools:node="remove"
        tools:ignore="PermissionImpliesUnsupportedChromeOsHardware" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" tools:node="remove" />

    <application>
        <service
            android:name="io.livekit.android.room.track.screencapture.ScreenCaptureService"
            tools:node="remove" />
    </application>
</manifest>
```

Android lint reports the removed `CAMERA` entry as if it were declared, hence `tools:ignore`. Check the merged manifest (Android Studio's **Merged Manifest** tab) after you update LiveKit, because a new version can add entries.

#### 3. Copy the transport

Copy [`LiveKitTransport.kt`](../code/testapp/src/main/kotlin/com/adobe/marketing/mobile/conciergetestapp/LiveKitTransport.kt) and [`VoiceForegroundService.kt`](../code/testapp/src/main/kotlin/com/adobe/marketing/mobile/conciergetestapp/VoiceForegroundService.kt) into your app (change the package, log tags and notification action to your own). The service uses the test app's notification strings (`voice_notification_channel`, `voice_notification_title`, `voice_notification_end`) and icon (`ic_voice_notification`); copy them or use your own. It also needs AndroidX Core 1.12 or later, which apps using Compose or AppCompat already have. Before you publish, declare the `microphone` foreground service in the Google Play Console (see [Background conversations](#background-conversations)). A `VoiceTransport` connects to the URL and token in the `VoiceConnection` Brand Concierge passes to `connect(connection)`, publishes the microphone, plays the agent's audio, and reports its state, incoming data messages and audio interruptions to its `listener`. The reference transport also records the microphone while the room connects and sends that audio once the agent joins, so the user can start speaking right away.

If you change the transport, keep the rules in the `VoiceTransport` documentation: Brand Concierge calls the transport on the main thread, and the transport may call the listener from any thread. When you update LiveKit, check that the file still builds and repeat the device checks below.

#### 4. Pass the transport to the chat

Keep one instance for the life of the app, created with the application context: Brand Concierge reuses it for each conversation, sets the listener before connecting and clears it before disconnecting.

```kotlin
object Voice {
    lateinit var transport: VoiceTransport
        private set

    fun initialize(application: Application) {
        transport = LiveKitTransport(application)
    }
}

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Voice.initialize(this)
        // ... MobileCore.initialize ...
    }
}
```

```kotlin
ConciergeChat(
    viewModel = viewModel,
    surfaces = listOf("my-surface"),
    voiceTransport = Voice.transport
) { showChat -> /* ... */ }
```

The direct `ConciergeChat(viewModel, onClose, ...)` composable takes the same `voiceTransport` parameter. For the XML view, set `ConciergeChatView.voiceTransport` before or after calling `bind`. Passing `null` hides the voice button; a conversation that is already running keeps its transport until it ends. When several `ConciergeChat` calls share a view model, pass the same transport to each of them; otherwise the last one to recompose decides which transport the chat uses.

When a transport is provided, the composer shows a voice button. During a conversation:

- **Chat.** The transcript appears in the chat. Typing, dictation and data handoffs are paused; `Concierge.sendDataHandoff` completes with `CHAT_IN_PROGRESS`.
- **Microphone permission.** Brand Concierge asks for it when the user taps the voice button, the same way as for dictation. If the user denies it, no conversation starts.
- **One at a time.** Only one conversation runs across the app. If two chats are alive together (for example, a chat in an activity opened over another chat), the other chat's voice button is disabled.
- **Ending.** The conversation ends when the user taps the voice button again, leaves the chat, resets identities, or the app moves to the background (unless it [continues in the background](#background-conversations)). It also ends without a message 60 minutes after it first connects (reconnecting doesn't restart the count, and the limit can't be changed), or when the voice agent ends an idle session (after 60 seconds without activity).
- **Errors.** If the connection fails, can't be restored within 30 seconds (the voice button shows a spinner meanwhile), or the voice agent reports a server error, the conversation ends with an error message in the chat.
- **Interruptions and phone calls.** See [Audio quality](#audio-quality).

#### Audio quality

The transport, and therefore LiveKit, owns audio focus, audio routing and audio processing:

- **Keep echo cancellation and noise suppression on.** LiveKit enables them by default. Without them, the agent's voice from the speaker reaches the microphone and the agent can interrupt itself. Even with them, loud speaker playback can occasionally be heard as the user speaking; headphones avoid this.
- **Speaker output.** LiveKit prefers a Bluetooth headset, then wired headphones, then the speaker over the earpiece. Headphones and Bluetooth devices take over when connected.
- **Interruptions.** The reference transport reports a transient loss of audio focus (another app playing audio for a while) as an audio interruption, and Brand Concierge mutes the microphone until focus returns, including for a loss that began while connecting. A permanent loss (`AUDIOFOCUS_LOSS`, for example when another app starts playing music) ends the conversation with an error message, because the chat has no unmute control. While a phone call rings, the conversation continues. Answering it opens the call screen, which moves the app to the background and ends the conversation, unless it [continues in the background](#background-conversations): then the call takes the audio and the microphone stays muted until the call ends, and a call longer than the voice agent's inactivity timeout (60 seconds) ends the conversation.
- **Test on a device.** The emulator proves the wiring and the connection, but not echo, audio routes or interruptions. Check a conversation on the speaker, connecting and disconnecting wired and Bluetooth headphones mid-session, a call during a session (ringing, then answering, short and longer than 60 seconds), locking the screen and switching to another app mid-session, ending the conversation from the notification, removing the app from the recent apps, a session with the app's notifications turned off, and turning Wi-Fi off mid-session. With the reference transport, the spinner appears only when LiveKit starts a full reconnect, because LiveKit doesn't report its first attempt (a quick resume); after a network loss that can take about 30 seconds, and a resume that succeeds shows no spinner. Repeat these checks when you update LiveKit or change its audio settings elsewhere in your app.

#### Limitations

- **No authentication token or XDM context.** Brand Concierge doesn't send the token from `Concierge.setAuthTokenProvider` or the context from `Concierge.updateXDMContext(...)` when it starts a voice conversation, so the voice agent works without them. Typed and dictated messages in the same chat still include them.
- **Language.** The app doesn't send a locale with voice; the agent speaks the language set for your Brand Concierge.
- **Voice UI text is English** and not configurable: the voice button's accessibility labels ("Start voice conversation", "Stop voice conversation") and the microphone permission rationale.
- **Network.** Voice uses WebRTC (UDP, with TCP and TLS fallback) in addition to the HTTPS chat requests. Networks that block it, such as some corporate or venue Wi-Fi, can prevent voice from connecting while text chat works. Test on the networks your users use.
- **Google Play Data safety.** During a conversation, the microphone audio is streamed to Adobe's voice service, which transcribes it. Reflect this in your Data safety form and privacy policy.

### Background conversations

By default a voice conversation ends when the app moves to the background, including when the user locks the screen. A transport can keep it running there instead, so the user can lock the phone or switch to another app and keep talking, by returning `true` from `continuesInBackground`:

```kotlin
class MyVoiceTransport(/* ... */) : VoiceTransport {
    override val continuesInBackground = true
    // ...
}
```

Brand Concierge reads the property once per conversation, when it calls `connect(connection)`, so an app with an in-app setting can return the setting's value; a change applies to the next conversation. With it on, a conversation that has reached `connect(connection)` keeps running in the background, with the microphone live and the agent audible, until the user ends it, the voice agent ends it (for example after 60 seconds without activity), the connection ends, or it reaches 60 minutes. A conversation still waiting for its connection details when the app moves to the background ends as before, because Android does not let an app start using the microphone from the background. Transcripts and product cards received in the background are in the chat when the user returns.

Android 11 and later allow microphone access in the background only through a foreground service of type `microphone` started while the app is in the foreground. Brand Concierge declares no service or permission for this; the app provides them:

- Declare the permissions and the service in the app's `AndroidManifest.xml`:

  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

  <application>
      <service
          android:name=".VoiceForegroundService"
          android:exported="false"
          android:foregroundServiceType="microphone" />
  </application>
  ```

- Start the service from `connect(connection)`, before it returns. Brand Concierge calls `connect` on the main thread while the app is in the foreground and after `RECORD_AUDIO` has been granted, which is what Android requires to start a `microphone` foreground service. Stop the service in `disconnect()` and whenever the connection ends (failure, room closed).
- The service shows an ongoing notification while the conversation runs, for example "Voice conversation in progress", that opens the app when tapped and has an **End** action. End leaves the room and reports `VoiceTransportState.Disconnected`, which Brand Concierge treats as a normal end. On Android 13 and later, if the user has not granted the notification permission, the service still runs and the notification appears only in the task manager; Brand Concierge does not ask for that permission.
- End the conversation in the service's `onTaskRemoved` (leave the room and report `Disconnected`), so removing the app from the recent apps ends it. Otherwise the foreground service keeps the process, and the conversation, alive.
- Declare the `microphone` foreground service in the Google Play Console (App content > Foreground service permissions): describe its use (a voice conversation the user starts and can end at any time) and provide a video that shows it.

The test app's `LiveKitTransport` and `VoiceForegroundService` implement all of the above. To keep voice foreground-only, return `false` (the default) and leave out the service and its permissions.

---

## Integration

### Managed Integration

Use this when you want to provide a drop-in entry point to the chat interface and let the Brand Concierge extension automatically manage it.

This mode:
- Shows the configured trigger when the Brand Concierge extension is ready (ECID + concierge configuration available)
- Opens chat as a full-screen dialog when the trigger is invoked
- Handles dismissal (back press, close button, gestures) automatically

#### Jetpack Compose

The `ConciergeChat` composable can be configured with a UI element (button, floating action button, or any custom element) of your choice to act as a trigger to launch the chat interface. Pass the list of surface URLs via **`surfaces`**.

```kotlin
@Composable
fun MyScreen() {
    val viewModel = viewModel<ConciergeChatViewModel>()
    val surfaces = listOf("web://example.com/your-surface.html")
    
    // Your app content
    // ... other views ...
    
    ConciergeChat(
        viewModel = viewModel,
        surfaces = surfaces
    ) { showChat ->
        // Your trigger button/view that launches ConciergeChat
        MyTriggerButton(onClick = { showChat() }) 
    }
}
```

#### XML/Views
For non-Compose apps, the SDK provides `ConciergeChatView` that wraps the Compose chat UI and can be included in XML layouts.

> **Note:** The activity hosting `ConciergeChatView` must have `android:windowSoftInputMode="adjustResize"` added in the app's `AndroidManifest.xml` to ensure the chat input field remains visible when the keyboard is shown:
> ```xml
> <activity
>     android:name=".YourActivity"
>     android:windowSoftInputMode="adjustResize" />
> ```

**Step 1: Add the view to your XML layout**

```xml
<com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView
    android:id="@+id/concierge_chat"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**Step 2: Bind with a trigger view in your Activity/Fragment**

```kotlin
class XmlActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Load optional theme
        val theme = ConciergeThemeLoader.load(this, "myTheme.json")
        
        // Create a trigger button of your choice
        val triggerButton = Button(this).apply {
            text = "Start Chat"
            textSize = 18f
            setPadding(32, 16, 32, 16)
        }
        
        // Obtain the ConciergeChatView and bind the triggerButton
        val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
        val surfaces = listOf("web://example.com/your-surface.html")
        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            surfaces = surfaces,
            theme = theme,  // Optional: apply custom theme
            triggerView = triggerButton
        )
    }
}
```

---

### Custom Integration

Use this when you want to embed the chat interface directly into your app's view hierarchy for more flexibility. This is useful for dedicated chat screens or custom layouts.

In this mode, you should:
- Ensure that the necessary components (ECID, Configuration) are ready before making this visible. You can check readiness by observing `ConciergeStateRepository.instance.state`.
- Manage the logic to control what happens when the chat interface is dismissed when notified via `onClose`.

#### Jetpack Compose

Set surfaces via **`surfaces`** and ensure the extension is ready (configuration, ECID, and surfaces) before showing the chat.

```kotlin
@Composable
fun YourChatScreen() {
    val viewModel = viewModel<ConciergeChatViewModel>()
    val conciergeState by ConciergeStateRepository.instance.state.collectAsStateWithLifecycle()
    val surfaces = listOf("web://example.com/your-surface.html")
    // Set surfaces so they are available for the chat session
    ConciergeStateRepository.instance.setSessionSurfaces(surfaces)
    val ready = conciergeState.configurationReady &&
        conciergeState.experienceCloudId != null &&
        conciergeState.surfaces.isNotEmpty()
    
    if (ready) {
        ConciergeChat(
            viewModel = viewModel,
            onClose = {
                // your logic on close or back press
            }
        )
    } else {
        // Show your intermediate loading state or wait for SDK to be ready
    }
}
```

#### XML/Views

> **Note:** The activity hosting `ConciergeChatView` must have `android:windowSoftInputMode="adjustResize"` added in the app's `AndroidManifest.xml` to ensure the chat input field remains visible when the keyboard is shown:
> ```xml
> <activity
>     android:name=".YourActivity"
>     android:windowSoftInputMode="adjustResize" />
> ```

**Step 1: Add the view to your XML layout**

```xml
<com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView
    android:id="@+id/concierge_chat"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**Step 2: Bind in your Activity/Fragment**

```kotlin
class XmlActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Load optional theme
        val theme = ConciergeThemeLoader.load(this, "myTheme.json")
        
        // Obtain the chat view and bind
        val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
        val surfaces = listOf("web://example.com/your-surface.html")
        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            surfaces = surfaces,
            theme = theme,  // Optional: apply custom theme
            onClose = { finish() }
        )
    }
}
```

### Theme Customization

The Brand Concierge chat interface can be customized by loading the theme file from the `assets` directory of your app using `ConciergeThemeLoader`.

```kotlin
@Composable
fun MyScreen() {
    val context = LocalContext.current

    val theme = remember {
        ConciergeThemeLoader.load(context, "myTheme.json")
            ?: ConciergeThemeLoader.default()
    }
    
    ConciergeTheme(theme = theme) {
        ConciergeChat(/* ... */)
    }
}
```

More information regarding theme customization can be found in the [style-guide](./style-guide.md).

### Deep Links and App Links

#### Required manifest entries

**1. Register your app as an App Link handler (all API levels)**

Add an `<intent-filter>` with `android:autoVerify="true"` to the activity in your `AndroidManifest.xml` that should handle your domain's URLs. This triggers Android's domain verification against your domain's `assetlinks.json` file, which is what makes your app the verified handler. Without this, the Concierge extension's App Link check will always fall back to the in-app WebView.

```xml
<activity android:name=".YourActivity" ...>
    <intent-filter android:autoVerify="true" android:exported="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="yourdomain.com" />
    </intent-filter>
</activity>
```

**2. Package visibility for Android 11 or higher**

Add the following `<queries>` block to your `AndroidManifest.xml`. Without it, the Concierge extension cannot use `PackageManager.resolveActivity()` to detect the App Link handler on API 30 or higher, and App Links will silently fall back to the in-app WebView on that API level.

```xml
<!-- Required for PackageManager.resolveActivity() on Android 11+ to detect
     which app handles VIEW intents for http/https URLs. -->
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="http" />
    </intent>
</queries>
```

#### Chat message link handling

The Concierge extension automatically opens links when your app is the verified handler for the URL's domain (e.g., listed in the domain's assetlinks.json). If your app is not the handler, the link opens in the in-app WebView overlay.

**Default link handling flow:** host `handleLink` callback (if provided) → App Link check → WebView overlay.

To customize this behavior, provide a `handleLink` callback. Return `true` if your app handled the link; return `false` to have the Brand Concierge extension handle the link with its default behavior (trying to open it as an App Link first, then using the WebView overlay).

**Compose (ConciergeChat):**
```kotlin
val context = LocalContext.current
ConciergeChat(
    viewModel = viewModel,
    onClose = { finish() },
    handleLink = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true  // Handled
        } catch (e: ActivityNotFoundException) {
            false  // Fall back to WebView overlay
        }
    }
)
```

**XML (ConciergeChatView):**
```kotlin
chatView.bind(
    lifecycleOwner = this,
    viewModelStoreOwner = this,
    surfaces = surfaces,
    theme = theme,
    handleLink = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    },
    onClose = { finish() }
)
```

When `handleLink` returns `true`, the SDK does not open the WebView overlay. When it returns `false` or is null, the SDK uses the default flow (trying to open it as an App Link first, then using the WebView overlay).

To close the chat when a deeplink is clicked, call `viewModel.closeConcierge()` inside your `handleLink` callback before returning `true`:

```kotlin
ConciergeChat(
    viewModel = viewModel,
    handleLink = { url ->
        if (url.startsWith("myapp://")) {
            viewModel.closeConcierge()
            // navigate to the deeplink destination
            true
        } else {
            false
        }
    }
) { showChat -> ... }
```

#### In-app WebView overlay link handling

Links clicked inside the in-app WebView overlay (e.g., links on a product page) follow their own routing rules, independent of the `handleLink` callback:

- **http/https URLs**: If your app is the verified App Link handler for the domain, the URL is forwarded to your app. Otherwise it loads in the WebView.
- **Non-web schemes** (e.g., `mailto:`, `tel:`, `sms:`, `myapp://`): Forwarded to the system via `Intent.ACTION_VIEW`.
- **Dangerous schemes** (`javascript:`, `file:`, `content:`, `intent:`, `data:`): Blocked.

No additional configuration is required for this behavior. App Link forwarding within the WebView uses the same domain verification as chat message link handling.
