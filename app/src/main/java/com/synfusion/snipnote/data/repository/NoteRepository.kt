package com.synfusion.snipnote.data.repository

import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

/** Read/write API for ordinary (non-Safe) content. */
interface NoteRepository {
    fun observeNotes(filter: NoteFilter): Flow<List<Note>>

    fun observeRecent(limit: Int = 6): Flow<List<Note>>

    fun observeNote(id: Long): Flow<Note?>

    suspend fun getNote(id: Long): Note?

    suspend fun save(draft: NoteDraft): Long

    suspend fun delete(id: Long): Boolean

    suspend fun deleteMany(ids: Collection<Long>): Int

    suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean)

    suspend fun setPinned(ids: Collection<Long>, pinned: Boolean)

    fun observeVisibleCount(): Flow<Int>

    fun observeFavoriteCount(): Flow<Int>

    fun observePinnedCount(): Flow<Int>

    fun sortComparator(sort: SortOrder): Comparator<Note>
}
