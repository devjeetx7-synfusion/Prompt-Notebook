package com.synfusion.snipnote.data.repository

import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.NoteType

/** Everything the editor needs to create or update a note. */
data class NoteDraft(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.NOTE,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isSecure: Boolean = false,
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()
}

data class CategoryWithCount(
    val category: Category,
    val itemCount: Int,
)

/** Explicit policy for what happens to notes when their category is deleted. */
sealed interface CategoryDeletionPolicy {
    /** Move every note in the category to [targetCategoryId] (must exist). */
    data class MoveTo(val targetCategoryId: Long) : CategoryDeletionPolicy

    /** Keep the notes but clear their category (they show up under "Uncategorized"). */
    data object Uncategorize : CategoryDeletionPolicy
}

data class CategoryDeletionResult(
    val deleted: Boolean,
    val reassignedCount: Int = 0,
    val error: String? = null,
)

/** Result of a mutation that can be rejected, e.g. a duplicate category name. */
sealed interface MutationResult {
    data class Success(val id: Long = 0L) : MutationResult
    data class Error(val message: String) : MutationResult
}
