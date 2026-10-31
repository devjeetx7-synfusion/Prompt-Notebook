package com.synfusion.snipnote.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.NoteRepository
import com.synfusion.snipnote.data.repository.SettingsRepository
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.ui.actions.NoteActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val filter: NoteFilter = NoteFilter(),
    val selection: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val preset: String = NoteListPreset.ALL,
    val categories: List<Category> = emptyList(),
) {
    val selecting: Boolean get() = selection.isNotEmpty()
}

object NoteListPreset {
    const val ALL = "all"
    const val FAVORITES = "favorites"
    const val PINNED = "pinned"
    const val RECENT = "recent"
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
    settings: SettingsRepository? = null,
    private val actions: NoteActions? = null,
    categories: CategoryRepository? = null,
) : ViewModel() {

    private val categoryList = MutableStateFlow<List<Category>>(emptyList())

    private val preset: String = savedStateHandle.get<String>("preset") ?: NoteListPreset.ALL
    private val initialCategoryId: Long? = savedStateHandle.get<Long>("categoryId")?.takeIf { it > 0 }
    private val initialType: NoteType? = NoteType.fromStorageKey(savedStateHandle.get<String>("type"))

    private val filter = MutableStateFlow(
        NoteFilter(
            type = initialType,
            categoryId = initialCategoryId,
            favoritesOnly = preset == NoteListPreset.FAVORITES,
            pinnedOnly = preset == NoteListPreset.PINNED,
        ),
    )
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val transient = MutableStateFlow(Transient())

    private data class Transient(val message: String? = null, val error: String? = null)

    /** True once the user picks a sort explicitly; the stored default must not override that. */
    private var sortChosenByUser = false

    init {
        if (categories != null) {
            viewModelScope.launch {
                categories.observeCategories().catch { emit(emptyList()) }.collect { list ->
                    categoryList.value = list
                }
            }
        }
        if (settings != null) {
            viewModelScope.launch {
                val stored = settings.settings.first().defaultSort
                filter.update { current -> if (sortChosenByUser) current else current.copy(sort = stored) }
            }
        }
    }

    private val notesFlow = filter.flatMapLatest { current ->
        repository.observeNotes(current).catch { throwable ->
            transient.update { it.copy(error = "Could not load notes: ${throwable.message ?: "unknown error"}") }
            emit(emptyList())
        }
    }

    val state: StateFlow<NoteListUiState> = combine(
        notesFlow,
        filter,
        selection,
        transient,
        categoryList,
    ) { notes, currentFilter, selected, extra, cats ->
        NoteListUiState(
            notes = notes,
            filter = currentFilter,
            selection = selected,
            loading = false,
            error = extra.error,
            message = extra.message,
            preset = preset,
            categories = cats,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NoteListUiState(filter = filter.value, preset = preset),
    )

    fun onQueryChange(value: String) = filter.update { it.copy(query = value) }

    fun setType(type: NoteType?) = filter.update { it.copy(type = type) }

    fun setSort(sort: SortOrder) {
        sortChosenByUser = true
        filter.update { it.copy(sort = sort) }
    }

    fun setCategoryFilter(categoryId: Long?) = filter.update { it.copy(categoryId = categoryId) }

    fun toggleFavoritesOnly() = filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    fun togglePinnedOnly() = filter.update { it.copy(pinnedOnly = !it.pinnedOnly) }

    fun clearFilters() = filter.update {
        it.copy(query = "", type = null, favoritesOnly = false, pinnedOnly = false)
    }

    fun toggleSelection(id: Long) = selection.update { current ->
        if (id in current) current - id else current + id
    }

    fun clearSelection() = selection.update { emptySet() }

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        repository.setFavorite(listOf(note.id), !note.isFavorite)
        transient.update { it.copy(message = if (note.isFavorite) "Removed from favorites" else "Added to favorites") }
    }

    fun togglePinned(note: Note) = viewModelScope.launch {
        repository.setPinned(listOf(note.id), !note.isPinned)
    }

    fun deleteSelected() = viewModelScope.launch {
        val ids = selection.value
        if (ids.isEmpty()) return@launch
        val deleted = repository.deleteMany(ids)
        selection.update { emptySet() }
        transient.update { it.copy(message = "$deleted note(s) deleted") }
    }

    fun favoriteSelected() = viewModelScope.launch {
        val ids = selection.value
        if (ids.isEmpty()) return@launch
        repository.setFavorite(ids, true)
        selection.update { emptySet() }
        transient.update { it.copy(message = "${ids.size} note(s) added to favorites") }
    }

    fun copy(note: Note) {
        val impl = actions ?: return
        transient.update { it.copy(message = impl.copy(note)) }
    }

    fun moveToCategory(note: Note, categoryId: Long?) = viewModelScope.launch {
        val impl = actions ?: return@launch
        transient.update { it.copy(message = impl.moveToCategory(note, categoryId)) }
    }

    fun togglePrivate(note: Note) = viewModelScope.launch {
        val impl = actions ?: return@launch
        transient.update { it.copy(message = impl.togglePrivate(note)) }
    }

    fun delete(note: Note) = viewModelScope.launch {
        val impl = actions ?: return@launch
        transient.update { it.copy(message = impl.delete(note)) }
    }

    fun clearMessage() = transient.update { Transient() }
}
