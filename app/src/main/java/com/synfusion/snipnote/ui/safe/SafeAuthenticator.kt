package com.synfusion.snipnote.ui.safe

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.synfusion.snipnote.data.security.SafeSession

sealed interface SafeAuthAvailability {
    data class Ready(val label: String) : SafeAuthAvailability
    data class Unavailable(val reason: String) : SafeAuthAvailability
}

/**
 * Bridges the Safe workspace to the platform authentication prompt.
 *
 * Preference order: biometrics + device credential (API 30+), then biometrics alone. Every
 * path requires a real, user-verified unlock from the OS – there is no app-level bypass.
 */
object SafeAuthenticator {

    fun availability(context: Context): SafeAuthAvailability {
        val manager = BiometricManager.from(context)
        val combos = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                add(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }
            add(BIOMETRIC_WEAK)
            add(BIOMETRIC_STRONG)
        }
        combos.forEach { authenticators ->
            val result = try {
                manager.canAuthenticate(authenticators)
            } catch (e: IllegalArgumentException) {
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
            }
            if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                val label = when {
                    authenticators and DEVICE_CREDENTIAL != 0 -> "Unlock with biometrics or device PIN"
                    else -> "Unlock with biometrics"
                }
                return SafeAuthAvailability.Ready(label)
            }
        }
        val reason = when (manager.canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "This device has no biometric hardware and no screen lock."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No biometric or screen lock is set up. Add one in system settings to unlock Safe."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "A security update is required before biometrics can be used."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is temporarily unavailable."
            else -> "Authentication is unavailable right now."
        }
        return SafeAuthAvailability.Unavailable(reason)
    }

    fun authenticate(
        activity: FragmentActivity,
        session: SafeSession,
        title: String = "Unlock Safe",
        subtitle: String = "Your private items are encrypted on this device",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onCancelled: () -> Unit,
    ) {
        val manager = BiometricManager.from(activity)
        val authenticators = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                manager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ->
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_WEAK
            manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_STRONG
            else -> {
                onFailure(availability(activity).let { if (it is SafeAuthAvailability.Unavailable) it.reason else "Authentication unavailable" })
                return
            }
        }

        session.beginAuthentication()
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    session.endAuthentication()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    session.endAuthentication()
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED,
                        -> onCancelled()
                        else -> onFailure(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // A single non-matching attempt: keep the prompt open, the OS handles retries.
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(authenticators)
            .apply {
                // A negative button may not be combined with a device-credential fallback.
                if (authenticators and DEVICE_CREDENTIAL == 0) setNegativeButtonText("Cancel")
            }
            .build()
        try {
            prompt.authenticate(info)
        } catch (e: Exception) {
            session.endAuthentication()
            onFailure(e.message ?: "The authentication prompt could not be shown")
        }
    }
}
