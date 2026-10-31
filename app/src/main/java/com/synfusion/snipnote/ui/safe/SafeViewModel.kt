package com.synfusion.snipnote.ui.safe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.data.security.SafeSession
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SafeUiState(
    val unlocked: Boolean = false,
    val notes: List<Note> = emptyList(),
    val secureCount: Int = 0,
    val query: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SafeViewModel(
    private val repository: RoomNoteRepository,
    val session: SafeSession,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val transient = MutableStateFlow(Transient())

    private data class Transient(val message: String? = null, val error: String? = null)

    private val notesFlow = combine(session.isUnlocked, query) { unlocked, text -> unlocked to text }
        .flatMapLatest { (unlocked, text) ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                try {
                    repository.observeSecureNotes(NoteFilter(query = text))
                        .catch { throwable ->
                            transient.update { it.copy(error = "Could not read the Safe: ${throwable.message}") }
                            emit(emptyList())
                        }
                } catch (e: SafeLockedException) {
                    flowOf(emptyList())
                }
            }
        }

    val state: StateFlow<SafeUiState> = combine(
        session.isUnlocked,
        notesFlow,
        repository.observeSecureCount(),
        query,
        transient,
    ) { unlocked, notes, count, text, extra ->
        SafeUiState(
            unlocked = unlocked,
            notes = notes,
            secureCount = count,
            query = text,
            loading = false,
            message = extra.message,
            error = extra.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SafeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onUnlockSucceeded() {
        session.markUnlocked()
        transient.update { it.copy(message = null, error = null) }
    }

    fun onUnlockFailed(message: String) {
        transient.update { it.copy(error = message) }
    }

    fun onUnlockCancelled() = transient.update { it.copy(error = "Authentication cancelled – Safe stays locked") }

    fun lock() {
        session.lock()
        query.value = ""
        transient.update { Transient() }
    }

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        try {
            repository.setFavorite(listOf(note.id), !note.isFavorite)
        } catch (e: SafeLockedException) {
            transient.update { it.copy(error = "Safe locked") }
        }
    }

    fun delete(note: Note, onDeleted: () -> Unit = {}) = viewModelScope.launch {
        try {
            if (repository.delete(note.id)) {
                transient.update { it.copy(message = "Item deleted") }
                onDeleted()
            }
        } catch (e: SafeLockedException) {
            transient.update { it.copy(error = "Safe locked") }
        }
    }

    fun clearMessage() = transient.update { Transient() }

    /** Only ever exposes a count, never titles or content, while locked. */
    fun observeSafeCount(): StateFlow<Int> = repository.observeSecureCount()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
