package com.synfusion.snipnote.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.BuildConfig
import com.synfusion.snipnote.data.backup.BackupCodec
import com.synfusion.snipnote.data.backup.DuplicatePolicy
import com.synfusion.snipnote.data.backup.ImportOutcome
import com.synfusion.snipnote.domain.model.AutoLockTimeout
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.domain.model.ThemeMode
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.GlassCard
import com.synfusion.snipnote.ui.components.Pill
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

@Composable
fun SettingsScreen(
    container: AppContainer,
    viewModel: SettingsViewModel = viewModel(factory = SnipViewModelFactories.settings(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Create document for export: the user picks the destination, we stream the JSON into it.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val pending = state.pendingExport
        if (uri == null || pending == null) {
            viewModel.onExportFinished(false)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(pending.json.toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
            }
            viewModel.onExportFinished(ok)
        }
    }

    // Open document for import: read as text and validate before anything is written.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { readTextSafely(context, uri) }
            if (result.text == null) {
                viewModel.failImportRead(result.fileName)
            } else {
                viewModel.prepareImport(result.text, result.fileName)
            }
        }
    }

    LaunchedEffect(state.pendingExport) {
        val pending = state.pendingExport
        if (pending != null) exportLauncher.launch(pending.fileName)
    }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Appearance, privacy and your data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection("Appearance") {
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            Pill(
                                label = mode.label,
                                selected = state.settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    DropdownSetting(
                        label = "Default sort",
                        value = state.settings.defaultSort.label,
                        options = SortOrder.entries.map { it.label },
                        onSelect = { index -> viewModel.setDefaultSort(SortOrder.entries[index]) },
                    )
                }
            }

            item {
                SettingsSection("Safe & security") {
                    DropdownSetting(
                        label = "Auto-lock Safe",
                        value = state.settings.autoLockTimeout.label,
                        options = AutoLockTimeout.entries.map { it.label },
                        onSelect = { index -> viewModel.setAutoLockTimeout(AutoLockTimeout.entries[index]) },
                    )
                    SwitchSetting(
                        label = "Block screenshots and recents preview",
                        description = "Adds FLAG_SECURE so Safe and notes never appear in the recents thumbnail or screen recordings.",
                        checked = state.settings.blockScreenCapture,
                        onCheckedChange = viewModel::setBlockScreenCapture,
                    )
                    OutlinedButton(onClick = { container.safeSession.lock() }) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Safe now")
                    }
                    Text(
                        "Encryption keys live in this device's Android Keystore and never leave it. There is no key recovery: a factory reset, cleared credentials or moving to a new device makes existing Safe items permanently unreadable. The app will show those items as unreadable instead of deleting them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection("Backup & restore") {
                    Text(
                        "Exports are plain JSON – they are not encrypted. Safe items are left out unless you opt in below, and opting in writes them as readable text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SwitchSetting(
                        label = "Include Safe items in exports",
                        description = "Off by default. Requires an unlocked Safe and produces unencrypted JSON.",
                        checked = state.settings.includeSafeInBackup,
                        onCheckedChange = viewModel::setIncludeSafeInBackup,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.startExport(state.settings.includeSafeInBackup) },
                            enabled = !state.busy,
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            enabled = !state.busy,
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                    }
                    if (state.busy) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Working…", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        "Import never overwrites silently: you preview the file, choose how duplicates are handled and confirm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsSection("About") {
                    Text("SnipNote ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Offline-first personal workspace for prompts, notes, code and links. Content is stored in a local Room database; nothing is uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        "Known limitations",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "• Backups are unencrypted JSON.\n" +
                            "• Safe items cannot be recovered if the Keystore key is lost.\n" +
                            "• Auto-lock only runs while the process is alive; a cold start always locks Safe.\n" +
                            "• Unlocking requires biometrics or a device screen lock; without either, Safe cannot be opened.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    state.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            policy = state.importPolicy,
            busy = state.busy,
            onPolicyChange = viewModel::setImportPolicy,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }

    if (state.importPreview == null && (state.importOutcome != null || state.importError != null)) {
        ImportResultDialog(
            outcome = state.importOutcome,
            error = state.importError,
            onDismiss = viewModel::dismissImportOutcome,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val extras = LocalAuroraExtras.current
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(extras.glassFill)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(index); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: com.synfusion.snipnote.data.backup.ImportPreview,
    policy: DuplicatePolicy,
    busy: Boolean,
    onPolicyChange: (DuplicatePolicy) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
        title = { Text("Import preview") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(preview.fileName, style = MaterialTheme.typography.labelMedium)
                Text(
                    "${preview.notes.size} item(s): ${preview.newCount} new, ${preview.duplicateCount} already present. " +
                        "${preview.newCategoryCount} category(ies) will be created.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (preview.exportedAt > 0) {
                    Text(
                        "Exported " + Instant.ofEpochMilli(preview.exportedAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                preview.warnings.forEach { warning ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Text("Duplicates", style = MaterialTheme.typography.labelLarge)
                DuplicatePolicy.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPolicyChange(option) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = policy == option, onClick = { onPolicyChange(option) })
                        Column {
                            Text(option.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                option.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !busy) { Text(if (busy) "Importing…" else "Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun ImportResultDialog(outcome: ImportOutcome?, error: String?, onDismiss: () -> Unit) {
    val title = if (outcome is ImportOutcome.Success) "Import complete" else "Import failed"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (outcome) {
                    is ImportOutcome.Success -> {
                        Text("Imported ${outcome.imported} item(s).")
                        if (outcome.skipped > 0) Text("Skipped ${outcome.skipped} duplicate(s).", style = MaterialTheme.typography.bodySmall)
                        if (outcome.replaced > 0) Text("Replaced ${outcome.replaced} existing item(s).", style = MaterialTheme.typography.bodySmall)
                        if (outcome.failed > 0) Text("${outcome.failed} item(s) could not be imported.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        outcome.warnings.take(4).forEach { warning ->
                            Text("• $warning", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (outcome.categoriesCreated > 0) Text("Created ${outcome.categoriesCreated} categor(ies).", style = MaterialTheme.typography.bodySmall)
                        if (outcome.tagsCreated > 0) Text("Created ${outcome.tagsCreated} tag(s).", style = MaterialTheme.typography.bodySmall)
                        if (outcome.secureImported > 0) Text("${outcome.secureImported} item(s) were encrypted into Safe.", style = MaterialTheme.typography.bodySmall)
                    }
                    is ImportOutcome.Failure -> Text(outcome.message)
                    null -> Text(error ?: "The import could not be completed.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private data class ImportedFile(val text: String?, val fileName: String)

/**
 * Reads at most [BackupCodec.MAX_FILE_CHARS] characters so a hostile or simply huge file cannot
 * exhaust memory before validation even starts.
 */
private fun readTextSafely(context: android.content.Context, uri: android.net.Uri): ImportedFile {
    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "backup.json"
    return try {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = stream.reader(Charsets.UTF_8).buffered()
            val builder = StringBuilder()
            val buffer = CharArray(8 * 1024)
            var total = 0
            while (true) {
                val read = reader.read(buffer)
                if (read <= 0) break
                total += read
                if (total > BackupCodec.MAX_FILE_CHARS) return ImportedFile(null, name)
                builder.append(buffer, 0, read)
            }
            builder.toString()
        }
        ImportedFile(text, name)
    } catch (e: OutOfMemoryError) {
        ImportedFile(null, name)
    } catch (e: Exception) {
        ImportedFile(null, name)
    }
}
