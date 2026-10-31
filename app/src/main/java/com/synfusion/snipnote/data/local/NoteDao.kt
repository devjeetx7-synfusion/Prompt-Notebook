package com.synfusion.snipnote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /**
     * The only list query used by ordinary screens. `isSecure = 0` is hard-coded so Safe
     * content can never leak into Home, Browse, Search or a category listing.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE isSecure = 0
          AND (:typeKey IS NULL OR type = :typeKey)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:tagId IS NULL OR id IN (SELECT noteId FROM note_tag_cross_ref WHERE tagId = :tagId))
          AND (:favoritesOnly = 0 OR isFavorite = 1)
          AND (:pinnedOnly = 0 OR isPinned = 1)
          AND (:query = '' OR title LIKE :query ESCAPE '\' OR content LIKE :query ESCAPE '\')
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeVisibleNotes(
        query: String,
        typeKey: String?,
        categoryId: Long?,
        tagId: Long?,
        favoritesOnly: Int,
        pinnedOnly: Int,
    ): Flow<List<NoteWithTags>>

    /** Full Safe list. Only reachable through [com.synfusion.snipnote.data.repository.SafeNoteRepository]. */
    @Transaction
    @Query("SELECT * FROM notes WHERE isSecure = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeSecureNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND isSecure = 0")
    suspend fun findVisibleById(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND isSecure = 0")
    fun observeVisibleById(id: Long): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND isSecure = 1")
    suspend fun findSecureById(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id AND isSecure = 1")
    fun observeSecureById(id: Long): Flow<NoteWithTags?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(notes: List<NoteEntity>): List<Long>

    @Update
    suspend fun update(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** Re-assignment policy when a category is deleted: notes are moved, never dropped. */
    @Query("UPDATE notes SET categoryId = :targetCategoryId, updatedAt = :now WHERE categoryId = :sourceCategoryId")
    suspend fun reassignCategory(sourceCategoryId: Long, targetCategoryId: Long?, now: Long): Int

    @Query("UPDATE notes SET categoryId = NULL, updatedAt = :now WHERE categoryId = :sourceCategoryId")
    suspend fun clearCategory(sourceCategoryId: Long, now: Long): Int

    @Query("UPDATE notes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean): Int

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean): Int

    @Transaction
    @Query("SELECT * FROM notes WHERE isSecure = 0 ORDER BY updatedAt DESC")
    suspend fun exportVisibleNotes(): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM notes WHERE isSecure = 1 ORDER BY updatedAt DESC")
    suspend fun exportSecureNotes(): List<NoteWithTags>

    @Query("SELECT contentHash FROM notes WHERE isSecure = 0 AND contentHash IS NOT NULL")
    suspend fun visibleContentHashes(): List<String>

    @Query("SELECT id FROM notes WHERE isSecure = 0 AND contentHash = :hash LIMIT 1")
    suspend fun findVisibleIdByHash(hash: String): Long?

    @Query("SELECT COUNT(*) FROM notes WHERE isSecure = 0")
    fun observeVisibleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isSecure = 1")
    fun observeSecureCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isSecure = 0 AND isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE isSecure = 0 AND isPinned = 1")
    fun observePinnedCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM notes WHERE isSecure = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAll(): Int

    @Query("DELETE FROM notes WHERE isSecure = 0")
    suspend fun deleteAllVisible(): Int
}
