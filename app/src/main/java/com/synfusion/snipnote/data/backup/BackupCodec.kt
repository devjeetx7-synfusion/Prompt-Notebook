package com.synfusion.snipnote.data.backup

import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.Tag
import com.synfusion.snipnote.util.TextUtils
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Pure JSON encode/decode + validation. Deliberately free of Android and database
 * dependencies so the whole import validation path is unit-testable on the JVM.
 */
object BackupCodec {

    const val FORMAT = "snipnote.backup"
    const val VERSION = 1
    const val MAX_NOTES = 20_000

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun encode(file: BackupFile): String {
        val withChecksum = file.copy(checksum = checksum(file.notes))
        return json.encodeToString(BackupFile.serializer(), withChecksum)
    }

    /** Detects obviously wrong files before touching the database. */
    fun prepare(
        raw: String?,
        fileName: String,
        existingFingerprints: Set<String>,
        existingCategoryNames: Set<String>,
        now: Long = System.currentTimeMillis(),
    ): ImportPreparation {
        if (raw.isNullOrBlank()) return ImportPreparation.Rejected("The selected file is empty.")
        if (raw.length > MAX_FILE_CHARS) {
            return ImportPreparation.Rejected("This file is larger than the ${MAX_FILE_CHARS / 1_000_000} MB import limit.")
        }

        // Fail fast with a friendly message for non-JSON input.
        val root = try {
            json.parseToJsonElement(raw.trim())
        } catch (e: SerializationException) {
            return ImportPreparation.Rejected("This is not valid JSON. Nothing was imported.")
        } catch (e: IllegalArgumentException) {
            return ImportPreparation.Rejected("This is not valid JSON. Nothing was imported.")
        }
        if (root !is JsonObject) {
            return ImportPreparation.Rejected("Unrecognised backup format. Nothing was imported.")
        }
        val formatField = root["format"]?.jsonPrimitive?.contentOrNullSafe()
        if (formatField != FORMAT) {
            return ImportPreparation.Rejected("This file was not created by SnipNote. Nothing was imported.")
        }
        val version = root["version"]?.jsonPrimitive?.longOrNull
            ?: return ImportPreparation.Rejected("The backup is missing a version number.")
        if (version > VERSION) {
            return ImportPreparation.Rejected("This backup was created by a newer SnipNote version (v$version).")
        }

        val file = try {
            json.decodeFromString(BackupFile.serializer(), raw)
        } catch (e: SerializationException) {
            return ImportPreparation.Rejected("The backup is malformed: ${e.message?.lineSequence()?.firstOrNull() ?: "unreadable fields"}")
        } catch (e: IllegalArgumentException) {
            return ImportPreparation.Rejected("The backup is malformed and could not be read.")
        }

        val warnings = mutableListOf<String>()
        if (file.notes.size > MAX_NOTES) {
            return ImportPreparation.Rejected("This backup contains ${file.notes.size} notes, above the $MAX_NOTES note limit.")
        }
        if (file.checksum.isNotBlank() && file.checksum != checksum(file.notes)) {
            return ImportPreparation.Rejected("The backup failed its integrity check and may be truncated. Nothing was imported.")
        }
        if (file.checksum.isBlank()) warnings += "This file has no checksum; integrity could not be verified."
        if (file.includesSecureContent) {
            warnings += "This backup contains Safe items in plain, unencrypted JSON. Treat the file as sensitive."
        }

        val prepared = mutableListOf<PreparedNote>()
        file.notes.forEachIndexed { index, note ->
            val noteWarnings = mutableListOf<String>()
            val title = note.title.take(500)
            val content = note.content
            if (title.isBlank() && content.isBlank()) {
                noteWarnings += "Item ${index + 1} was empty and will be skipped."
                return@forEachIndexed
            }
            val type = NoteType.fromStorageKey(note.type)
            if (type == null) noteWarnings += "Unknown type \"${note.type}\" was imported as a note."
            if (content.length > MAX_CONTENT_CHARS) {
                noteWarnings += "Item ${index + 1} exceeds $MAX_CONTENT_CHARS characters and will be skipped."
                return@forEachIndexed
            }
            val fingerprint = TextUtils.contentFingerprint(title, content)
            prepared += PreparedNote(
                note = note.copy(
                    title = title,
                    tags = Tag.parseList(note.tags.joinToString(",")),
                    createdAt = if (note.createdAt > 0) note.createdAt else now,
                    updatedAt = if (note.updatedAt > 0) note.updatedAt else now,
                ),
                normalizedType = (type ?: NoteType.NOTE).storageKey,
                isDuplicate = fingerprint in existingFingerprints,
                warnings = noteWarnings,
            )
        }

        if (prepared.isEmpty()) {
            return ImportPreparation.Rejected("This backup does not contain any importable notes.")
        }

        val categoryNames = file.categories.map { it.name }.filter { it.isNotBlank() }
        val newCategories = categoryNames.count { it.lowercase() !in existingCategoryNames }
        val duplicates = prepared.count { it.isDuplicate }
        if (duplicates > 0) warnings += "$duplicates item(s) already exist and follow your duplicate policy."
        prepared.flatMap { it.warnings }.distinct().forEach { warnings += it }

        return ImportPreparation.Ready(
            ImportPreview(
                fileName = fileName,
                exportedAt = file.exportedAt,
                includesSecureContent = file.includesSecureContent,
                notes = prepared,
                categories = file.categories,
                newCategoryCount = newCategories,
                warnings = warnings.distinct(),
            ),
        )
    }

    fun checksum(notes: List<BackupNote>): String {
        val payload = notes.joinToString("\u001E") { note ->
            listOf(
                note.title,
                note.content,
                note.type,
                note.category ?: "",
                note.tags.joinToString(","),
                note.createdAt.toString(),
                note.updatedAt.toString(),
            ).joinToString("\u001F")
        }
        return TextUtils.sha256(payload)
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? = content

    const val MAX_FILE_CHARS = 32 * 1024 * 1024
    const val MAX_CONTENT_CHARS = 1_000_000
}
