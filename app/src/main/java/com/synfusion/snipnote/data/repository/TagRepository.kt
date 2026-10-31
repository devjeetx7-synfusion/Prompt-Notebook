package com.synfusion.snipnote.data.repository

import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.data.local.TagCount
import com.synfusion.snipnote.data.local.TagEntity
import com.synfusion.snipnote.domain.model.Tag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TagRepository(
    database: SnipNoteDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val tagDao = database.tagDao()

    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { rows -> rows.map { Tag(it.id, it.name) } }.flowOn(ioDispatcher)

    fun observeTagsWithCounts(): Flow<List<TagWithCount>> =
        tagDao.observeTagCounts().map { rows: List<TagCount> ->
            rows.map { TagWithCount(id = it.id, name = it.name, itemCount = it.itemCount) }
        }.flowOn(ioDispatcher)

    /** Creates the tag if needed and returns its id. */
    suspend fun getOrCreateId(name: String): Long = withContext(ioDispatcher) {
        val clean = Tag.normalize(name)
        if (clean.isEmpty()) return@withContext 0L
        val key = clean.lowercase()
        tagDao.getByNameKey(key)?.id
            ?: tagDao.insertIgnoringDuplicates(TagEntity(name = clean, nameKey = key)).takeIf { it > 0 }
            ?: tagDao.getByNameKey(key)?.id
            ?: 0L
    }
}

data class TagWithCount(val id: Long, val name: String, val itemCount: Int)
