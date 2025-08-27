package com.adobe.marketing.mobile.concierge.ui.chat

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * A custom View that wraps ConciergeChat for easy integration into XML-based applications.
 *
 * Usage in XML:
 * ```xml
 * <com.adobe.marketing.mobile.concierge.ui.xml.ConciergeChatView
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
 *     onClose = { chatView.visibility = View.GONE }
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
     * Binds the chat view to lifecycle and viewmodel owners.
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
}