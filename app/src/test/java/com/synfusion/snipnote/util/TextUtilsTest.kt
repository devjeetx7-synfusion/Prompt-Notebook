package com.synfusion.snipnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `like escaping neutralises wildcards and escape characters`() {
        assertEquals("100\\% done", TextUtils.escapeLike("100% done"))
        assertEquals("a\\_b", TextUtils.escapeLike("a_b"))
        assertEquals("back\\\\slash", TextUtils.escapeLike("back\\slash"))
        assertEquals("%100\\%%", TextUtils.likeContains("100%"))
    }

    @Test
    fun `fingerprint ignores surrounding whitespace but not content`() {
        val a = TextUtils.contentFingerprint("Title", " Body ")
        val b = TextUtils.contentFingerprint("Title ", "Body")
        val c = TextUtils.contentFingerprint("Title", "Body!")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `fingerprint is stable across calls`() {
        assertEquals(TextUtils.sha256("snipnote"), TextUtils.sha256("snipnote"))
        assertEquals(64, TextUtils.sha256("snipnote").length)
    }

    @Test
    fun `preview keeps short content intact and trims long content`() {
        assertEquals("hello", TextUtils.preview("hello"))
        val long = "x".repeat(300)
        val preview = TextUtils.preview(long, maxChars = 40)
        assertTrue(preview.endsWith("…"))
        assertEquals(41, preview.length)
    }

    @Test
    fun `copy normalisation is lossless`() {
        val original = "line1\n  indented\tTabbed\n\nUnicode: 日本語 🚀\n"
        assertEquals(original, ClipboardService.normalizeForCopy(original))
    }

    @Test
    fun `character count handles surrogate pairs`() {
        assertEquals(1, TextUtils.charCount("🚀"))
        assertEquals(3, TextUtils.charCount("a🚀b"))
    }
}
