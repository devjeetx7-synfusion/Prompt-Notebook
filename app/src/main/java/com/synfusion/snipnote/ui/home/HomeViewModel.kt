package com.synfusion.snipnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.NoteRepository
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.actions.NoteActionDelegate
import com.synfusion.snipnote.ui.actions.NoteActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val items: List<Note> = emptyList(),
    val totalCount: Int = 0,
    val filterType: NoteType? = null,
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val error: String? = null,
)

/**
 * Home is intentionally thin: one list of recent items, one type filter and the contextual
 * actions. There are no dashboard counters and no per-card action buttons.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val notes: NoteRepository,
    categories: CategoryRepository,
    actions: NoteActions,
) : ViewModel() {

    private val filterType = MutableStateFlow<NoteType?>(null)
    private val transient = MutableStateFlow(Transient())

    private data class Transient(val message: String? = null, val error: String? = null)

    private val delegate = NoteActionDelegate(actions, viewModelScope) { message ->
        transient.update { it.copy(message = message) }
    }

    private val itemsFlow = filterType
        .flatMapLatest { type -> notes.observeNotes(NoteFilter(type = type)) }
        .catch { throwable ->
            transient.update { it.copy(error = "Could not load your notes: ${throwable.message ?: "unknown error"}") }
            emit(emptyList())
        }

    val state: StateFlow<HomeUiState> = combine(
        itemsFlow,
        filterType,
        notes.observeVisibleCount().catch { emit(0) },
        categories.observeCategories().catch { emit(emptyList()) },
        transient,
    ) { items, type, count, cats, extra ->
        HomeUiState(
            items = items.take(HOME_ITEM_LIMIT),
            totalCount = count,
            filterType = type,
            categories = cats,
            loading = false,
            message = extra.message,
            error = extra.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setFilter(type: NoteType?) {
        filterType.value = type
    }

    fun copy(note: Note) = delegate.copy(note)

    fun toggleFavorite(note: Note) = delegate.toggleFavorite(note)

    fun moveToCategory(note: Note, categoryId: Long?) = delegate.moveToCategory(note, categoryId)

    fun togglePrivate(note: Note) = delegate.togglePrivate(note)

    fun delete(note: Note) = delegate.delete(note)

    fun clearMessage() = transient.update { Transient() }

    companion object {
        /** Home shows recent items; the full archive lives behind the type filters / search. */
        const val HOME_ITEM_LIMIT = 40
    }
}
