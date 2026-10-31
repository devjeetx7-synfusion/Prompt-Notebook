package com.synfusion.snipnote.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity? = null,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity> = emptyList(),
)

data class CategoryCount(
    val categoryId: Long?,
    val itemCount: Int,
)

data class TagCount(
    val id: Long,
    val name: String,
    val itemCount: Int,
)
