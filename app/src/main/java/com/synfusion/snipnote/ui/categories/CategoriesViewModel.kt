package com.synfusion.snipnote.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryDeletionPolicy
import com.synfusion.snipnote.data.repository.CategoryDeletionResult
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.data.repository.MutationResult
import com.synfusion.snipnote.data.repository.TagRepository
import com.synfusion.snipnote.data.repository.TagWithCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryEditorState(
    val isOpen: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val iconKey: String = "folder",
    val colorArgb: Long = 0xFF7567F8,
    val error: String? = null,
    val saving: Boolean = false,
)

data class DeleteCategoryState(
    val category: CategoryWithCount? = null,
    val targetCategoryId: Long? = null,
    val uncategorize: Boolean = false,
    val busy: Boolean = false,
)

data class CategoriesUiState(
    val categories: List<CategoryWithCount> = emptyList(),
    val tags: List<TagWithCount> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null,
    val editor: CategoryEditorState = CategoryEditorState(),
    val deletion: DeleteCategoryState? = null,
) {
    val totalItems: Int get() = categories.sumOf { it.itemCount }
}

class CategoriesViewModel(
    private val repository: CategoryRepository,
    tags: TagRepository,
) : ViewModel() {

    private val editor = MutableStateFlow(CategoryEditorState())
    private val deletion = MutableStateFlow<DeleteCategoryState?>(null)
    private val transient = MutableStateFlow(Transient())

    private data class Transient(val message: String? = null, val error: String? = null)

    val state: StateFlow<CategoriesUiState> = combine(
        repository.observeCategoriesWithCounts().catch { emit(emptyList()) },
        tags.observeTagsWithCounts().catch { emit(emptyList()) },
        editor,
        combine(deletion, transient) { deleteState, extra -> deleteState to extra },
    ) { categories, tagList, editorState, (deleteState, extra) ->
        CategoriesUiState(
            categories = categories,
            tags = tagList,
            loading = false,
            message = extra.message,
            error = extra.error,
            editor = editorState,
            deletion = deleteState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    // region create / edit

    fun openCreate() = editor.update { CategoryEditorState(isOpen = true) }

    fun openEdit(category: CategoryWithCount) = editor.update {
        CategoryEditorState(
            isOpen = true,
            editingId = category.category.id,
            name = category.category.name,
            iconKey = category.category.iconKey,
            colorArgb = category.category.colorArgb,
        )
    }

    fun closeEditor() = editor.update { it.copy(isOpen = false) }

    fun onEditorNameChange(value: String) = editor.update { it.copy(name = value, error = null) }

    fun onEditorIconChange(iconKey: String) = editor.update { it.copy(iconKey = iconKey) }

    fun onEditorColorChange(color: Long) = editor.update { it.copy(colorArgb = color) }

    fun saveCategory() {
        val current = editor.value
        if (current.saving) return
        editor.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = if (current.editingId == null) {
                repository.create(current.name, current.iconKey, current.colorArgb)
            } else {
                repository.update(current.editingId, current.name, current.iconKey, current.colorArgb)
            }
            when (result) {
                is MutationResult.Success -> {
                    editor.update { CategoryEditorState() }
                    transient.update { it.copy(message = if (current.editingId == null) "Category created" else "Category updated") }
                }
                is MutationResult.Error -> editor.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    // endregion

    // region delete

    fun requestDelete(category: CategoryWithCount) = deletion.update {
        DeleteCategoryState(category = category, targetCategoryId = null, uncategorize = false)
    }

    fun cancelDelete() = deletion.update { null }

    fun chooseMoveTarget(targetId: Long?) = deletion.update { it?.copy(targetCategoryId = targetId, uncategorize = false) }

    fun chooseUncategorize() = deletion.update { it?.copy(uncategorize = true, targetCategoryId = null) }

    fun confirmDelete() {
        val current = deletion.value ?: return
        val category = current.category?.category ?: return
        val policy = if (current.uncategorize) {
            CategoryDeletionPolicy.Uncategorize
        } else {
            val target = current.targetCategoryId
            if (target == null) {
                deletion.update { it?.copy(busy = false) }
                transient.update { it.copy(error = "Choose where to move the existing notes first") }
                return
            }
            CategoryDeletionPolicy.MoveTo(target)
        }
        deletion.update { it?.copy(busy = true) }
        viewModelScope.launch {
            val result: CategoryDeletionResult = repository.delete(category.id, policy)
            if (result.deleted) {
                deletion.update { null }
                val suffix = when {
                    result.reassignedCount == 0 -> "Category deleted"
                    policy is CategoryDeletionPolicy.MoveTo -> "Category deleted, ${result.reassignedCount} note(s) moved"
                    else -> "Category deleted, ${result.reassignedCount} note(s) left uncategorized"
                }
                transient.update { it.copy(message = suffix) }
            } else {
                deletion.update { it?.copy(busy = false) }
                transient.update { it.copy(error = result.error ?: "Could not delete the category") }
            }
        }
    }

    fun moveCategory(from: Int, to: Int) {
        val ordered = state.value.categories.map { it.category.id }.toMutableList()
        if (from !in ordered.indices || to !in ordered.indices) return
        ordered.add(to, ordered.removeAt(from))
        viewModelScope.launch { repository.reorder(ordered) }
    }

    // endregion

    fun clearMessage() = transient.update { Transient() }
}
