package com.synfusion.snipnote.domain.model

/**
 * The kind of content a snippet holds. Type is only ever a *suggestion* produced by
 * [com.synfusion.snipnote.domain.detect.ContentTypeDetector] – the user always has the last word.
 */
enum class NoteType(val storageKey: String, val label: String) {
    PROMPT("prompt", "Prompt"),
    NOTE("note", "Note"),
    CODE("code", "Code"),
    LINK("link", "Link"),
    ;

    companion object {
        fun fromStorageKey(key: String?): NoteType? = entries.firstOrNull { it.storageKey == key }

        fun fromStorageKeyOrDefault(key: String?): NoteType = fromStorageKey(key) ?: NOTE
    }
}
