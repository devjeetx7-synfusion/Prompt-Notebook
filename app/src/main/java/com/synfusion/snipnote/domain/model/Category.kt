package com.synfusion.snipnote.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val iconKey: String = CategoryIcons.DEFAULT,
    val colorArgb: Long = DefaultCategoryColors.DEFAULT,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
)

/** Keys are mapped to Material icons in the UI layer so the domain stays free of Compose types. */
object CategoryIcons {
    const val DEFAULT = "folder"
    const val PROMPT = "sparkle"
    const val CODE = "code"
    const val WRITING = "edit"
    const val STUDY = "school"
    const val BUSINESS = "briefcase"
    const val LINKS = "link"
    const val PERSONAL = "person"
    const val STAR = "star"
    const val LIGHTBULB = "lightbulb"
    const val MUSIC = "music"
    const val RECIPE = "restaurant"

    val all: List<String> = listOf(
        DEFAULT, PROMPT, CODE, WRITING, STUDY, BUSINESS, LINKS, PERSONAL, STAR, LIGHTBULB, MUSIC, RECIPE,
    )
}

object DefaultCategoryColors {
    const val DEFAULT = 0xFF7567F8L
    const val PROMPT = 0xFF7567F8L
    const val CODE = 0xFF55C7E8L
    const val WRITING = 0xFFA5E6C8L
    const val STUDY = 0xFFFFC46BL
    const val BUSINESS = 0xFFF49AC1L
    const val LINKS = 0xFF7FC8A9L
    const val PERSONAL = 0xFFB0A7FFL

    val all: List<Long> = listOf(
        PROMPT, CODE, WRITING, STUDY, BUSINESS, LINKS, PERSONAL,
        0xFF9BA8FFL, 0xFF6FD3C7L, 0xFFFF9E9EL, 0xFFE2A0FFL,
    )
}

/** Category used when a note has no category or its category was removed. */
data class CategoryRef(val id: Long, val name: String)
