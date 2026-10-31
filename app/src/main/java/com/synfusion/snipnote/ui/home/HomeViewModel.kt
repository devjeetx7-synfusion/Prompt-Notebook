package com.synfusion.snipnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.data.repository.NoteRepository
import com.synfusion.snipnote.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class Counts(
    val total: Int = 0,
    val favorites: Int = 0,
    val pinned: Int = 0,
)

data class HomeUiState(
    val greeting: String = "Hello",
    val recent: List<Note> = emptyList(),
    val categories: List<CategoryWithCount> = emptyList(),
    val counts: Counts = Counts(),
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

class HomeViewModel(
    private val notes: NoteRepository,
    categories: CategoryRepository,
    hourProvider: () -> Int = { LocalTime.now().hour },
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    private data class Transient(val message: String? = null, val error: String? = null)

    private val recentFlow = notes.observeRecent(RECENT_LIMIT)
        .catch { throwable ->
            transient.update { it.copy(error = "Could not load recent notes: ${throwable.message ?: "unknown error"}") }
            emit(emptyList())
        }

    private val categoryFlow = categories.observeCategoriesWithCounts()
        .catch { emit(emptyList()) }

    private val countsFlow = combine(
        notes.observeVisibleCount(),
        notes.observeFavoriteCount(),
        notes.observePinnedCount(),
    ) { total, favorites, pinned -> Counts(total, favorites, pinned) }

    private val greeting = com.synfusion.snipnote.util.TimeFormat.greeting(hourProvider())

    val state: StateFlow<HomeUiState> =
        combine(recentFlow, categoryFlow, countsFlow, transient) { recent, categories, counts, extra ->
            HomeUiState(
                greeting = greeting,
                recent = recent,
                categories = categories,
                counts = counts,
                loading = false,
                error = extra.error,
                message = extra.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(greeting = greeting))

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        notes.setFavorite(listOf(note.id), !note.isFavorite)
        transient.update { it.copy(message = if (note.isFavorite) "Removed from favorites" else "Added to favorites") }
    }

    fun togglePinned(note: Note) = viewModelScope.launch {
        notes.setPinned(listOf(note.id), !note.isPinned)
        transient.update { it.copy(message = if (note.isPinned) "Unpinned" else "Pinned to top") }
    }

    fun delete(note: Note) = viewModelScope.launch {
        val deleted = notes.delete(note.id)
        transient.update { it.copy(message = if (deleted) "Note deleted" else "Could not delete note") }
    }

    /** Saves the quick-capture sheet contents in one shot (no separate editor round trip). */
    fun quickCapture(text: String, type: com.synfusion.snipnote.domain.model.NoteType, onSaved: (Long) -> Unit) {
        if (text.isBlank()) {
            transient.update { it.copy(error = "Nothing to capture yet") }
            return
        }
        viewModelScope.launch {
            try {
                val id = notes.save(
                    com.synfusion.snipnote.data.repository.NoteDraft(
                        title = "",
                        content = text,
                        type = type,
                    ),
                )
                transient.update { it.copy(message = "Captured") }
                onSaved(id)
            } catch (e: Exception) {
                transient.update { it.copy(error = "Could not save: ${e.message ?: "unknown error"}") }
            }
        }
    }

    fun clearMessage() = transient.update { it.copy(message = null, error = null) }

    companion object {
        const val RECENT_LIMIT = 6
    }
}
