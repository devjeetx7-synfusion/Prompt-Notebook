package com.synfusion.snipnote.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.Pill
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    container: AppContainer,
    secure: Boolean,
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = SnipViewModelFactories.editor(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extras = LocalAuroraExtras.current
    var showDiscard by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedId) {
        val id = state.savedId
        if (id != null) {
            onSaved(id, state.isSecure)
        }
    }

    LaunchedEffect(state.error) {
        val message = state.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val requestBack: () -> Unit = {
        if (state.isDirty && state.savedId == null) showDiscard = true else onBack()
    }
    BackHandler(enabled = true) { requestBack() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            state.isNew && state.isSecure -> "New private ${state.type.label.lowercase()}"
                            state.isNew -> "New ${state.type.label.lowercase()}"
                            else -> "Edit ${state.type.label.lowercase()}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave && !state.saving) {
                        if (state.saving) Text("Saving…") else Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.sessionExpired) {
                SecureBanner("The Safe locked before this item could be opened. Go back and unlock again.")
            }
            if (state.unreadable) {
                SecureBanner("This item's encryption key is not available on this device, so its content cannot be shown. Editing will overwrite it – export first if you need to keep it.")
            }

            // Type selector
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NoteType.entries.forEach { option ->
                    Pill(
                        label = option.label,
                        selected = state.type == option,
                        onClick = { viewModel.onTypeChange(option) },
                    )
                }
            }

            state.detection?.let { detection ->
                if (detection.suggestedType != state.type) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Looks like a ${detection.suggestedType.label.lowercase()} (${detection.reason.lowercase()}).",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::applySuggestion) { Text("Use") }
                        TextButton(onClick = viewModel::dismissSuggestion) { Text("Dismiss") }
                    }
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                placeholder = { Text("Optional – a title makes notes easier to find") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                label = { Text("Content") },
                placeholder = { Text("Paste or type here. Line breaks, indentation and Unicode are preserved.") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                minLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (state.type == NoteType.CODE) FontFamily.Monospace else FontFamily.Default,
                ),
            )
            Text(
                "${state.characterCount} characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Category picker
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(extras.glassFill)
                        .clickable { categoryMenu = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    val current = state.categories.firstOrNull { it.id == state.categoryId }
                    Text(
                        current?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Uncategorized") },
                        onClick = { viewModel.onCategoryChange(null); categoryMenu = false },
                    )
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    com.synfusion.snipnote.ui.components.SnipIcons.category(category.iconKey),
                                    contentDescription = null,
                                    tint = Color(category.colorArgb),
                                )
                            },
                            onClick = { viewModel.onCategoryChange(category.id); categoryMenu = false },
                        )
                    }
                }
            }

            // Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.tagInput,
                        onValueChange = viewModel::onTagInputChange,
                        label = { Text("Add tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = viewModel::addTag, enabled = state.tagInput.isNotBlank()) { Text("Add") }
                }
                if (state.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.tags) { tag ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("#$tag", style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = { viewModel.removeTag(tag) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove tag $tag", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Toggles
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TogglePill(
                    icon = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    label = "Favorite",
                    active = state.isFavorite,
                    activeTint = extras.favorite,
                    onClick = viewModel::toggleFavorite,
                )
                TogglePill(
                    icon = Icons.Filled.PushPin,
                    label = "Pin",
                    active = state.isPinned,
                    activeTint = extras.pin,
                    onClick = viewModel::togglePinned,
                )
                TogglePill(
                    icon = if (state.isSecure) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    label = if (state.isSecure) "Private" else "Not private",
                    active = state.isSecure,
                    activeTint = extras.secure,
                    onClick = viewModel::toggleSecure,
                )
            }

            if (state.isSecure || secure) {
                SecureBanner(
                    "Private items are encrypted with an AES-GCM key stored in the Android Keystore, hidden from normal lists and excluded from standard backups.",
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave && !state.saving,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.saving) "Saving…" else "Save note") }
                TextButton(onClick = requestBack) { Text("Cancel") }
            }
            Spacer(modifier = Modifier.size(40.dp))
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Leaving now will lose them.") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onBack() }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun TogglePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeTint: Color,
    onClick: () -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (active) activeTint.copy(alpha = 0.18f) else extras.glassFill, shape)
            .border(BorderStroke(1.dp, if (active) activeTint.copy(alpha = 0.5f) else extras.glassBorder), shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (active) activeTint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) activeTint else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SecureBanner(message: String) {
    val extras = LocalAuroraExtras.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(extras.secure.copy(alpha = 0.14f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = extras.secure, modifier = Modifier.size(16.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
