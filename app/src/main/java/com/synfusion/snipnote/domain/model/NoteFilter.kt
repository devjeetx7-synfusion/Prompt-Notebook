package com.synfusion.snipnote.domain.model

enum class SortOrder(val label: String) {
    UPDATED_DESC("Recently updated"),
    CREATED_DESC("Recently created"),
    TITLE_ASC("Title A–Z"),
    TYPE_ASC("Content type"),
}

/**
 * A single, composable description of a note query. Favorites / pinned / recent are
 * filters over the same records – never duplicated rows.
 */
data class NoteFilter(
    val query: String = "",
    val type: NoteType? = null,
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val favoritesOnly: Boolean = false,
    val pinnedOnly: Boolean = false,
    val sort: SortOrder = SortOrder.UPDATED_DESC,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || categoryId != null || tagId != null ||
            favoritesOnly || pinnedOnly

    fun withQuery(value: String): NoteFilter = copy(query = value)

    fun toggleFavoriteOnly(): NoteFilter = copy(favoritesOnly = !favoritesOnly)

    fun togglePinnedOnly(): NoteFilter = copy(pinnedOnly = !pinnedOnly)
}
