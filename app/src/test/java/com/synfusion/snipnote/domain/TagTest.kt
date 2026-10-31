package com.synfusion.snipnote.domain

import com.synfusion.snipnote.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTest {

    @Test
    fun `normalize trims hashes and collapses whitespace`() {
        assertEquals("machine learning", Tag.normalize("  #machine   learning  "))
        assertEquals("kotlin", Tag.normalize("#kotlin"))
        assertEquals("", Tag.normalize("   #   "))
    }

    @Test
    fun `normalize enforces the maximum length`() {
        val long = "a".repeat(100)
        assertEquals(Tag.MAX_LENGTH, Tag.normalize(long).length)
    }

    @Test
    fun `parse list deduplicates case-insensitively and keeps order`() {
        val parsed = Tag.parseList("Kotlin, kotlin, #Compose ,, android")
        assertEquals(listOf("Kotlin", "Compose", "android"), parsed)
    }

    @Test
    fun `parse list caps the number of tags`() {
        val many = (1..40).joinToString(",") { "tag$it" }
        assertTrue(Tag.parseList(many).size <= Tag.MAX_TAGS_PER_NOTE)
    }
}
