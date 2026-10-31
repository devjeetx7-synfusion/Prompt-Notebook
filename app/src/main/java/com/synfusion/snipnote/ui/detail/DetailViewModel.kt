package com.synfusion.snipnote.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.NoteRepository
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val note: Note? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val deleted: Boolean = false,
    val requiresUnlock: Boolean = false,
)

class DetailViewModel(
    private val visibleRepository: NoteRepository,
    private val secureRepository: RoomNoteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val secure: Boolean = savedStateHandle.get<Boolean>("secure") ?: false
    private val transient = MutableStateFlow(Transient())

    private data class Transient(
        val message: String? = null,
        val error: String? = null,
        val deleted: Boolean = false,
        val requiresUnlock: Boolean = false,
    )

    private val noteFlow = try {
        if (secure) secureRepository.observeSecureNote(noteId) else visibleRepository.observeNote(noteId)
    } catch (e: SafeLockedException) {
        transient.update { it.copy(requiresUnlock = true) }
        kotlinx.coroutines.flow.flowOf(null)
    }

    val state: StateFlow<DetailUiState> = combine(noteFlow.catch { throwable ->
        transient.update { it.copy(error = "Could not open this note: ${throwable.message ?: "unknown error"}") }
        emit(null)
    }, transient) { note, extra ->
        DetailUiState(
            note = note,
            loading = false,
            error = extra.error,
            message = extra.message,
            deleted = extra.deleted,
            requiresUnlock = extra.requiresUnlock,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleFavorite() {
        val note = state.value.note ?: return
        viewModelScope.launch {
            visibleRepository.setFavorite(listOf(note.id), !note.isFavorite)
            transient.update { it.copy(message = if (note.isFavorite) "Removed from favorites" else "Added to favorites") }
        }
    }

    fun togglePinned() {
        val note = state.value.note ?: return
        viewModelScope.launch {
            visibleRepository.setPinned(listOf(note.id), !note.isPinned)
            transient.update { it.copy(message = if (note.isPinned) "Unpinned" else "Pinned") }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val note = state.value.note ?: return
        viewModelScope.launch {
            val deleted = visibleRepository.delete(note.id)
            if (deleted) {
                transient.update { it.copy(deleted = true) }
                onDeleted()
            } else {
                transient.update { it.copy(error = "Could not delete this note") }
            }
        }
    }

    /** Moves the item into or out of the Safe workspace (requires an unlocked Safe). */
    fun togglePrivate() {
        val note = state.value.note ?: return
        viewModelScope.launch {
            val target = !note.isSecure
            val message = try {
                visibleRepository.save(
                    NoteDraft(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        type = note.type,
                        categoryId = note.category?.id,
                        tags = note.tags.map { it.name },
                        isFavorite = note.isFavorite,
                        isPinned = note.isPinned,
                        isSecure = target,
                    ),
                )
                if (target) "Moved to Safe" else "Moved out of Safe"
            } catch (e: com.synfusion.snipnote.data.security.SafeLockedException) {
                "Unlock the Safe workspace to change where this item is stored"
            } catch (e: Exception) {
                "Could not move the item: ${e.message ?: "unknown error"}"
            }
            transient.update { it.copy(message = message) }
        }
    }

    fun setMessage(message: String) = transient.update { it.copy(message = message) }

    fun clearMessage() = transient.update { it.copy(message = null) }
}
