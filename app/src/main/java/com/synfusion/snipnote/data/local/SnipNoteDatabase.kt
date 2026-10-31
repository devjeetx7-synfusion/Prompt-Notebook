package com.synfusion.snipnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
    ],
    version = SnipNoteDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SnipNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    companion object {
        const val VERSION = 1

        const val NAME = "snipnote.db"

        @Volatile
        private var instance: SnipNoteDatabase? = null

        fun get(context: Context): SnipNoteDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): SnipNoteDatabase =
            Room.databaseBuilder(context, SnipNoteDatabase::class.java, NAME)
                // Schema v1 is the first release; future versions must add explicit Migrations.
                .build()

        /** Used by tests to construct an isolated, file-backed or in-memory database. */
        fun buildInMemory(context: Context): SnipNoteDatabase =
            Room.inMemoryDatabaseBuilder(context, SnipNoteDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        fun buildFileBacked(context: Context, fileName: String): SnipNoteDatabase =
            Room.databaseBuilder(context, SnipNoteDatabase::class.java, fileName)
                .allowMainThreadQueries()
                .build()
    }
}
