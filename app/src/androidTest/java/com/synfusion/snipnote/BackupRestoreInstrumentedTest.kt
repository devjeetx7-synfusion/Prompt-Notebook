package com.synfusion.snipnote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synfusion.snipnote.data.backup.BackupRepository
import com.synfusion.snipnote.data.backup.DuplicatePolicy
import com.synfusion.snipnote.data.backup.ExportOutcome
import com.synfusion.snipnote.data.backup.ImportOutcome
import com.synfusion.snipnote.data.backup.ImportPreparation
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.util.TestGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentedTest {

    private lateinit var graph: TestGraph
    private lateinit var backup: BackupRepository

    @Before
    fun setUp() {
        graph = TestGraph(ApplicationProvider.getApplicationContext())
        backup = BackupRepository(
            database = graph.database,
            notes = graph.notes,
            categories = graph.categories,
            session = graph.session,
        )
    }

    @After
    fun tearDown() = graph.close()

    @Test
    fun exportIsValidJsonThatImportsBack() = runTest {
        val category = graph.categories.create("Work").let { (it as com.synfusion.snipnote.data.repository.MutationResult.Success).id }
        graph.notes.save(
            NoteDraft(
                title = "Prompt",
                content = "Act as a reviewer",
                type = NoteType.PROMPT,
                categoryId = category,
                tags = listOf("work"),
                isFavorite = true,
            ),
        )
        val exported = backup.export(includeSecure = false)
        assertTrue(exported is ExportOutcome.Success)
        val json = (exported as ExportOutcome.Success).json
        assertTrue(json.contains("snipnote.backup"))
        assertEquals(1, exported.noteCount)

        val preparation = backup.prepareImport(json, "backup.json")
        assertTrue(preparation is ImportPreparation.Ready)
        val preview = (preparation as ImportPreparation.Ready).preview
        assertEquals(1, preview.notes.size)

        // Same database: the item is a duplicate and gets skipped by default.
        val outcome = backup.applyImport(preview, DuplicatePolicy.SKIP) as ImportOutcome.Success
        assertEquals(0, outcome.imported)
        assertEquals(1, outcome.skipped)
        assertEquals(1, graph.notes.observeNotes(NoteFilter()).first().size)
    }

    @Test
    fun keepBothCreatesASecondRowAndReplaceKeepsOne() = runTest {
        graph.notes.save(NoteDraft(title = "Prompt", content = "body", type = NoteType.PROMPT))
        val json = (backup.export(false) as ExportOutcome.Success).json

        val firstPreview = (backup.prepareImport(json, "b.json") as ImportPreparation.Ready).preview
        val kept = backup.applyImport(firstPreview, DuplicatePolicy.KEEP_BOTH) as ImportOutcome.Success
        assertEquals(1, kept.imported)
        assertEquals(2, graph.notes.observeNotes(NoteFilter()).first().size)

        val secondPreview = (backup.prepareImport(json, "b.json") as ImportPreparation.Ready).preview
        val replaced = backup.applyImport(secondPreview, DuplicatePolicy.REPLACE) as ImportOutcome.Success
        assertEquals(1, replaced.replaced)
        assertEquals(2, graph.notes.observeNotes(NoteFilter()).first().size)
    }

    @Test
    fun safeContentIsExcludedFromStandardExports() = runTest {
        graph.session.markUnlocked()
        graph.notes.save(NoteDraft(title = "Public", content = "visible", type = NoteType.NOTE))
        graph.notes.save(
            NoteDraft(title = "Private", content = "top secret", type = NoteType.NOTE, isSecure = true),
        )
        val exported = backup.export(includeSecure = false) as ExportOutcome.Success
        assertEquals(1, exported.noteCount)
        assertFalse(exported.json.contains("top secret"))
        assertFalse(exported.json.contains("Private"))
    }

    @Test
    fun includingSafeContentRequiresAnUnlockedWorkspaceAndWarns() = runTest {
        graph.session.markUnlocked()
        graph.notes.save(
            NoteDraft(title = "Private", content = "top secret", type = NoteType.NOTE, isSecure = true),
        )
        graph.session.lock()
        assertTrue(backup.export(includeSecure = true) is ExportOutcome.Failure)

        graph.session.markUnlocked()
        val exported = backup.export(includeSecure = true) as ExportOutcome.Success
        assertTrue(exported.json.contains("\"includesSecureContent\": true"))
        val preview = (backup.prepareImport(exported.json, "safe.json") as ImportPreparation.Ready).preview
        assertTrue(preview.includesSecureContent)
        assertTrue(preview.warnings.any { it.contains("unencrypted") })
    }

    @Test
    fun malformedFilesAreRejectedWithoutTouchingData() = runTest {
        graph.notes.save(NoteDraft(title = "Existing", content = "body", type = NoteType.NOTE))
        listOf(
            "",
            "[]",
            "{ \"nodes\": 3 }",
            "{\"format\":\"snipnote.backup\",\"version\":1,\"notes\":\"nope\"}",
            "not json at all",
        ).forEach { raw ->
            val result = backup.prepareImport(raw, "bad.json")
            assertTrue("expected rejection for: $raw", result is ImportPreparation.Rejected)
        }
        assertEquals(1, graph.notes.observeNotes(NoteFilter()).first().size)
    }

    @Test
    fun importCreatesMissingCategoriesAndTags() = runTest {
        graph.notes.save(NoteDraft(title = "Source", content = "body", type = NoteType.NOTE, tags = listOf("alpha")))
        val json = (backup.export(false) as ExportOutcome.Success).json
        graph.database.clearAllTables()

        val preview = (backup.prepareImport(json, "b.json") as ImportPreparation.Ready).preview
        val outcome = backup.applyImport(preview, DuplicatePolicy.SKIP) as ImportOutcome.Success
        assertEquals(1, outcome.imported)
        val restored = graph.notes.observeNotes(NoteFilter()).first().single()
        assertEquals("Source", restored.title)
        assertEquals(listOf("alpha"), restored.tags.map { it.name })
    }
}
