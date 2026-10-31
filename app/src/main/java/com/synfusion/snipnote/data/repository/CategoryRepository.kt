package com.synfusion.snipnote.data.repository

import androidx.room.withTransaction
import com.synfusion.snipnote.data.local.CategoryDao
import com.synfusion.snipnote.data.local.CategoryEntity
import com.synfusion.snipnote.data.local.NoteDao
import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.CategoryIcons
import com.synfusion.snipnote.domain.model.DefaultCategoryColors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The seven categories created on first launch. Users may rename, recolour or delete them. */
object DefaultCategories {
    data class Seed(val name: String, val iconKey: String, val color: Long)

    val seeds: List<Seed> = listOf(
        Seed("AI Prompts", CategoryIcons.PROMPT, DefaultCategoryColors.PROMPT),
        Seed("Coding", CategoryIcons.CODE, DefaultCategoryColors.CODE),
        Seed("Writing", CategoryIcons.WRITING, DefaultCategoryColors.WRITING),
        Seed("Study", CategoryIcons.STUDY, DefaultCategoryColors.STUDY),
        Seed("Business", CategoryIcons.BUSINESS, DefaultCategoryColors.BUSINESS),
        Seed("Links", CategoryIcons.LINKS, DefaultCategoryColors.LINKS),
        Seed("Personal", CategoryIcons.PERSONAL, DefaultCategoryColors.PERSONAL),
    )
}

class CategoryRepository(
    private val database: SnipNoteDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val categoryDao: CategoryDao = database.categoryDao()
    private val noteDao: NoteDao = database.noteDao()

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll()
            .map { rows -> rows.map(CategoryEntity::toDomain) }
            .flowOn(ioDispatcher)

    fun observeCategoriesWithCounts(): Flow<List<CategoryWithCount>> =
        combine(categoryDao.observeAll(), categoryDao.observeCountsByCategory()) { categories, counts ->
            val countById = counts.filter { it.categoryId != null }.associate { it.categoryId!! to it.itemCount }
            categories.map { entity ->
                CategoryWithCount(category = entity.toDomain(), itemCount = countById[entity.id] ?: 0)
            }
        }.flowOn(ioDispatcher)

    fun observeCategory(id: Long): Flow<Category?> =
        categoryDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    /** Number of visible (non-Safe) items sitting in a category. */
    suspend fun countVisible(categoryId: Long): Int = withContext(ioDispatcher) {
        categoryDao.countVisibleInCategory(categoryId)
    }

    /** Seeds the default set exactly once, when no categories exist yet. */
    suspend fun ensureDefaults(): Unit = withContext(ioDispatcher) {
        if (categoryDao.count() > 0) return@withContext
        val now = clock()
        database.withTransaction {
            DefaultCategories.seeds.forEachIndexed { index, seed ->
                categoryDao.insertIgnoringDuplicates(
                    CategoryEntity(
                        name = seed.name,
                        nameKey = seed.name.lowercase(),
                        iconKey = seed.iconKey,
                        colorArgb = seed.color,
                        sortOrder = index,
                        isDefault = true,
                        createdAt = now,
                    ),
                )
            }
        }
    }

    suspend fun create(
        name: String,
        iconKey: String = CategoryIcons.DEFAULT,
        colorArgb: Long = DefaultCategoryColors.DEFAULT,
    ): MutationResult = withContext(ioDispatcher) {
        val clean = name.trim()
        if (clean.isEmpty()) return@withContext MutationResult.Error("Category name cannot be empty")
        if (clean.length > MAX_NAME_LENGTH) return@withContext MutationResult.Error("Name is too long")
        val key = clean.lowercase()
        if (categoryDao.getByNameKey(key) != null) {
            return@withContext MutationResult.Error("\"$clean\" already exists")
        }
        val nextOrder = (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val id = categoryDao.insert(
            CategoryEntity(
                name = clean,
                nameKey = key,
                iconKey = iconKey,
                colorArgb = colorArgb,
                sortOrder = nextOrder,
                isDefault = false,
                createdAt = clock(),
            ),
        )
        MutationResult.Success(id)
    }

    suspend fun update(
        id: Long,
        name: String,
        iconKey: String,
        colorArgb: Long,
    ): MutationResult = withContext(ioDispatcher) {
        val existing = categoryDao.getById(id)
            ?: return@withContext MutationResult.Error("Category no longer exists")
        val clean = name.trim()
        if (clean.isEmpty()) return@withContext MutationResult.Error("Category name cannot be empty")
        if (clean.length > MAX_NAME_LENGTH) return@withContext MutationResult.Error("Name is too long")
        val key = clean.lowercase()
        val clash = categoryDao.getByNameKey(key)
        if (clash != null && clash.id != id) {
            return@withContext MutationResult.Error("\"$clean\" already exists")
        }
        categoryDao.update(
            existing.copy(name = clean, nameKey = key, iconKey = iconKey, colorArgb = colorArgb),
        )
        MutationResult.Success(id)
    }

    /**
     * Deletes a category and applies an explicit policy to its notes – nothing is discarded.
     * Returns the number of notes that were moved so the UI can report it.
     */
    suspend fun delete(id: Long, policy: CategoryDeletionPolicy): CategoryDeletionResult =
        withContext(ioDispatcher) {
            val existing = categoryDao.getById(id)
                ?: return@withContext CategoryDeletionResult(false, error = "Category no longer exists")
            when (policy) {
                is CategoryDeletionPolicy.MoveTo -> {
                    if (policy.targetCategoryId == id) {
                        return@withContext CategoryDeletionResult(false, error = "Choose a different category")
                    }
                    if (categoryDao.getById(policy.targetCategoryId) == null) {
                        return@withContext CategoryDeletionResult(false, error = "Target category no longer exists")
                    }
                }
                CategoryDeletionPolicy.Uncategorize -> Unit
            }
            val moved = database.withTransaction {
                val affected = when (policy) {
                    is CategoryDeletionPolicy.MoveTo ->
                        noteDao.reassignCategory(id, policy.targetCategoryId, clock())
                    CategoryDeletionPolicy.Uncategorize -> noteDao.clearCategory(id, clock())
                }
                categoryDao.deleteById(existing.id)
                affected
            }
            CategoryDeletionResult(deleted = true, reassignedCount = moved)
        }

    /** Persists a drag-and-drop / manual ordering as dense sort indexes. */
    suspend fun reorder(orderedIds: List<Long>): Unit = withContext(ioDispatcher) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, categoryId ->
                categoryDao.getById(categoryId)?.let { categoryDao.update(it.copy(sortOrder = index)) }
            }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
