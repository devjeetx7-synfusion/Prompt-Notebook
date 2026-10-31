package com.synfusion.snipnote.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.data.repository.NoteRepository
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filter: NoteFilter = NoteFilter(),
    val results: List<Note> = emptyList(),
    val categories: List<CategoryWithCount> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val hasQuery: Boolean get() = query.isNotBlank() || filter.isActive
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val repository: NoteRepository,
    categories: CategoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val extras = MutableStateFlow(NoteFilter())
    private val message = MutableStateFlow<String?>(null)

    private val debouncedFilter = combine(query.debounce(220), extras) { text, base -> base.copy(query = text) }
        .distinctUntilChanged()

    private val resultsFlow = debouncedFilter.flatMapLatest { filter ->
        if (!filter.isActive) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.observeNotes(filter).catch { throwable ->
                message.value = "Search failed: ${throwable.message ?: "unknown error"}"
                emit(emptyList())
            }
        }
    }

    val state: StateFlow<SearchUiState> = combine(
        query,
        extras,
        resultsFlow,
        categories.observeCategoriesWithCounts().catch { emit(emptyList()) },
        message,
    ) { text, filter, results, cats, msg ->
        SearchUiState(
            query = text,
            filter = filter.copy(query = text),
            results = results,
            categories = cats,
            searching = false,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setType(type: NoteType?) = extras.update { it.copy(type = type) }

    fun setCategory(categoryId: Long?) = extras.update { it.copy(categoryId = categoryId) }

    fun toggleFavoritesOnly() = extras.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    fun togglePinnedOnly() = extras.update { it.copy(pinnedOnly = !it.pinnedOnly) }

    fun setSort(sort: SortOrder) = extras.update { it.copy(sort = sort) }

    fun clearFilters() = extras.update { NoteFilter(sort = it.sort) }

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        repository.setFavorite(listOf(note.id), !note.isFavorite)
        message.value = if (note.isFavorite) "Removed from favorites" else "Added to favorites"
    }

    fun clearMessage() {
        message.value = null
    }
}
