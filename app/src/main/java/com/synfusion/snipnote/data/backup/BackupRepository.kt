package com.synfusion.snipnote.data.backup

import androidx.room.withTransaction
import com.synfusion.snipnote.data.local.CategoryEntity
import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.data.local.TagEntity
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.SafeSession
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.Tag
import com.synfusion.snipnote.util.TextUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JSON export/import orchestration.
 *
 * Safety rules implemented here:
 *  * Safe rows are excluded unless the caller explicitly opts in **and** the workspace is unlocked.
 *  * Import is two-phase: [prepareImport] only validates and produces a preview; nothing is
 *    written until [applyImport] is called with that preview.
 *  * Existing notes are never silently overwritten – the [DuplicatePolicy] is explicit.
 */
class BackupRepository(
    private val database: SnipNoteDatabase,
    private val notes: RoomNoteRepository,
    private val categories: CategoryRepository,
    private val session: SafeSession,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val noteDao = database.noteDao()
    private val categoryDao = database.categoryDao()
    private val tagDao = database.tagDao()

    suspend fun export(includeSecure: Boolean): ExportOutcome = withContext(ioDispatcher) {
        if (includeSecure && !session.isUnlocked.value) {
            return@withContext ExportOutcome.Failure(
                "Unlock the Safe workspace before including private items in an export.",
            )
        }
        val visible = noteDao.exportVisibleNotes().map { notes.toDomain(it) }
        val secure = if (includeSecure) {
            noteDao.exportSecureNotes()
                .map { notes.toDomain(it) }
                .filter { it.isReadable }
        } else {
            emptyList()
        }
        val all = visible + secure
        if (all.isEmpty()) {
            return@withContext ExportOutcome.Failure("There is nothing to export yet.")
        }
        val categoryEntities = categoryDao.getAll()
        val categoryNameById = categoryEntities.associate { it.id to it.name }

        val backupNotes = all.map { note ->
            BackupNote(
                title = note.title,
                content = note.content,
                type = note.type.storageKey,
                category = note.category?.name ?: categoryNameById[note.category?.id],
                tags = note.tags.map { it.name },
                favorite = note.isFavorite,
                pinned = note.isPinned,
                secure = note.isSecure,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
            )
        }
        val usedCategoryIds = all.mapNotNull { it.category?.id }.toSet()
        val backupCategories = categoryEntities
            .filter { it.id in usedCategoryIds }
            .map { BackupCategory(name = it.name, icon = it.iconKey, colorArgb = it.colorArgb) }

        val stamp = java.time.Instant.ofEpochMilli(clock()).toString().replace(":", "-")
        val fileName = "snipnote-backup-$stamp.json"
        val json = BackupCodec.encode(
            BackupFile(
                format = BackupCodec.FORMAT,
                version = BackupCodec.VERSION,
                exportedAt = clock(),
                includesSecureContent = includeSecure,
                categories = backupCategories,
                notes = backupNotes,
            ),
        )
        ExportOutcome.Success(
            json = json,
            fileName = fileName,
            noteCount = backupNotes.size,
            includesSecure = includeSecure,
        )
    }

    suspend fun prepareImport(raw: String?, fileName: String): ImportPreparation = withContext(ioDispatcher) {
        val fingerprints = noteDao.visibleContentHashes().toSet()
        val categoryNames = categoryDao.getAll().map { it.name.lowercase() }.toSet()
        BackupCodec.prepare(raw, fileName, fingerprints, categoryNames, clock())
    }

    suspend fun applyImport(preview: ImportPreview, policy: DuplicatePolicy): ImportOutcome =
        withContext(ioDispatcher) {
            if (preview.secureCount > 0 && !session.isUnlocked.value) {
                return@withContext ImportOutcome.Failure(
                    "This backup contains Safe items. Unlock the Safe workspace and try again.",
                )
            }
            var imported = 0
            var skipped = 0
            var replaced = 0
            var failed = 0
            var categoriesCreated = 0
            var tagsCreated = 0
            var secureImported = 0
            val warnings = mutableListOf<String>()

            // Phase 1: categories, matched by name, created only when missing.
            val categoryIdByName = try {
                database.withTransaction {
                    val map = categoryDao.getAll().associate { it.nameKey to it.id }.toMutableMap()
                    preview.categories.forEach { incoming ->
                        val name = incoming.name.trim()
                        if (name.isEmpty()) return@forEach
                        val key = name.lowercase()
                        if (key !in map) {
                            val nextOrder = (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
                            val id = categoryDao.insertIgnoringDuplicates(
                                CategoryEntity(
                                    name = name,
                                    nameKey = key,
                                    iconKey = incoming.icon,
                                    colorArgb = incoming.colorArgb,
                                    sortOrder = nextOrder,
                                    isDefault = false,
                                    createdAt = clock(),
                                ),
                            )
                            if (id > 0) {
                                map[key] = id
                                categoriesCreated++
                            }
                        }
                    }
                    map
                }
            } catch (e: Exception) {
                return@withContext ImportOutcome.Failure(
                    "Categories could not be prepared (${e.message ?: e.javaClass.simpleName}). No notes were imported.",
                )
            }

            // Phase 2: notes are written one at a time through the normal save path, so Safe
            // encryption and tag handling are identical to the editor. A failure in item N is
            // reported rather than rolling back items 1..N-1 behind the user's back.
            val knownTags = tagDao.getAll().map { it.nameKey }.toMutableSet()
            preview.notes.forEach { prepared ->
                val backupNote = prepared.note
                try {
                    val type = NoteType.fromStorageKeyOrDefault(prepared.normalizedType)
                    val fingerprint = TextUtils.contentFingerprint(backupNote.title, backupNote.content)
                    if (prepared.isDuplicate) {
                        when (policy) {
                            DuplicatePolicy.SKIP -> {
                                skipped++
                                return@forEach
                            }
                            DuplicatePolicy.REPLACE -> {
                                noteDao.findVisibleIdByHash(fingerprint)?.let { existingId ->
                                    noteDao.deleteById(existingId)
                                    replaced++
                                }
                            }
                            DuplicatePolicy.KEEP_BOTH -> Unit
                        }
                    }
                    val newTagCountBefore = knownTags.size
                    notes.save(
                        NoteDraft(
                            title = backupNote.title,
                            content = backupNote.content,
                            type = type,
                            categoryId = backupNote.category?.let { categoryIdByName[it.trim().lowercase()] },
                            tags = backupNote.tags,
                            isFavorite = backupNote.favorite,
                            isPinned = backupNote.pinned,
                            isSecure = backupNote.secure,
                        ),
                    )
                    val currentTags = tagDao.getAll().map { it.nameKey }
                    if (currentTags.size > newTagCountBefore) {
                        tagsCreated += currentTags.size - newTagCountBefore
                    }
                    knownTags.addAll(currentTags)
                    imported++
                    if (backupNote.secure) secureImported++
                } catch (e: com.synfusion.snipnote.data.security.SafeLockedException) {
                    return@withContext ImportOutcome.Failure(
                        "The Safe workspace locked during import. $imported item(s) were imported before this point.",
                    )
                } catch (e: Exception) {
                    failed++
                    warnings += "Item \"${backupNote.title.take(40)}\" could not be imported: ${e.message ?: e.javaClass.simpleName}"
                }
            }

            preview.warnings.forEach { warnings += it }
            ImportOutcome.Success(
                imported = imported,
                skipped = skipped,
                replaced = replaced,
                failed = failed,
                categoriesCreated = categoriesCreated,
                tagsCreated = tagsCreated,
                secureImported = secureImported,
                warnings = warnings.distinct(),
            )
        }
}
