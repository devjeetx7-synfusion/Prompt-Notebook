package com.synfusion.snipnote.data.backup

import kotlinx.serialization.Serializable

/**
 * On-disk JSON schema. Versioned so future releases can migrate old exports.
 *
 * The file is **not encrypted**: exports are plain JSON. Safe items are excluded unless the
 * user explicitly opts in, and the file then says so via [includesSecureContent] so the
 * import preview can warn before anything is restored.
 */
@Serializable
data class BackupFile(
    val format: String = BackupCodec.FORMAT,
    val version: Int = BackupCodec.VERSION,
    val app: String = "SnipNote",
    val exportedAt: Long = 0L,
    val includesSecureContent: Boolean = false,
    val categories: List<BackupCategory> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    /** SHA-256 over the ordered note payload – detects truncated or hand-edited files. */
    val checksum: String = "",
)

@Serializable
data class BackupCategory(
    val name: String,
    val icon: String = "folder",
    val colorArgb: Long = 0xFF7567F8L,
)

@Serializable
data class BackupNote(
    val title: String = "",
    val content: String = "",
    val type: String = "note",
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val secure: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/** How an imported note that already exists locally is treated. */
enum class DuplicatePolicy(val label: String, val description: String) {
    SKIP("Skip duplicates", "Keep my existing note and ignore the imported copy."),
    KEEP_BOTH("Keep both", "Import the note again as a separate copy."),
    REPLACE("Replace existing", "Delete my local copy and use the imported version."),
}

/** A single validated note ready to be written. */
data class PreparedNote(
    val note: BackupNote,
    val normalizedType: String,
    val isDuplicate: Boolean,
    val warnings: List<String>,
)

data class ImportPreview(
    val fileName: String,
    val exportedAt: Long,
    val includesSecureContent: Boolean,
    val notes: List<PreparedNote>,
    val categories: List<BackupCategory>,
    val newCategoryCount: Int,
    val warnings: List<String>,
) {
    val newCount: Int get() = notes.count { !it.isDuplicate }
    val duplicateCount: Int get() = notes.count { it.isDuplicate }
    val secureCount: Int get() = notes.count { it.note.secure }
}

sealed interface ImportPreparation {
    data class Ready(val preview: ImportPreview) : ImportPreparation
    data class Rejected(val message: String) : ImportPreparation
}

sealed interface ImportOutcome {
    data class Success(
        val imported: Int,
        val skipped: Int,
        val replaced: Int,
        val failed: Int,
        val categoriesCreated: Int,
        val tagsCreated: Int,
        val secureImported: Int,
        val warnings: List<String>,
    ) : ImportOutcome

    data class Failure(val message: String) : ImportOutcome
}

sealed interface ExportOutcome {
    data class Success(val json: String, val fileName: String, val noteCount: Int, val includesSecure: Boolean) : ExportOutcome
    data class Failure(val message: String) : ExportOutcome
}
