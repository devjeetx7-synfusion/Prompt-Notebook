package com.synfusion.snipnote.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.domain.detect.ContentTypeDetector
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.browse.NoteListPreset
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.GlassCard
import com.synfusion.snipnote.ui.components.NoteTile
import com.synfusion.snipnote.ui.components.Pill
import com.synfusion.snipnote.ui.components.SectionHeader
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    twoPane: Boolean,
    onOpenNote: (Long) -> Unit,
    onCreateNote: (NoteType?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenList: (String, Long?, NoteType?) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSafe: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = SnipViewModelFactories.home(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCapture by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCapture = true },
                icon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
                text = { Text("Quick capture") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HomeHeader(state.greeting, onOpenSearch, onOpenSafe, onOpenSettings) }
            item { SearchLauncher(onOpenSearch) }
            item { QuickActions(onCreateNote = onCreateNote) }
            item {
                StatsRow(
                    counts = state.counts,
                    onOpenList = { preset -> onOpenList(preset, null, null) },
                )
            }
            item {
                SectionHeader(
                    title = "Categories",
                    actionLabel = "See all",
                    onAction = onOpenCategories,
                )
            }
            item {
                CategoryShortcuts(
                    categories = state.categories,
                    onOpen = { category -> onOpenList(NoteListPreset.ALL, category.category.id, null) },
                    onOpenAll = onOpenCategories,
                )
            }
            item {
                SectionHeader(
                    title = if (state.counts.favorites > 0) "Recent" else "Recent notes",
                    actionLabel = "Favorites (${state.counts.favorites})",
                    onAction = { onOpenList(NoteListPreset.FAVORITES, null, null) },
                )
            }
            if (state.recent.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Notes,
                        title = "Nothing saved yet",
                        message = "Capture a prompt, a note, a snippet of code or a link. Everything stays on this device.",
                        actionLabel = "Create your first note",
                        onAction = { onCreateNote(null) },
                    )
                }
            } else {
                val columns = if (twoPane) 3 else 2
                val rows = state.recent.chunked(columns)
                items(rows.size) { index ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rows[index].forEach { note ->
                            NoteTile(
                                note = note,
                                onClick = { onOpenNote(note.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rows[index].size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    TextButton(onClick = { onOpenList(NoteListPreset.ALL, null, null) }) {
                        Text("Browse all ${state.counts.total} notes")
                    }
                }
            }
            if (state.error != null) {
                item {
                    GlassCard(fill = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    if (showCapture) {
        ModalBottomSheet(
            onDismissRequest = { showCapture = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            QuickCaptureSheet(
                onSave = { text, type ->
                    viewModel.quickCapture(text, type) { id ->
                        showCapture = false
                        onOpenNote(id)
                    }
                },
                onDismiss = { showCapture = false },
                onOpenFullEditor = { type ->
                    showCapture = false
                    onCreateNote(type)
                },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    onOpenSearch: () -> Unit,
    onOpenSafe: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SnipNote",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$greeting – your prompts, notes and code in one quiet place.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
        IconButton(onClick = onOpenSafe) {
            Icon(Icons.Filled.Lock, contentDescription = "Safe")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun SearchLauncher(onOpenSearch: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.glassFill, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Search titles and content",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenSearch) { Text("Search") }
    }
}

@Composable
private fun QuickActions(
    onCreateNote: (NoteType?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.AutoAwesome, "New prompt", NoteType.PROMPT, onCreateNote, Modifier.weight(1f))
            QuickAction(Icons.Outlined.EditNote, "New note", NoteType.NOTE, onCreateNote, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Filled.Code, "New code", NoteType.CODE, onCreateNote, Modifier.weight(1f))
            QuickAction(Icons.Filled.Link, "Save link", NoteType.LINK, onCreateNote, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    type: NoteType,
    onClick: (NoteType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(extras.glassFill, shape)
            .clickable(role = Role.Button) { onClick(type) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.Add, contentDescription = "Create $label", modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatsRow(counts: Counts, onOpenList: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("All", counts.total.toString(), Icons.Outlined.Notes, Modifier.weight(1f)) {
            onOpenList(NoteListPreset.ALL)
        }
        StatCard("Favorites", counts.favorites.toString(), Icons.Filled.Star, Modifier.weight(1f)) {
            onOpenList(NoteListPreset.FAVORITES)
        }
        StatCard("Pinned", counts.pinned.toString(), Icons.Filled.PushPin, Modifier.weight(1f)) {
            onOpenList(NoteListPreset.PINNED)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(extras.glassFill, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryShortcuts(
    categories: List<CategoryWithCount>,
    onOpen: (CategoryWithCount) -> Unit,
    onOpenAll: () -> Unit,
) {
    if (categories.isEmpty()) {
        GlassCard(onClick = onOpenAll) {
            Text("Create a category to group your snippets", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories.take(10), key = { it.category.id }) { category ->
            val color = Color(category.category.colorArgb)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = SnipIcons.category(category.category.iconKey),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Text(category.category.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    category.itemCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onOpen(category) }, modifier = Modifier.height(28.dp)) { Text("Open") }
            }
        }
    }
}

/** Fast, single-field capture. Type is auto-detected but always editable. */
@Composable
private fun QuickCaptureSheet(
    onSave: (String, NoteType) -> Unit,
    onDismiss: () -> Unit,
    onOpenFullEditor: (NoteType?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val detection = remember(text) { ContentTypeDetector.detect(text) }
    var type by remember { mutableStateOf(NoteType.NOTE) }
    LaunchedEffect(detection) {
        if (detection.isConfident) type = detection.suggestedType
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quick capture", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("Paste a prompt, a link or a snippet…") },
            label = { Text("Content") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteType.entries.forEach { option ->
                Pill(
                    label = option.label,
                    selected = type == option,
                    onClick = { type = option },
                )
            }
        }
        if (detection.isConfident) {
            Text(
                text = "Detected ${detection.suggestedType.label.lowercase()} – ${detection.reason.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onSave(text, type) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
            TextButton(onClick = { onOpenFullEditor(type) }) { Text("Full editor") }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
