package com.synfusion.snipnote

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.util.ClipboardService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clipboard = ClipboardService(context)

    /**
     * Android 10+ only allows the focused app to *read* the clipboard, so the read-back is
     * asserted when the platform permits it. The copy call itself must always succeed.
     */
    private fun readBack(): String? = runCatching {
        context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text?.toString()
    }.getOrNull()

    @Test
    fun copiedContentIsByteForByteIdentical() {
        val text = "  leading spaces\n\tTabbed indentation\n\nUnicode: 日本語 🚀\nTrailing   \n"
        assertTrue(clipboard.copy("SnipNote test", text))
        readBack()?.let { assertEquals(text, it) }
    }

    @Test
    fun longContentIsNotTruncated() {
        val text = (1..2_000).joinToString("\n") { "line $it – payload" }
        assertTrue(clipboard.copy("Long", text))
        readBack()?.let { assertEquals(text.length, it.length); assertEquals(text, it) }
    }

    @Test
    fun sensitiveFlagDoesNotAlterTheCopiedText() {
        val text = "sk-secret-value"
        assertTrue(clipboard.copy("Sensitive", text, sensitive = true))
        readBack()?.let { assertEquals(text, it) }
    }
}
