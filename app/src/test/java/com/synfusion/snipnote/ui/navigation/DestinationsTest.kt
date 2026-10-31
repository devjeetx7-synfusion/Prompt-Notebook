package com.synfusion.snipnote.ui.navigation

import com.synfusion.snipnote.domain.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Route building is plain Kotlin, so it is covered by JVM tests. This pins the argument encoding
 * that the floating "+" and the contextual actions rely on.
 */
class DestinationsTest {

    @Test
    fun `editor route always carries explicit defaults`() {
        assertEquals("editor?noteId=-1&secure=false&type=-", Destinations.editor())
        assertEquals("editor?noteId=7&secure=false&type=-", Destinations.editor(noteId = 7))
        assertEquals("editor?noteId=7&secure=true&type=code", Destinations.editor(noteId = 7, secure = true, type = NoteType.CODE))
    }

    @Test
    fun `detail route encodes the secure flag`() {
        assertEquals("note/12?secure=false", Destinations.detail(12))
        assertEquals("note/12?secure=true", Destinations.detail(12, secure = true))
    }

    @Test
    fun `list route stays filterable by preset category type and tag`() {
        assertEquals("list/all?categoryId=-1&type=-&tagId=-1", Destinations.noteList())
        assertEquals(
            "list/favorites?categoryId=3&type=prompt&tagId=9",
            Destinations.noteList(preset = "favorites", categoryId = 3, type = NoteType.PROMPT, tagId = 9),
        )
    }

    @Test
    fun `only the four chrome destinations are top level`() {
        assertTrue(Destinations.isTopLevel(Destinations.HOME))
        assertTrue(Destinations.isTopLevel(Destinations.CATEGORIES))
        assertTrue(Destinations.isTopLevel(Destinations.SAFE))
        assertTrue(Destinations.isTopLevel(Destinations.SETTINGS))
        assertFalse(Destinations.isTopLevel(Destinations.SEARCH))
        assertFalse(Destinations.isTopLevel(Destinations.EDITOR))
        assertFalse(Destinations.isTopLevel(Destinations.DETAIL))
        assertFalse(Destinations.isTopLevel(null))
    }
}
