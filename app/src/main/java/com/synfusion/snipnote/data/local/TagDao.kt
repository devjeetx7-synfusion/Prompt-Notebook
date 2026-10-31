package com.synfusion.snipnote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE nameKey = :nameKey")
    suspend fun getByNameKey(nameKey: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TagEntity>

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    /** Tag list with counts restricted to non-Safe notes so badges cannot leak hidden items. */
    @Query(
        """
        SELECT t.id AS id, t.name AS name, COUNT(n.id) AS itemCount
        FROM tags t
        LEFT JOIN note_tag_cross_ref r ON r.tagId = t.id
        LEFT JOIN notes n ON n.id = r.noteId AND n.isSecure = 0
        GROUP BY t.id, t.name
        ORDER BY t.name COLLATE NOCASE ASC
        """,
    )
    fun observeTagCounts(): Flow<List<TagCount>>
}
