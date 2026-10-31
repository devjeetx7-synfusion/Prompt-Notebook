package com.synfusion.snipnote.domain

import com.synfusion.snipnote.domain.detect.ContentTypeDetector
import com.synfusion.snipnote.domain.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTypeDetectorTest {

    @Test
    fun `single url is detected as a link with high confidence`() {
        val result = ContentTypeDetector.detect("https://example.com/path?query=1#frag")
        assertEquals(NoteType.LINK, result.suggestedType)
        assertTrue(result.isConfident)
    }

    @Test
    fun `www urls are detected as links`() {
        assertEquals(NoteType.LINK, ContentTypeDetector.detect("www.example.org/docs").suggestedType)
    }

    @Test
    fun `text containing a url is not treated as a bare link`() {
        val result = ContentTypeDetector.detect("Read this later: https://example.com is a great write-up about testing.")
        assertFalse(result.suggestedType == NoteType.LINK && result.isConfident)
    }

    @Test
    fun `indented kotlin code is detected as code`() {
        val code = """
            fun greet(name: String): String {
                val greeting = "Hello, ${'$'}name"
                return greeting
            }
        """.trimIndent()
        val result = ContentTypeDetector.detect(code)
        assertEquals(NoteType.CODE, result.suggestedType)
        assertTrue(result.confidence >= 0.5f)
    }

    @Test
    fun `fenced markdown code blocks are detected as code`() {
        val text = "```\nSELECT * FROM notes;\n```"
        assertEquals(NoteType.CODE, ContentTypeDetector.detect(text).suggestedType)
    }

    @Test
    fun `instruction style prompts are detected as prompts`() {
        val text = "Act as a senior Android engineer and review this Kotlin module for security issues."
        val result = ContentTypeDetector.detect(text)
        assertEquals(NoteType.PROMPT, result.suggestedType)
    }

    @Test
    fun `plain prose stays a note`() {
        val text = "Remember to buy milk on the way home."
        val result = ContentTypeDetector.detect(text)
        assertEquals(NoteType.NOTE, result.suggestedType)
        assertFalse(result.isConfident)
    }

    @Test
    fun `empty content is never confidently classified`() {
        val result = ContentTypeDetector.detect("   \n  ")
        assertEquals(NoteType.NOTE, result.suggestedType)
        assertFalse(result.isConfident)
    }

    @Test
    fun `detection is a suggestion and does not depend on the title`() {
        // The detector only sees content; a titleful note with plain text remains a note.
        val result = ContentTypeDetector.detect("Some meeting notes without structure")
        assertTrue(result.confidence <= 0.6f)
    }
}
