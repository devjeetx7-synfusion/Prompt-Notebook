package com.synfusion.snipnote.domain.model

/**
 * A saved snippet. [content] is always decrypted plaintext for readable items; when
 * [isReadable] is false the ciphertext could not be decrypted (for example because the
 * Android Keystore key was lost) and the app surfaces that state instead of dropping data.
 */
data class Note(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val type: NoteType = NoteType.NOTE,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isSecure: Boolean = false,
    val isReadable: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val displayTitle: String
        get() = title.ifBlank { content.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)?.trim() ?: "Untitled" }

    val preview: String
        get() = content.replace("\r\n", "\n").trim()

    fun matchesQuery(rawQuery: String): Boolean {
        val query = rawQuery.trim()
        if (query.isEmpty()) return true
        return title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
    }

    companion object {
        const val TITLE_MAX_LENGTH = 200
    }
}
