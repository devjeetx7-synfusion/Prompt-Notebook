package com.synfusion.snipnote.ui.actions

import com.synfusion.snipnote.data.repository.NoteDraft
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.security.SafeLockedException
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.util.ClipboardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared implementation of the contextual actions offered by the long-press sheet, so the Home,
 * list, search and Safe screens behave identically and none of them re-implement repository or
 * clipboard logic inside a composable.
 */
class NoteActions(
    private val repository: RoomNoteRepository,
    private val clipboard: ClipboardService,
) {

    /** Copies the exact stored text; returns user-facing feedback. */
    fun copy(note: Note): String {
        if (!note.isReadable) return "This item cannot be copied – its encryption key is missing"
        val ok = clipboard.copy(label = note.displayTitle, text = note.content, sensitive = note.isSecure)
        return if (ok) {
            "Copied ${note.content.length} characters"
        } else {
            "Copy failed – the clipboard is unavailable"
        }
    }

    suspend fun toggleFavorite(note: Note): String {
        return try {
            repository.setFavorite(listOf(note.id), !note.isFavorite)
            if (note.isFavorite) "Removed from favorites" else "Added to favorites"
        } catch (e: SafeLockedException) {
            "Unlock the Safe workspace first"
        }
    }

    suspend fun moveToCategory(note: Note, categoryId: Long?): String {
        return try {
            repository.save(
                NoteDraft(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    type = note.type,
                    categoryId = categoryId,
                    tags = note.tags.map { it.name },
                    isFavorite = note.isFavorite,
                    isPinned = note.isPinned,
                    isSecure = note.isSecure,
                ),
            )
            "Moved"
        } catch (e: SafeLockedException) {
            "Unlock the Safe workspace first"
        } catch (e: Exception) {
            "Could not move the note: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * Moves an item into or out of the Safe workspace. Both directions re-encrypt/re-decrypt
     * through the repository, and the Safe must be unlocked first.
     */
    suspend fun togglePrivate(note: Note): String {
        val target = !note.isSecure
        return try {
            repository.save(
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
        } catch (e: SafeLockedException) {
            "Unlock the Safe workspace to change where this item is stored"
        } catch (e: IllegalStateException) {
            if (target) "Unlock the Safe workspace to change where this item is stored" else "Could not move the item"
        } catch (e: Exception) {
            "Could not move the item: ${e.message ?: "unknown error"}"
        }
    }

    suspend fun delete(note: Note): String {
        return try {
            if (repository.delete(note.id)) "Note deleted" else "Could not delete the note"
        } catch (e: SafeLockedException) {
            "Unlock the Safe workspace first"
        }
    }
}

/**
 * Small helper composed into each screen ViewModel: runs an action in the ViewModel scope and
 * reports the result through [onFeedback].
 */
class NoteActionDelegate(
    private val actions: NoteActions,
    private val scope: CoroutineScope,
    private val onFeedback: (String) -> Unit,
) {
    fun copy(note: Note) = onFeedback(actions.copy(note))

    fun toggleFavorite(note: Note) = run { scope.launch { onFeedback(actions.toggleFavorite(note)) } }

    fun moveToCategory(note: Note, categoryId: Long?) =
        run { scope.launch { onFeedback(actions.moveToCategory(note, categoryId)) } }

    fun togglePrivate(note: Note) = run { scope.launch { onFeedback(actions.togglePrivate(note)) } }

    fun delete(note: Note, onDeleted: () -> Unit = {}) = run {
        scope.launch {
            val message = actions.delete(note)
            onFeedback(message)
            if (message == "Note deleted") onDeleted()
        }
    }
}
