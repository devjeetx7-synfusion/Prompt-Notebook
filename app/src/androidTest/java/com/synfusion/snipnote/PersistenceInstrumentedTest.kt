package com.synfusion.snipnote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.MutationResult
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.util.TestGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves data survives a database close/reopen cycle – the emulation of an app restart.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "snipnote-persistence-test.db"

    @Before
    fun clearPreviousRun() {
        context.deleteDatabase(dbName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun notesCategoriesTagsAndSecureItemsSurviveReopen() = runTest {
        // --- first "launch"
        val first = TestGraph(context, dbName)
        first.categories.ensureDefaults()
        val category = (first.categories.create("Restart") as MutationResult.Success).id
        val visibleId = first.notes.save(
            NoteDraft(
                title = "Survives",
                content = "line one\nline two  \n日本語",
                type = NoteType.NOTE,
                categoryId = category,
                tags = listOf("persisted", "restart"),
                isFavorite = true,
                isPinned = true,
            ),
        )
        first.session.markUnlocked()
        val secureId = first.notes.save(
            NoteDraft(title = "Private survives", content = "secret body", type = NoteType.NOTE, isSecure = true),
        )
        first.close()

        // --- second "launch" against the same file
        val second = TestGraph(context, dbName)
        val note = second.notes.getNote(visibleId)!!
        assertEquals("Survives", note.title)
        assertEquals("line one\nline two  \n日本語", note.content)
        assertEquals("Restart", note.category?.name)
        assertEquals(listOf("persisted", "restart"), note.tags.map { it.name })
        assertTrue(note.isFavorite)
        assertTrue(note.isPinned)
        assertEquals(8, second.categories.observeCategories().first().size)

        // Safe rows stay ciphertext until unlocked.
        assertEquals(1, second.notes.observeSecureCount().first())
        second.session.markUnlocked()
        assertEquals("secret body", second.notes.getSecureNote(secureId)!!.content)

        // Ordinary queries still do not see it.
        assertTrue(second.notes.observeNotes(NoteFilter()).first().none { it.isSecure })
        second.close()
    }
}
