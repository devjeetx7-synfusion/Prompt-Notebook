package com.synfusion.snipnote.data.local

import androidx.room.TypeConverter
import com.synfusion.snipnote.domain.model.NoteType

class Converters {
    @TypeConverter
    fun noteTypeToKey(type: NoteType): String = type.storageKey

    @TypeConverter
    fun keyToNoteType(key: String?): NoteType = NoteType.fromStorageKeyOrDefault(key)
}
