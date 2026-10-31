package com.synfusion.snipnote.data.backup

import com.synfusion.snipnote.util.TextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun sampleFile(
        notes: List<BackupNote> = listOf(
            BackupNote(
                title = "Prompt",
                content = "Act as a reviewer",
                type = "prompt",
                category = "AI Prompts",
                tags = listOf("work", "review"),
                createdAt = 1_700_000_000_000,
                updatedAt = 1_700_000_001_000,
            ),
            BackupNote(
                title = "Snippet",
                content = "fun main() {}\n",
                type = "code",
                createdAt = 1_700_000_002_000,
                updatedAt = 1_700_000_003_000,
            ),
        ),
        categories: List<BackupCategory> = listOf(BackupCategory("AI Prompts")),
    ) = BackupCodec.encode(
        BackupFile(
            exportedAt = 1_700_000_010_000,
            categories = categories,
            notes = notes,
        ),
    )

    @Test
    fun `valid export is accepted and counts are correct`() {
        val result = BackupCodec.prepare(sampleFile(), "backup.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Ready)
        val preview = (result as ImportPreparation.Ready).preview
        assertEquals(2, preview.notes.size)
        assertEquals(2, preview.newCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals("backup.json", preview.fileName)
        assertFalse(preview.includesSecureContent)
    }

    @Test
    fun `unicode and whitespace survive a full encode and prepare cycle`() {
        val content = "  leading\n\tindented\n\n日本語 🚀\ntrailing  \n"
        val json = sampleFile(
            notes = listOf(
                BackupNote(title = "Unicode", content = content, type = "note", createdAt = 1, updatedAt = 1),
            ),
        )
        val preview = (BackupCodec.prepare(json, "u.json", emptySet(), emptySet()) as ImportPreparation.Ready).preview
        assertEquals(content, preview.notes.single().note.content)
        assertEquals("Unicode", preview.notes.single().note.title)
    }

    @Test
    fun `duplicates are detected by content fingerprint`() {
        val note = BackupNote(title = "Same", content = "Same body", type = "note")
        val fingerprints = setOf(TextUtils.contentFingerprint("Same", "Same body"))
        val preview = (BackupCodec.prepare(
            sampleFile(notes = listOf(note)),
            "d.json",
            fingerprints,
            emptySet(),
        ) as ImportPreparation.Ready).preview
        assertEquals(1, preview.duplicateCount)
        assertEquals(0, preview.newCount)
    }

    @Test
    fun `malformed json is rejected with a friendly message`() {
        val result = BackupCodec.prepare("{ not json", "broken.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Rejected)
        assertTrue((result as ImportPreparation.Rejected).message.contains("not valid JSON"))
    }

    @Test
    fun `foreign json is rejected`() {
        val result = BackupCodec.prepare("""{"hello":"world"}""", "other.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Rejected)
        assertTrue((result as ImportPreparation.Rejected).message.contains("not created by SnipNote"))
    }

    @Test
    fun `a newer file version is rejected instead of partially imported`() {
        val json = sampleFile().replace("\"version\": 1", "\"version\": 99")
        val result = BackupCodec.prepare(json, "future.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Rejected)
        assertTrue((result as ImportPreparation.Rejected).message.contains("newer SnipNote version"))
    }

    @Test
    fun `tampered content fails the integrity check`() {
        val json = sampleFile().replace("Act as a reviewer", "Act as something else")
        val result = BackupCodec.prepare(json, "tampered.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Rejected)
        assertTrue((result as ImportPreparation.Rejected).message.contains("integrity check"))
    }

    @Test
    fun `empty and blank input is rejected`() {
        assertTrue(BackupCodec.prepare(null, "x.json", emptySet(), emptySet()) is ImportPreparation.Rejected)
        assertTrue(BackupCodec.prepare("   ", "x.json", emptySet(), emptySet()) is ImportPreparation.Rejected)
    }

    @Test
    fun `a file with no notes is rejected`() {
        val json = BackupCodec.encode(BackupFile(exportedAt = 1, notes = emptyList()))
        val result = BackupCodec.prepare(json, "empty.json", emptySet(), emptySet())
        assertTrue(result is ImportPreparation.Rejected)
    }

    @Test
    fun `unknown type is normalised to note and flagged`() {
        val json = sampleFile(
            notes = listOf(BackupNote(title = "Weird", content = "content", type = "hologram")),
        )
        val preview = (BackupCodec.prepare(json, "w.json", emptySet(), emptySet()) as ImportPreparation.Ready).preview
        assertEquals("note", preview.notes.single().normalizedType)
        assertTrue(preview.warnings.any { it.contains("Unknown type") })
    }

    @Test
    fun `empty notes are skipped but do not fail the import`() {
        val json = sampleFile(
            notes = listOf(
                BackupNote(title = "", content = ""),
                BackupNote(title = "Kept", content = "body"),
            ),
        )
        val preview = (BackupCodec.prepare(json, "s.json", emptySet(), emptySet()) as ImportPreparation.Ready).preview
        assertEquals(1, preview.notes.size)
        assertEquals("Kept", preview.notes.single().note.title)
    }

    @Test
    fun `secure exports are flagged so the preview can warn`() {
        val json = BackupCodec.encode(
            BackupFile(
                exportedAt = 1,
                includesSecureContent = true,
                notes = listOf(BackupNote(title = "Private", content = "secret", secure = true)),
            ),
        )
        val preview = (BackupCodec.prepare(json, "safe.json", emptySet(), emptySet()) as ImportPreparation.Ready).preview
        assertTrue(preview.includesSecureContent)
        assertEquals(1, preview.secureCount)
        assertTrue(preview.warnings.any { it.contains("plain, unencrypted JSON") })
    }

    @Test
    fun `tags are normalised during preparation`() {
        val json = sampleFile(
            notes = listOf(BackupNote(title = "Tags", content = "body", tags = listOf("#Kotlin", " kotlin ", "Android"))),
        )
        val preview = (BackupCodec.prepare(json, "t.json", emptySet(), emptySet()) as ImportPreparation.Ready).preview
        assertEquals(listOf("Kotlin", "Android"), preview.notes.single().note.tags)
    }

    @Test
    fun `checksum changes when any note field changes`() {
        val a = BackupNote(title = "a", content = "b", type = "note")
        val b = a.copy(title = "a2")
        assertTrue(BackupCodec.checksum(listOf(a)) != BackupCodec.checksum(listOf(b)))
    }
}
