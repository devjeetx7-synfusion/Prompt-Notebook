package com.synfusion.snipnote.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synfusion.snipnote.domain.model.NoteType

/**
 * Persisted snippet row.
 *
 * For items in the Safe workspace ([isSecure] = true) the [title] and [content] columns hold
 * AES-GCM ciphertext produced by the Android Keystore-backed key, base64 encoded, and
 * [isEncrypted] records that fact so a future key loss can be detected instead of silently
 * returning garbage. Non-secure rows are stored as plaintext.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("updatedAt"),
        Index("createdAt"),
        Index("type"),
        Index("contentHash"),
        Index(value = ["isSecure", "updatedAt"]),
        Index(value = ["isSecure", "isFavorite"]),
        Index(value = ["isSecure", "isPinned"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val type: NoteType,
    val categoryId: Long? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isSecure: Boolean = false,
    val isEncrypted: Boolean = false,
    /** SHA-256 of the plaintext title+content – used only for import de-duplication. */
    @ColumnInfo(name = "contentHash") val contentHash: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
