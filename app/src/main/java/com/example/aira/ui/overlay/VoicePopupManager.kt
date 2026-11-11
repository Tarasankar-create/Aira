package com.example.aira.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.aira.R
import com.example.aira.service.AssistantStateHolder
import com.example.aira.ui.theme.AiraTheme
import com.example.aira.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Manages displaying the Siri-style floating voice assistant popup over the system
 * and third-party apps using Android WindowManager (when SYSTEM_ALERT_WINDOW is granted).
 */
class VoicePopupManager(
    private val context: Context,
    private val onDismissRequested: () -> Unit
) {

    companion object {
        private const val TAG = "AiraVoicePopupManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var overlayScope: CoroutineScope? = null
    private var isOverlayAttached = false

    fun show() {
        mainHandler.post {
            AssistantStateHolder.updateStatus { it.copy(isPopupVisible = true) }

            if (!PermissionManager.hasOverlayPermission(context) || windowManager == null) {
                Log.d(TAG, "Overlay permission not granted; falling back to in-app/notification UI")
                return@post
            }

            if (isOverlayAttached) {
                Log.d(TAG, "Overlay is already attached to WindowManager")
                return@post
            }

            try {
                val themedContext = ContextThemeWrapper(context, R.style.Theme_Aira)
                val owner = OverlayLifecycleOwner().apply {
                    onCreate()
                    onResume()
                }
                lifecycleOwner = owner

                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                overlayScope = scope
                val recomposer = Recomposer(scope.coroutineContext)

                val composeView = ComposeView(themedContext).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    compositionContext = recomposer

                    setContent {
                        AiraTheme {
                            val status by AssistantStateHolder.status.collectAsState()
                            VoiceAssistantPopup(
                                status = status,
                                onDismiss = {
                                    hide()
                                    onDismissRequested()
                                }
                            )
                        }
                    }
                }

                scope.launch {
                    recomposer.runRecomposeAndApplyChanges()
                }

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    y = 20
                }

                windowManager.addView(composeView, layoutParams)
                overlayView = composeView
                isOverlayAttached = true
                Log.i(TAG, "Voice assistant overlay attached to WindowManager")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to attach voice overlay to WindowManager", e)
                detachOverlay()
            }
        }
    }

    fun hide(smoothDelayMs: Long = 250L) {
        mainHandler.post {
            AssistantStateHolder.updateStatus { it.copy(isPopupVisible = false) }

            if (!isOverlayAttached || overlayView == null) return@post

            mainHandler.postDelayed({
                detachOverlay()
            }, smoothDelayMs)
        }
    }

    private fun detachOverlay() {
        if (!isOverlayAttached && overlayView == null) return
        try {
            overlayView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
            lifecycleOwner?.onDestroy()
            overlayScope?.cancel()
            Log.d(TAG, "Voice assistant overlay removed from WindowManager")
        } catch (e: Throwable) {
            Log.w(TAG, "Error removing overlay view", e)
        } finally {
            overlayView = null
            lifecycleOwner = null
            overlayScope = null
            isOverlayAttached = false
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        detachOverlay()
    }
}

/**
 * Lightweight LifecycleOwner for hosting Jetpack Compose in a WindowManager overlay
 * outside of an Activity.
 */
private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
