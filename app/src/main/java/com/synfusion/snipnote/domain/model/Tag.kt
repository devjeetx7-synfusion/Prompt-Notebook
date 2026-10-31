package com.synfusion.snipnote.domain.model

data class Tag(
    val id: Long = 0L,
    val name: String,
) {
    companion object {
        const val MAX_LENGTH = 32
        const val MAX_TAGS_PER_NOTE = 20

        /** Normalises user input: trims, collapses inner whitespace and strips a leading '#'. */
        fun normalize(raw: String): String {
            val collapsed = raw.trim().removePrefix("#").trim().replace(Regex("\\s+"), " ")
            return collapsed.take(MAX_LENGTH)
        }

        /**
         * Parses a comma separated tag string, normalising and de-duplicating while
         * preserving the user's original ordering.
         */
        fun parseList(raw: String): List<String> =
            raw.split(',')
                .map(::normalize)
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(MAX_TAGS_PER_NOTE)
    }
}
