package com.synfusion.snipnote.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["nameKey"], unique = true), Index("sortOrder")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Lower-cased, whitespace-trimmed name; unique so we never create duplicate categories. */
    val nameKey: String,
    val iconKey: String,
    val colorArgb: Long,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long,
)
