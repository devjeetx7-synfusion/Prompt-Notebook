package com.synfusion.snipnote.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synfusion.snipnote.domain.model.AutoLockTimeout
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.domain.model.ThemeMode
import com.synfusion.snipnote.domain.model.UserSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "snipnote_settings")

/** DataStore-backed UI preferences. Contains no content and no keys. */
class SettingsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            UserSettings(
                themeMode = ThemeMode.fromKey(prefs[KEY_THEME]),
                autoLockTimeout = AutoLockTimeout.fromSeconds(prefs[KEY_AUTOLOCK] ?: AutoLockTimeout.ONE_MINUTE.seconds),
                blockScreenCapture = prefs[KEY_BLOCK_CAPTURE] ?: true,
                defaultSort = prefs[KEY_SORT]?.let { key -> SortOrder.entries.firstOrNull { it.name == key } }
                    ?: SortOrder.UPDATED_DESC,
                includeSafeInBackup = prefs[KEY_INCLUDE_SAFE] ?: false,
                onboarded = prefs[KEY_ONBOARDED] ?: false,
            )
        }
        .flowOn(ioDispatcher)

    suspend fun setThemeMode(mode: ThemeMode) = put { it[KEY_THEME] = mode.storageKey }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) = put { it[KEY_AUTOLOCK] = timeout.seconds }

    suspend fun setBlockScreenCapture(enabled: Boolean) = put { it[KEY_BLOCK_CAPTURE] = enabled }

    suspend fun setDefaultSort(sort: SortOrder) = put { it[KEY_SORT] = sort.name }

    suspend fun setIncludeSafeInBackup(enabled: Boolean) = put { it[KEY_INCLUDE_SAFE] = enabled }

    suspend fun setOnboarded(value: Boolean = true) = put { it[KEY_ONBOARDED] = value }

    private suspend fun put(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) =
        withContext(ioDispatcher) { context.settingsDataStore.edit(block) }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_AUTOLOCK = longPreferencesKey("auto_lock_seconds")
        val KEY_BLOCK_CAPTURE = booleanPreferencesKey("block_screen_capture")
        val KEY_SORT = stringPreferencesKey("default_sort")
        val KEY_INCLUDE_SAFE = booleanPreferencesKey("include_safe_in_backup")
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
