package com.adobe.marketing.mobile.concierge.ui.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * A custom View that wraps ConciergeChat for easy integration into XML-based applications.
 *
 * This view supports two integration modes:
 *
 * ## Mode 1: Direct Chat View (Full-screen chat)
 * Shows the chat interface directly without a dialog wrapper.
 *
 * Usage in XML:
 * ```xml
 * <com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView
 *     android:id="@+id/concierge_chat"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * ```
 *
 * Usage in Activity/Fragment:
 * ```kotlin
 * val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
 * chatView.bind(
 *     lifecycleOwner = this,
 *     viewModelStoreOwner = this,
 *     onClose = { finish() }
 * )
 * ```
 *
 * ## Mode 2: Dialog-based Chat (With trigger view)
 * Shows a trigger view (like a button) that opens the chat in a dialog when clicked.
 * Only shows the trigger view when the Concierge repository is ready.
 *
 * Usage in Activity/Fragment:
 * ```kotlin
 * val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
 * val triggerButton = Button(this).apply {
 *     text = "Start Chat"
 * }
 * chatView.bind(
 *     lifecycleOwner = this,
 *     viewModelStoreOwner = this,
 *     triggerView = triggerButton
 * )
 * ```
 */
class ConciergeChatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val composeView = ComposeView(context)
    private var viewModel: ConciergeChatViewModel? = null
    private lateinit var onCloseCallback: () -> Unit

    init {
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        // Set composition strategy to dispose when detached from window
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    /**
     * Binds the chat view to lifecycle and viewmodel owners (Mode 1: Direct Chat).
     *
     * Shows the chat interface directly without a dialog wrapper.
     *
     * @param lifecycleOwner The lifecycle owner (usually Activity or Fragment)
     * @param viewModelStoreOwner The viewmodel store owner (usually Activity or Fragment)
     * @param onClose Optional callback when the close button is pressed
     */
    fun bind(
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        onClose: () -> Unit
    ) {
        this.onCloseCallback = onClose

        // Create or get existing ViewModel
        viewModel = ViewModelProvider(viewModelStoreOwner)[ConciergeChatViewModel::class.java]

        // Set the Compose content
        composeView.setContent {
            viewModel?.let { vm ->
                ConciergeChat(
                    viewModel = vm,
                    onClose = onCloseCallback
                )
            }
        }
    }

    /**
     * Binds the chat view to show a trigger view that opens chat in a dialog (Mode 2: Dialog-based).
     *
     * This mode:
     * - Checks if Concierge repository is ready (configuration loaded and ECID available)
     * - Shows the trigger view only when ready
     * - Opens chat in a full-screen dialog when trigger view is clicked
     * - Reuses the same dialog implementation as the Compose wrapper
     *
     * @param lifecycleOwner The lifecycle owner (usually Activity or Fragment)
     * @param viewModelStoreOwner The viewmodel store owner (usually Activity or Fragment)
     * @param triggerView The view (e.g., Button) that will trigger the chat dialog when clicked
     */
    fun bind(
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        triggerView: View
    ) {
        // Create or get existing ViewModel
        viewModel = ViewModelProvider(viewModelStoreOwner)[ConciergeChatViewModel::class.java]

        // Set the Compose content using the wrapper composable
        composeView.setContent {
            viewModel?.let { vm ->
                // Use the dialog-based ConciergeChat wrapper (same as MainScreen.kt)
                ConciergeChat(viewModel = vm) { showChat ->
                    // Wrap trigger view in a Box to center it
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { triggerView },
                            modifier = Modifier.wrapContentSize()
                        )

                        // Setup click listener to trigger chat dialog
                        DisposableEffect(triggerView) {
                            val clickListener = OnClickListener { showChat() }
                            triggerView.setOnClickListener(clickListener)

                            onDispose {
                                triggerView.setOnClickListener(null)
                            }
                        }
                    }
                }
            }
        }
    }
}