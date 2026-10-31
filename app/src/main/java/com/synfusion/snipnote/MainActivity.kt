package com.synfusion.snipnote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synfusion.snipnote.domain.model.AutoLockTimeout
import com.synfusion.snipnote.domain.model.UserSettings
import com.synfusion.snipnote.ui.navigation.SnipNoteRoot
import com.synfusion.snipnote.ui.theme.SnipNoteTheme

/**
 * Single-activity host. [FragmentActivity] is required by AndroidX BiometricPrompt.
 */
class MainActivity : FragmentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        container = (application as SnipNoteApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            // Sensitive-recents protection: FLAG_SECURE hides the app from the recents thumbnail
            // and blocks screenshots / screen recording while enabled.
            LaunchedEffect(settings.blockScreenCapture) {
                if (settings.blockScreenCapture) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            AutoLockEffect(
                timeout = settings.autoLockTimeout,
                onBackground = { container.safeSession.onAppBackgrounded(System.currentTimeMillis()) },
                onForeground = { container.safeSession.onAppForegrounded(System.currentTimeMillis(), settings.autoLockTimeout) },
            )

            SnipNoteTheme(themeMode = settings.themeMode) {
                SnipNoteRoot(container = container)
            }
        }
    }
}

/**
 * Applies the configurable Safe auto-lock timer. Registered on the activity lifecycle so it
 * also covers the case where the user leaves via the recents screen.
 */
@androidx.compose.runtime.Composable
private fun AutoLockEffect(
    timeout: AutoLockTimeout,
    onBackground: () -> Unit,
    onForeground: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, timeout) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> onBackground()
                Lifecycle.Event.ON_START -> onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
