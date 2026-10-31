package com.synfusion.snipnote.data.security

import com.synfusion.snipnote.domain.model.AutoLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory only unlock state for the Safe workspace.
 *
 * Deliberately *not* persisted: process death or a cold start always returns to Locked, and
 * unlocking again always requires biometric / device-credential authentication.
 */
class SafeSession {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var backgroundedAt: Long? = null

    /**
     * True while a biometric / device-credential prompt is on screen. The prompt itself can
     * push the activity through onStop; suppressing the background timer there prevents an
     * immediate re-lock right after a successful unlock.
     */
    private var authenticating: Boolean = false

    val unlocked: Boolean get() = _isUnlocked.value

    fun beginAuthentication() {
        authenticating = true
    }

    fun endAuthentication() {
        authenticating = false
    }

    fun markUnlocked() {
        backgroundedAt = null
        updating(true)
    }

    private fun updating(value: Boolean) {
        _isUnlocked.value = value
    }

    fun lock() {
        backgroundedAt = null
        updating(false)
    }

    /** Returns true when the caller may read or write Safe content right now. */
    fun requireUnlocked(): Boolean = _isUnlocked.value

    fun onAppBackgrounded(now: Long) {
        if (authenticating) return
        if (_isUnlocked.value) backgroundedAt = now
    }

    /**
     * Re-evaluates the auto-lock timer on return to the foreground.
     * @return true when the session was locked by this call.
     */
    fun onAppForegrounded(now: Long, timeout: AutoLockTimeout): Boolean {
        val since = backgroundedAt ?: return false
        backgroundedAt = null
        if (timeout == AutoLockTimeout.NEVER) return false
        val elapsedSeconds = (now - since) / 1000
        return if (elapsedSeconds >= timeout.seconds) {
            lock()
            true
        } else {
            false
        }
    }
}
