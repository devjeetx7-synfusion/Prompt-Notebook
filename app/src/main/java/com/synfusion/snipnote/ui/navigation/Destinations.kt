package com.synfusion.snipnote.ui.navigation

import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.browse.NoteListPreset

/**
 * Single source of truth for routes. Optional arguments are always encoded with defaults so
 * a route can be built with partial information.
 */
object Destinations {

    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val SAFE = "safe"
    const val SETTINGS = "settings"
    const val SEARCH = "search"

    const val NOTE_LIST = "list/{preset}?categoryId={categoryId}&type={type}&tagId={tagId}"
    const val EDITOR = "editor?noteId={noteId}&secure={secure}&type={type}"
    const val DETAIL = "note/{noteId}?secure={secure}"

    const val ARG_PRESET = "preset"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_TYPE = "type"
    const val ARG_TAG_ID = "tagId"
    const val ARG_NOTE_ID = "noteId"
    const val ARG_SECURE = "secure"

    fun noteList(
        preset: String = NoteListPreset.ALL,
        categoryId: Long? = null,
        type: NoteType? = null,
        tagId: Long? = null,
    ): String = "list/$preset?categoryId=${categoryId ?: -1}&type=${type?.storageKey ?: "-"}&tagId=${tagId ?: -1}"

    fun editor(noteId: Long? = null, secure: Boolean = false, type: NoteType? = null): String =
        "editor?noteId=${noteId ?: -1}&secure=$secure&type=${type?.storageKey ?: "-"}"

    fun detail(noteId: Long, secure: Boolean = false): String = "note/$noteId?secure=$secure"

    /** Top-level destinations, used by the navigation bar / rail. */
    val topLevel: List<TopLevelDestination> = listOf(
        TopLevelDestination(HOME, "Home", "Home"),
        TopLevelDestination(CATEGORIES, "Categories", "Categories"),
        TopLevelDestination(SAFE, "Safe", "Safe"),
        TopLevelDestination(SETTINGS, "Settings", "Settings"),
    )

    fun isTopLevel(route: String?): Boolean = topLevel.any { it.route == route }
}

data class TopLevelDestination(val route: String, val label: String, val contentDescription: String)
