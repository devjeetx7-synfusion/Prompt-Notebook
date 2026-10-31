package com.synfusion.snipnote

import android.content.Context
import com.synfusion.snipnote.data.backup.BackupRepository
import com.synfusion.snipnote.data.local.SnipNoteDatabase
import com.synfusion.snipnote.data.repository.CategoryRepository
import com.synfusion.snipnote.data.repository.RoomNoteRepository
import com.synfusion.snipnote.data.repository.SettingsRepository
import com.synfusion.snipnote.data.repository.TagRepository
import com.synfusion.snipnote.data.security.CipherBox
import com.synfusion.snipnote.data.security.KeystoreCipherBox
import com.synfusion.snipnote.data.security.SafeSession
import com.synfusion.snipnote.util.ClipboardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Small manual DI graph. The app is intentionally a single-module, single-process app, so a
 * container created in [SnipNoteApp] is simpler and more transparent than a DI framework.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: SnipNoteDatabase by lazy { SnipNoteDatabase.get(appContext) }

    val safeSession = SafeSession()

    private val cipherBox: CipherBox by lazy { KeystoreCipherBox() }

    val noteRepository: RoomNoteRepository by lazy {
        RoomNoteRepository(database, cipherBox, safeSession)
    }

    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database) }

    val tagRepository: TagRepository by lazy { TagRepository(database) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(database, noteRepository, categoryRepository, safeSession)
    }

    val clipboard: ClipboardService by lazy { ClipboardService(appContext) }

    /** Seeds the seven default categories on first launch. Idempotent. */
    fun seedDefaults() {
        applicationScope.launch { categoryRepository.ensureDefaults() }
    }
}
