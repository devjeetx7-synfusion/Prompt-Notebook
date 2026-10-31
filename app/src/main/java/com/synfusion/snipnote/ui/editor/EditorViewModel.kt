package com.synfusion.snipnote.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.domain.detect.ContentTypeDetector
import com.synfusion.snipnote.domain.detect.TypeDetection
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.NOTE,
    val typeManuallyChosen: Boolean = false,
    val detection: TypeDetection? = null,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isSecure: Boolean = false,
    val isNew: Boolean = true,
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val savedId: Long? = null,
    val sessionExpired: Boolean = false,
    val isDirty: Boolean = false,
    val unreadable: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() || content.isNotBlank()
    val characterCount: Int get() = content.length
}

class EditorViewModel(
    private val repository: RoomNoteRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialId: Long = savedStateHandle.get<Long>("noteId")?.takeIf { it > 0 } ?: 0L
    private val secureRequested: Boolean = savedStateHandle.get<Boolean>("secure") ?: false
    private val initialTypeKey: String? = savedStateHandle.get<String>("type")

    private val _state = MutableStateFlow(
        EditorUiState(
            noteId = initialId.takeIf { it > 0 },
            isNew = initialId == 0L,
            type = NoteType.fromStorageKeyOrDefault(initialTypeKey),
            typeManuallyChosen = initialTypeKey != null,
            isSecure = secureRequested,
        ),
    )
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories()
                .catch { emit(emptyList()) }
                .collect { categories -> _state.update { it.copy(categories = categories) } }
        }
        if (initialId > 0) load(initialId)
        else _state.update { it.copy(loading = false) }
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            try {
                val note: Note? = if (secureRequested) repository.getSecureNote(id) else repository.getNote(id)
                if (note == null) {
                    _state.update { it.copy(loading = false, error = "This note could not be found") }
                    return@launch
                }
                _state.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        content = note.content,
                        type = note.type,
                        typeManuallyChosen = true,
                        categoryId = note.category?.id,
                        tags = note.tags.map { tag -> tag.name },
                        isFavorite = note.isFavorite,
                        isPinned = note.isPinned,
                        isSecure = note.isSecure,
                        isNew = false,
                        loading = false,
                        unreadable = !note.isReadable,
                    )
                }
            } catch (e: SafeLockedException) {
                _state.update { it.copy(loading = false, sessionExpired = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not open the editor: ${e.message}") }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value.take(MAX_TITLE), isDirty = true) }
    }

    fun onContentChange(value: String) {
        val detection = ContentTypeDetector.detect(value)
        _state.update { current ->
            val autoType = !current.typeManuallyChosen && detection.isConfident
            current.copy(
                content = value,
                isDirty = true,
                detection = if (detection.suggestedType == current.type) null else detection,
                type = if (autoType) detection.suggestedType else current.type,
            )
        }
    }

    /** Applies the detected type explicitly and remembers that the user accepted a suggestion. */
    fun applySuggestion() {
        _state.update { it.copy(type = it.detection?.suggestedType ?: it.type, typeManuallyChosen = true, detection = null) }
    }

    fun dismissSuggestion() = _state.update { it.copy(detection = null, typeManuallyChosen = true) }

    fun onTypeChange(type: NoteType) = _state.update { it.copy(type = type, typeManuallyChosen = true, detection = null) }

    fun onCategoryChange(categoryId: Long?) = _state.update { it.copy(categoryId = categoryId, isDirty = true) }

    fun onTagInputChange(value: String) = _state.update { it.copy(tagInput = value) }

    fun addTag() {
        _state.update { current ->
            val normalized = Tag.normalize(current.tagInput)
            if (normalized.isEmpty()) return@update current.copy(tagInput = "")
            if (current.tags.size >= Tag.MAX_TAGS_PER_NOTE) {
                return@update current.copy(tagInput = "", error = "Up to ${Tag.MAX_TAGS_PER_NOTE} tags per note")
            }
            if (current.tags.any { it.equals(normalized, ignoreCase = true) }) {
                return@update current.copy(tagInput = "")
            }
            current.copy(tags = current.tags + normalized, tagInput = "", isDirty = true)
        }
    }

    fun removeTag(tag: String) = _state.update {
        it.copy(tags = it.tags.filterNot { existing -> existing.equals(tag, ignoreCase = true) }, isDirty = true)
    }

    fun toggleFavorite() = _state.update { it.copy(isFavorite = !it.isFavorite, isDirty = true) }

    fun togglePinned() = _state.update { it.copy(isPinned = !it.isPinned, isDirty = true) }

    fun toggleSecure() = _state.update { it.copy(isSecure = !it.isSecure, isDirty = true) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave) {
            _state.update { it.copy(error = "Add a title or some content before saving") }
            return
        }
        if (snapshot.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val id = repository.save(
                    NoteDraft(
                        id = snapshot.noteId,
                        title = snapshot.title.trim(),
                        content = snapshot.content,
                        type = snapshot.type,
                        categoryId = snapshot.categoryId,
                        tags = snapshot.tags,
                        isFavorite = snapshot.isFavorite,
                        isPinned = snapshot.isPinned,
                        isSecure = snapshot.isSecure,
                    ),
                )
                _state.update { it.copy(saving = false, savedId = id, isDirty = false, isNew = false, noteId = id) }
            } catch (e: SafeLockedException) {
                _state.update { it.copy(saving = false, sessionExpired = true) }
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(saving = false, error = e.message ?: "This note could not be saved") }
            } catch (e: IllegalStateException) {
                _state.update { it.copy(saving = false, error = e.message ?: "This note could not be saved") }
            }
        }
    }

    companion object {
        const val MAX_TITLE = Note.TITLE_MAX_LENGTH
    }
}
