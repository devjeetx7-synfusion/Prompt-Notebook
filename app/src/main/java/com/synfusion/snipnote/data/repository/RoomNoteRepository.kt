package com.synfusion.snipnote.data.repository

import androidx.room.withTransaction
import com.synfusion.snipnote.data.local.NoteEntity
import com.synfusion.snipnote.data.local.NoteTagCrossRef
import com.synfusion.snipnote.data.local.NoteWithTags
import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.data.local.TagEntity
import com.synfusion.snipnote.data.security.CipherBox
import com.synfusion.snipnote.data.security.CryptoResult
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.data.security.SafeSession
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.domain.model.Tag
import com.synfusion.snipnote.util.TextUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed store for both ordinary and Safe content.
 *
 * Ordinary queries can never reach Safe rows (the SQL hard-codes `isSecure = 0`), and every
 * Safe entry point calls [requireUnlocked] first. Secure title/content are encrypted with the
 * Android Keystore AES-GCM key before they are written; a failed decryption is surfaced as
 * [Note.isReadable] = false instead of deleting or silently blanking the row.
 */
class RoomNoteRepository(
    private val database: SnipNoteDatabase,
    private val cipherBox: CipherBox,
    private val session: SafeSession,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NoteRepository {

    private val noteDao = database.noteDao()
    private val tagDao = database.tagDao()

    // region ordinary reads

    override fun observeNotes(filter: NoteFilter): Flow<List<Note>> {
        val query = if (filter.query.isBlank()) "" else TextUtils.likeContains(filter.query.trim())
        return noteDao.observeVisibleNotes(
            query = query,
            typeKey = filter.type?.storageKey,
            categoryId = filter.categoryId,
            tagId = filter.tagId,
            favoritesOnly = if (filter.favoritesOnly) 1 else 0,
            pinnedOnly = if (filter.pinnedOnly) 1 else 0,
        )
            .map { rows -> rows.map(::toDomain).sortedWith(sortComparator(filter.sort)) }
            .flowOn(ioDispatcher)
    }

    override fun observeRecent(limit: Int): Flow<List<Note>> =
        noteDao.observeRecent(limit)
            .map { rows -> rows.map(::toDomain) }
            .flowOn(ioDispatcher)

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeVisibleById(id)
            .map { row -> row?.let(::toDomain) }
            .flowOn(ioDispatcher)

    override suspend fun getNote(id: Long): Note? = withContext(ioDispatcher) {
        noteDao.findVisibleById(id)?.let(::toDomain)
    }

    override fun observeVisibleCount(): Flow<Int> = noteDao.observeVisibleCount()

    override fun observeFavoriteCount(): Flow<Int> = noteDao.observeFavoriteCount()

    override fun observePinnedCount(): Flow<Int> = noteDao.observePinnedCount()

    // endregion

    // region writes

    override suspend fun save(draft: NoteDraft): Long = withContext(ioDispatcher) {
        require(draft.type in NoteType.entries) { "Unsupported content type" }
        require(draft.title.length <= Note.TITLE_MAX_LENGTH) { "Title is too long" }

        database.withTransaction {
            val now = clock()
            val existing = draft.id?.let { id ->
                noteDao.findVisibleById(id) ?: noteDao.findSecureById(id)
            }
            if (existing?.note?.isSecure == true) requireUnlocked()

            val categoryId = draft.categoryId?.let { id ->
                if (database.categoryDao().getById(id) == null) {
                    throw IllegalArgumentException("The selected category no longer exists")
                }
                id
            }

            val entity = if (existing == null) {
                val secure = draft.isSecure
                if (secure) requireUnlocked()
                NoteEntity(
                    title = if (secure) encrypt(draft.title) else draft.title,
                    content = if (secure) encrypt(draft.content) else draft.content,
                    type = draft.type,
                    categoryId = categoryId,
                    isFavorite = draft.isFavorite,
                    isPinned = draft.isPinned,
                    isSecure = secure,
                    isEncrypted = secure,
                    contentHash = if (secure) null else TextUtils.contentFingerprint(draft.title, draft.content),
                    createdAt = now,
                    updatedAt = now,
                )
            } else {
                val secure = draft.isSecure
                // Moving into the Safe workspace requires an unlocked session; moving out of it
                // re-encrypts or decrypts accordingly, so no plaintext ever lands in a secure row.
                if (secure && !existing.note.isSecure) requireUnlocked()
                NoteEntity(
                    id = existing.note.id,
                    title = if (secure) encrypt(draft.title) else draft.title,
                    content = if (secure) encrypt(draft.content) else draft.content,
                    type = draft.type,
                    categoryId = categoryId,
                    isFavorite = draft.isFavorite,
                    isPinned = draft.isPinned,
                    isSecure = secure,
                    isEncrypted = secure,
                    contentHash = if (secure) null else TextUtils.contentFingerprint(draft.title, draft.content),
                    createdAt = existing.note.createdAt,
                    updatedAt = now,
                )
            }

            val noteId = if (existing == null) noteDao.insert(entity) else {
                noteDao.update(entity)
                entity.id
            }
            replaceTags(noteId, draft.tags)
            noteId
        }
    }

    override suspend fun delete(id: Long): Boolean = withContext(ioDispatcher) {
        val secure = noteDao.findSecureById(id)?.note?.isSecure == true
        if (secure) requireUnlocked()
        noteDao.deleteById(id) > 0
    }

    override suspend fun deleteMany(ids: Collection<Long>): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        var deleted = 0
        database.withTransaction {
            ids.forEach { id ->
                val secure = noteDao.findSecureById(id)?.note?.isSecure == true
                if (secure) requireUnlocked()
                deleted += noteDao.deleteById(id)
            }
        }
        deleted
    }

    override suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean) = withContext(ioDispatcher) {
        requireUnlockedForAnySecure(ids)
        database.withTransaction { ids.forEach { noteDao.setFavorite(it, favorite) } }
        Unit
    }

    override suspend fun setPinned(ids: Collection<Long>, pinned: Boolean) = withContext(ioDispatcher) {
        requireUnlockedForAnySecure(ids)
        database.withTransaction { ids.forEach { noteDao.setPinned(it, pinned) } }
        Unit
    }

    /** Mutating a Safe row through a bulk path still requires an unlocked session. */
    private suspend fun requireUnlockedForAnySecure(ids: Collection<Long>) {
        if (session.requireUnlocked()) return
        if (ids.any { noteDao.findSecureById(it) != null }) requireUnlocked()
    }

    // endregion

    // region Safe workspace

    fun observeSecureNotes(): Flow<List<Note>> {
        requireUnlocked()
        return noteDao.observeSecureNotes()
            .map { rows -> rows.map(::toDomain).sortedWith(sortComparator(SortOrder.UPDATED_DESC)) }
            .flowOn(ioDispatcher)
    }

    /** Filtered Safe list; filtering happens after decryption so ciphertext is never matched. */
    fun observeSecureNotes(filter: NoteFilter): Flow<List<Note>> {
        requireUnlocked()
        return observeSecureNotes().map { notes ->
            notes.filter { note ->
                (!filter.favoritesOnly || note.isFavorite) &&
                    (!filter.pinnedOnly || note.isPinned) &&
                    (filter.type == null || note.type == filter.type) &&
                    (filter.categoryId == null || note.category?.id == filter.categoryId) &&
                    note.matchesQuery(filter.query)
            }
        }
    }

    fun observeSecureNote(id: Long): Flow<Note?> {
        requireUnlocked()
        return noteDao.observeSecureById(id)
            .map { row -> row?.let(::toDomain) }
            .flowOn(ioDispatcher)
    }

    suspend fun getSecureNote(id: Long): Note? = withContext(ioDispatcher) {
        requireUnlocked()
        noteDao.findSecureById(id)?.let(::toDomain)
    }

    fun observeSecureCount(): Flow<Int> = noteDao.observeSecureCount()

    // endregion

    // region mapping

    fun toDomain(row: NoteWithTags): Note = row.toDomainInternal(cipherBox)

    override fun sortComparator(sort: SortOrder): Comparator<Note> = when (sort) {
        SortOrder.UPDATED_DESC -> compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
        SortOrder.CREATED_DESC -> compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt }
        SortOrder.TITLE_ASC -> compareByDescending<Note> { it.isPinned }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle }
        SortOrder.TYPE_ASC -> compareByDescending<Note> { it.isPinned }.thenBy { it.type.ordinal }.thenByDescending { it.updatedAt }
    }

    // endregion

    private fun requireUnlocked() {
        if (!session.requireUnlocked()) throw SafeLockedException()
    }

    private fun encrypt(plaintext: String): String = when (val result = cipherBox.encrypt(plaintext)) {
        is CryptoResult.Success -> result.value
        CryptoResult.KeyUnavailable -> throw IllegalStateException(
            "Secure storage is unavailable on this device. Your content was not saved.",
        )
        is CryptoResult.Failure -> throw IllegalStateException("Encryption failed (${result.reason}). Content was not saved.")
    }

    private suspend fun replaceTags(noteId: Long, rawTags: List<String>) {
        tagDao.clearTagsForNote(noteId)
        val normalized = rawTags.map(Tag::normalize).filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (normalized.isEmpty()) return
        val refs = normalized.map { name ->
            val key = name.lowercase()
            val id = tagDao.getByNameKey(key)?.id
                ?: tagDao.insertIgnoringDuplicates(TagEntity(name = name, nameKey = key)).takeIf { it > 0 }
                ?: tagDao.getByNameKey(key)?.id
                ?: 0L
            NoteTagCrossRef(noteId = noteId, tagId = id)
        }.filter { it.tagId > 0L }
        if (refs.isNotEmpty()) tagDao.insertCrossRefs(refs)
    }
}

/** Shared entity → domain conversion, including Safe decryption semantics. */
internal fun NoteWithTags.toDomainInternal(cipherBox: CipherBox): Note {
    val isSecure = note.isSecure
    var readable = true
    val title: String
    val content: String
    if (isSecure && note.isEncrypted) {
        val decryptedTitle = cipherBox.decrypt(note.title)
        val decryptedContent = cipherBox.decrypt(note.content)
        readable = decryptedTitle is CryptoResult.Success && decryptedContent is CryptoResult.Success
        title = (decryptedTitle as? CryptoResult.Success)?.value ?: ""
        content = (decryptedContent as? CryptoResult.Success)?.value ?: ""
    } else {
        // Non-secure rows, or legacy secure rows stored before encryption was introduced.
        title = note.title
        content = note.content
    }
    return Note(
        id = note.id,
        title = title,
        content = content,
        type = note.type,
        category = category?.toDomain(),
        tags = tags.map { Tag(id = it.id, name = it.name) }.sortedBy { it.name.lowercase() },
        isFavorite = note.isFavorite,
        isPinned = note.isPinned,
        isSecure = isSecure,
        isReadable = readable,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
    )
}

internal fun com.synfusion.snipnote.data.local.CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    isDefault = isDefault,
    createdAt = createdAt,
)
