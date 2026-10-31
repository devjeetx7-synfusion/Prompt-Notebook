package com.synfusion.snipnote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.util.TestGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeAccessControlInstrumentedTest {

    private lateinit var graph: TestGraph

    @Before
    fun setUp() {
        graph = TestGraph(ApplicationProvider.getApplicationContext())
        graph.session.markUnlocked()
    }

    @After
    fun tearDown() = graph.close()

    private suspend fun createSecureNote(title: String, content: String): Long = graph.notes.save(
        NoteDraft(title = title, content = content, type = NoteType.NOTE, isSecure = true),
    )

    @Test
    fun secureNotesAreExcludedFromEveryOrdinaryQuery() = runTest {
        graph.notes.save(NoteDraft(title = "Visible", content = "public body", type = NoteType.NOTE))
        createSecureNote("Hidden", "private body")

        val all = graph.notes.observeNotes(NoteFilter()).first()
        assertEquals(listOf("Visible"), all.map { it.title })
        assertTrue(graph.notes.observeNotes(NoteFilter(query = "private")).first().isEmpty())
        assertTrue(graph.notes.observeNotes(NoteFilter(query = "Hidden")).first().isEmpty())
        assertTrue(graph.notes.observeRecent(10).first().none { it.isSecure })
        assertEquals(1, graph.notes.observeVisibleCount().first())
        assertEquals(1, graph.notes.observeSecureCount().first())
    }

    @Test
    fun secureContentIsEncryptedAtRest() = runTest {
        val id = createSecureNote("API key", "sk-test-super-secret")
        val raw = graph.database.noteDao().findSecureById(id)!!
        assertTrue(raw.note.isSecure)
        assertTrue(raw.note.isEncrypted)
        assertFalse(raw.note.content.contains("sk-test-super-secret"))
        assertFalse(raw.note.title.contains("API key"))
        assertNull(raw.note.contentHash)

        val decrypted = graph.notes.getSecureNote(id)!!
        assertEquals("sk-test-super-secret", decrypted.content)
        assertTrue(decrypted.isReadable)
    }

    @Test
    fun lockedWorkspaceBlocksEverySecureReadAndWrite() = runTest {
        val id = createSecureNote("Hidden", "private")
        graph.session.lock()

        try {
            graph.notes.observeSecureNotes()
            fail("Expected SafeLockedException while locked")
        } catch (e: SafeLockedException) {
            // expected
        }
        try {
            graph.notes.getSecureNote(id)
            fail("Expected SafeLockedException while locked")
        } catch (e: SafeLockedException) {
            // expected
        }
        try {
            graph.notes.setFavorite(listOf(id), true)
            fail("Expected SafeLockedException while locked")
        } catch (e: SafeLockedException) {
            // expected
        }
        try {
            graph.notes.delete(id)
            fail("Expected SafeLockedException while locked")
        } catch (e: SafeLockedException) {
            // expected
        }
        // The row still exists and is untouched.
        graph.session.markUnlocked()
        assertEquals("private", graph.notes.getSecureNote(id)!!.content)
    }

    @Test
    fun creatingASecureNoteRequiresAnUnlockedSession() = runTest {
        graph.session.lock()
        try {
            createSecureNote("Nope", "should not persist")
            fail("Expected SafeLockedException")
        } catch (e: SafeLockedException) {
            // expected
        }
        assertEquals(0, graph.notes.observeSecureCount().first())
    }

    @Test
    fun movingANoteIntoAndOutOfSafeReencryptsOrDecrypts() = runTest {
        val id = graph.notes.save(NoteDraft(title = "Draft", content = "body", type = NoteType.NOTE))
        graph.notes.save(NoteDraft(id = id, title = "Draft", content = "body", type = NoteType.NOTE, isSecure = true))

        val secureRow = graph.database.noteDao().findSecureById(id)!!
        assertTrue(secureRow.note.isEncrypted)
        assertFalse(secureRow.note.content.contains("body"))

        graph.notes.save(NoteDraft(id = id, title = "Draft", content = "body", type = NoteType.NOTE, isSecure = false))
        val plainRow = graph.database.noteDao().findVisibleById(id)!!
        assertFalse(plainRow.note.isEncrypted)
        assertEquals("body", plainRow.note.content)
    }

    @Test
    fun failingToDecryptMarksTheNoteUnreadableInsteadOfDeletingIt() = runTest {
        val id = createSecureNote("Secret", "very private")
        // Simulate a lost/changed Keystore key by swapping in a cipher with a different key.
        val otherCipher = com.synfusion.snipnote.util.TestCipherBox(ByteArray(32) { (it * 5 + 9).toByte() })
        val otherGraphRepo = com.synfusion.snipnote.data.repository.RoomNoteRepository(
            graph.database,
            otherCipher,
            graph.session,
        )
        val note = otherGraphRepo.getSecureNote(id)!!
        assertFalse(note.isReadable)
        assertTrue(note.content.isEmpty())
        // Row is preserved.
        assertEquals(1, graph.notes.observeSecureCount().first())
    }

    @Test
    fun autoLockTimerLocksAfterTimeoutAndRespectsNever() {
        val session = com.synfusion.snipnote.data.security.SafeSession()
        session.markUnlocked()
        session.onAppBackgrounded(now = 1_000)
        val locked = session.onAppForegrounded(
            now = 1_000 + 61_000,
            timeout = com.synfusion.snipnote.domain.model.AutoLockTimeout.ONE_MINUTE,
        )
        assertTrue(locked)
        assertFalse(session.isUnlocked.value)

        val neverSession = com.synfusion.snipnote.data.security.SafeSession()
        neverSession.markUnlocked()
        neverSession.onAppBackgrounded(now = 0)
        assertFalse(
            neverSession.onAppForegrounded(
                now = 999_999_999,
                timeout = com.synfusion.snipnote.domain.model.AutoLockTimeout.NEVER,
            ),
        )
        assertTrue(neverSession.isUnlocked.value)
    }

    @Test
    fun authenticationSuppressesTheBackgroundTimer() {
        val session = com.synfusion.snipnote.data.security.SafeSession()
        session.markUnlocked()
        session.beginAuthentication()
        session.onAppBackgrounded(now = 0)
        session.endAuthentication()
        // No background timestamp was recorded during auth, so nothing to expire.
        assertFalse(
            session.onAppForegrounded(
                now = 10_000_000,
                timeout = com.synfusion.snipnote.domain.model.AutoLockTimeout.IMMEDIATELY,
            ),
        )
        assertTrue(session.isUnlocked.value)
    }
}
