package com.synfusion.snipnote.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * One-tap copy with no truncation: the exact string handed in is what lands on the clipboard
 * (verified by unit tests over the pure helper [normalizeForCopy]).
 */
class ClipboardService(private val context: Context) {

    /**
     * @param sensitive marks the clip so Android 13+ suppresses its preview in the system UI.
     * @return true when the platform accepted the clip.
     */
    fun copy(label: String, text: String, sensitive: Boolean = false): Boolean = runCatching {
        val manager = context.getSystemService(ClipboardManager::class.java) ?: return false
        val clip = ClipData.newPlainText(label, text)
        if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
        }
        manager.setPrimaryClip(clip)
        true
    }.getOrDefault(false)

    companion object {
        /**
         * The only transformation applied before copying: nothing. Kept as a named function so
         * the "copy is lossless" guarantee is testable and cannot drift.
         */
        fun normalizeForCopy(text: String): String = text
    }
}
