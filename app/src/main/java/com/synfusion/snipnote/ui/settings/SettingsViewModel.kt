package com.synfusion.snipnote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.snipnote.data.backup.DuplicatePolicy
import com.synfusion.snipnote.data.backup.ExportOutcome
import com.synfusion.snipnote.data.backup.ImportOutcome
import com.synfusion.snipnote.data.backup.ImportPreparation
import com.synfusion.snipnote.data.backup.ImportPreview
import com.synfusion.snipnote.data.repository.SettingsRepository
import com.synfusion.snipnote.domain.model.AutoLockTimeout
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.domain.model.ThemeMode
import com.synfusion.snipnote.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingExport(
    val json: String,
    val fileName: String,
    val noteCount: Int,
    val includesSecure: Boolean,
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val pendingExport: PendingExport? = null,
    val importPreview: ImportPreview? = null,
    val importPolicy: DuplicatePolicy = DuplicatePolicy.SKIP,
    val importOutcome: ImportOutcome? = null,
    val importError: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backup: com.synfusion.snipnote.data.backup.BackupRepository,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    private data class LocalState(
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null,
        val pendingExport: PendingExport? = null,
        val importPreview: ImportPreview? = null,
        val importPolicy: DuplicatePolicy = DuplicatePolicy.SKIP,
        val importOutcome: ImportOutcome? = null,
        val importError: String? = null,
    )

    val state: StateFlow<SettingsUiState> = combine(
        repository.settings.catch { emit(UserSettings()) },
        local,
    ) { settings, extra ->
        SettingsUiState(
            settings = settings,
            busy = extra.busy,
            message = extra.message,
            error = extra.error,
            pendingExport = extra.pendingExport,
            importPreview = extra.importPreview,
            importPolicy = extra.importPolicy,
            importOutcome = extra.importOutcome,
            importError = extra.importError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) = viewModelScope.launch {
        repository.setAutoLockTimeout(timeout)
    }

    fun setBlockScreenCapture(enabled: Boolean) = viewModelScope.launch {
        repository.setBlockScreenCapture(enabled)
    }

    fun setDefaultSort(sort: SortOrder) = viewModelScope.launch { repository.setDefaultSort(sort) }

    fun setIncludeSafeInBackup(enabled: Boolean) = viewModelScope.launch {
        repository.setIncludeSafeInBackup(enabled)
    }

    // region backup

    fun startExport(includeSecure: Boolean) {
        if (local.value.busy) return
        local.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = backup.export(includeSecure)) {
                is ExportOutcome.Success -> local.update {
                    it.copy(
                        busy = false,
                        pendingExport = PendingExport(result.json, result.fileName, result.noteCount, result.includesSecure),
                    )
                }
                is ExportOutcome.Failure -> local.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    fun onExportFinished(written: Boolean) {
        local.update { current ->
            current.copy(
                pendingExport = null,
                message = if (written) {
                    "Backup written (${current.pendingExport?.noteCount ?: 0} notes)"
                } else {
                    "Export cancelled – no file was written"
                },
                error = null,
            )
        }
    }

    /** Called when the picked file could not be read at all (permissions, size, I/O). */
    fun failImportRead(fileName: String) {
        local.update {
            it.copy(
                busy = false,
                importPreview = null,
                importOutcome = null,
                importError = "\"$fileName\" could not be read. It may be too large (limit 32 MB) or no longer accessible.",
            )
        }
    }

    fun prepareImport(raw: String?, fileName: String) {
        local.update { it.copy(busy = true, error = null, importError = null, importOutcome = null, importPreview = null) }
        viewModelScope.launch {
            when (val result = backup.prepareImport(raw, fileName)) {
                is ImportPreparation.Ready -> local.update { it.copy(busy = false, importPreview = result.preview) }
                is ImportPreparation.Rejected -> local.update { it.copy(busy = false, importError = result.message) }
            }
        }
    }

    fun setImportPolicy(policy: DuplicatePolicy) = local.update { it.copy(importPolicy = policy) }

    fun confirmImport() {
        val preview = local.value.importPreview ?: return
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            val outcome = backup.applyImport(preview, local.value.importPolicy)
            local.update { it.copy(busy = false, importPreview = null, importOutcome = outcome) }
        }
    }

    fun cancelImport() = local.update { it.copy(importPreview = null, importError = null) }

    fun dismissImportOutcome() = local.update { it.copy(importOutcome = null, importError = null) }

    // endregion

    fun clearMessage() = local.update { it.copy(message = null, error = null) }
}
