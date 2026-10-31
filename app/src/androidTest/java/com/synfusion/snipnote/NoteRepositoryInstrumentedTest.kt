package com.synfusion.snipnote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.data.repository.MutationResult
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.CategoryDeletionPolicy
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.util.TestGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryInstrumentedTest {

    private lateinit var graph: TestGraph

    @Before
    fun setUp() {
        graph = TestGraph(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = graph.close()

    private suspend fun createNote(
        title: String,
        content: String = "body of $title",
        type: NoteType = NoteType.NOTE,
        categoryId: Long? = null,
        tags: List<String> = emptyList(),
        favorite: Boolean = false,
        pinned: Boolean = false,
    ): Long = graph.notes.save(
        NoteDraft(
            title = title,
            content = content,
            type = type,
            categoryId = categoryId,
            tags = tags,
            isFavorite = favorite,
            isPinned = pinned,
        ),
    )

    @Test
    fun createReadUpdateDelete() = runTest {
        val id = createNote("Alpha", content = "first body")
        val created = graph.notes.getNote(id)
        assertNotNull(created)
        assertEquals("Alpha", created!!.title)
        assertEquals("first body", created.content)

        graph.notes.save(
            NoteDraft(id = id, title = "Alpha v2", content = "second body", type = NoteType.CODE),
        )
        val updated = graph.notes.getNote(id)!!
        assertEquals("Alpha v2", updated.title)
        assertEquals("second body", updated.content)
        assertEquals(NoteType.CODE, updated.type)
        assertEquals(created.createdAt, updated.createdAt)

        assertTrue(graph.notes.delete(id))
        assertNull(graph.notes.getNote(id))
    }

    @Test
    fun multilineUnicodeAndWhitespaceArePreserved() = runTest {
        val content = "  line one\n\tline two\n\n  日本語 🚀  \n"
        val id = createNote("Unicode", content = content)
        assertEquals(content, graph.notes.getNote(id)!!.content)
    }

    @Test
    fun emptyContentWithTitleIsAllowedButFullyBlankIsRejectedByEditor() = runTest {
        val id = createNote("Only a title", content = "")
        assertEquals("Only a title", graph.notes.getNote(id)!!.title)
    }

    @Test
    fun notesAreObservedInPinnedThenUpdatedOrder() = runTest {
        createNote("old")
        val newer = createNote("newer")
        val pinned = createNote("pinned", pinned = true)
        val list = graph.notes.observeNotes(NoteFilter()).first()
        assertEquals("pinned", list.first().title)
        assertEquals(3, list.size)
        assertEquals(pinned, list.first().id)
        assertTrue(list.map { it.id }.contains(newer))
    }

    @Test
    fun searchMatchesTitleAndContent() = runTest {
        createNote("Kotlin coroutines", content = "structured concurrency")
        createNote("Groceries", content = "buy oat milk and a banananananana")

        val byTitle = graph.notes.observeNotes(NoteFilter(query = "kotlin")).first()
        assertEquals(1, byTitle.size)
        assertEquals("Kotlin coroutines", byTitle.first().title)

        val byContent = graph.notes.observeNotes(NoteFilter(query = "oat milk")).first()
        assertEquals(1, byContent.size)
    }

    @Test
    fun searchTreatsSqlWildcardsLiterally() = runTest {
        createNote("Fifty percent", content = "100% complete")
        createNote("Other", content = "nothing here")
        val results = graph.notes.observeNotes(NoteFilter(query = "%")).first()
        assertEquals(1, results.size)
        assertEquals("Fifty percent", results.first().title)
    }

    @Test
    fun filtersByTypeCategoryTagFavoriteAndPinned() = runTest {
        val category = (graph.categories.create("Work") as MutationResult.Success).id
        val taggedId = createNote("Tagged", tags = listOf("urgent", "later"), categoryId = category)
        createNote("Prompt", type = NoteType.PROMPT, favorite = true)
        createNote("Pinned", pinned = true)

        assertEquals(1, graph.notes.observeNotes(NoteFilter(type = NoteType.PROMPT)).first().size)
        assertEquals(1, graph.notes.observeNotes(NoteFilter(categoryId = category)).first().size)
        assertEquals(1, graph.notes.observeNotes(NoteFilter(favoritesOnly = true)).first().size)
        assertEquals(1, graph.notes.observeNotes(NoteFilter(pinnedOnly = true)).first().size)

        val tags = graph.notes.getNote(taggedId)!!.tags
        assertEquals(listOf("later", "urgent"), tags.map { it.name })
        assertEquals(1, graph.notes.observeNotes(NoteFilter(tagId = tags.first().id)).first().size)
    }

    @Test
    fun tagsAreReplacedNotAccumulatedOnUpdate() = runTest {
        val id = createNote("Tagged", tags = listOf("one", "two"))
        graph.notes.save(NoteDraft(id = id, title = "Tagged", content = "body of Tagged", tags = listOf("three")))
        assertEquals(listOf("three"), graph.notes.getNote(id)!!.tags.map { it.name })
    }

    @Test
    fun sortOrdersAreApplied() = runTest {
        createNote("Bravo")
        createNote("Alpha")
        val byTitle = graph.notes.observeNotes(NoteFilter(sort = SortOrder.TITLE_ASC)).first()
        assertEquals(listOf("Alpha", "Bravo"), byTitle.map { it.title })
    }

    @Test
    fun bulkOperationsAffectOnlySelectedRows() = runTest {
        val a = createNote("A")
        val b = createNote("B")
        val c = createNote("C")
        graph.notes.setFavorite(listOf(a, b), true)
        assertEquals(2, graph.notes.observeNotes(NoteFilter(favoritesOnly = true)).first().size)
        graph.notes.setPinned(listOf(c), true)
        assertEquals(1, graph.notes.observeNotes(NoteFilter(pinnedOnly = true)).first().size)

        assertEquals(2, graph.notes.deleteMany(listOf(a, b)))
        assertEquals(1, graph.notes.observeNotes(NoteFilter()).first().size)
        assertNotNull(graph.notes.getNote(c))
    }

    @Test
    fun deletingACategoryMovesItsNotesAndKeepsThem() = runTest {
        val work = (graph.categories.create("Work") as MutationResult.Success).id
        val archive = (graph.categories.create("Archive") as MutationResult.Success).id
        val id = createNote("Meeting", categoryId = work)

        val result = graph.categories.delete(work, CategoryDeletionPolicy.MoveTo(archive))
        assertTrue(result.deleted)
        assertEquals(1, result.reassignedCount)
        assertEquals(archive, graph.notes.getNote(id)!!.category?.id)
    }

    @Test
    fun deletingACategoryCanLeaveNotesUncategorized() = runTest {
        val work = (graph.categories.create("Work") as MutationResult.Success).id
        val id = createNote("Meeting", categoryId = work)
        val result = graph.categories.delete(work, CategoryDeletionPolicy.Uncategorize)
        assertTrue(result.deleted)
        assertNull(graph.notes.getNote(id)!!.category)
        assertNotNull(graph.notes.getNote(id))
    }

    @Test
    fun defaultCategoriesAreSeededExactlyOnce() = runTest {
        graph.categories.ensureDefaults()
        val first = graph.categories.observeCategories().first()
        assertEquals(7, first.size)
        assertEquals(
            listOf("AI Prompts", "Coding", "Writing", "Study", "Business", "Links", "Personal"),
            first.map { it.name },
        )
        graph.categories.ensureDefaults()
        assertEquals(7, graph.categories.observeCategories().first().size)
    }

    @Test
    fun duplicateCategoryNamesAreRejected() = runTest {
        assertTrue(graph.categories.create("Reading") is MutationResult.Success)
        val duplicate = graph.categories.create("reading")
        assertTrue(duplicate is MutationResult.Error)
    }

    @Test
    fun categoryCountsIgnoreNothingAndTrackMoves() = runTest {
        val work = (graph.categories.create("Work") as MutationResult.Success).id
        createNote("One", categoryId = work)
        createNote("Two", categoryId = work)
        val counts = graph.categories.observeCategoriesWithCounts().first()
        assertEquals(2, counts.first { it.category.id == work }.itemCount)
        val other = (graph.categories.create("Other") as MutationResult.Success).id
        graph.categories.delete(work, CategoryDeletionPolicy.MoveTo(other))
        val after = graph.categories.observeCategoriesWithCounts().first()
        assertEquals(2, after.first { it.category.id == other }.itemCount)
        assertFalse(after.any { it.category.id == work })
    }
}
