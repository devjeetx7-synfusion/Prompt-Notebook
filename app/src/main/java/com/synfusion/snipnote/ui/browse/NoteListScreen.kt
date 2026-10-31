package com.synfusion.snipnote.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.NoteFilterBar
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.components.ResultSummary
import com.synfusion.snipnote.ui.components.SearchField

/**
 * Generic list used for All / Favorites / Pinned / Category / Tag / Type views.
 * All of these are filters over the same rows – the database stores each note once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    container: AppContainer,
    preset: String,
    categoryId: Long?,
    tagId: Long?,
    typeKey: String?,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Remember the flows: calling a repository observe* function inside composition would
    // create a new Flow identity every recomposition and re-subscribe.
    val categoriesFlow = remember { container.categoryRepository.observeCategoriesWithCounts() }
    val categories by categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val tagsFlow = remember { container.tagRepository.observeTags() }
    val tags by tagsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }

    val title = remember(preset, categoryId, tagId, typeKey, categories, tags) {
        when {
            categoryId != null -> categories.firstOrNull { it.category.id == categoryId }?.category?.name ?: "Category"
            tagId != null -> "#" + (tags.firstOrNull { it.id == tagId }?.name ?: "tag")
            preset == NoteListPreset.FAVORITES -> "Favorites"
            preset == NoteListPreset.PINNED -> "Pinned"
            typeKey != null -> NoteType.fromStorageKey(typeKey)?.let { "${it.label}s" } ?: "Notes"
            else -> "All notes"
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selecting) {
                TopAppBar(
                    title = { Text("${state.selection.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::favoriteSelected) {
                            Icon(Icons.Filled.Star, contentDescription = "Favorite selected")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    ),
                )
            } else {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = onCreateNote) { Icon(Icons.Filled.Add, contentDescription = "New note") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            SearchField(
                value = state.filter.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search in $title",
            )
            Column(modifier = Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteFilterBar(
                    selectedType = state.filter.type,
                    favoritesOnly = state.filter.favoritesOnly,
                    pinnedOnly = state.filter.pinnedOnly,
                    categories = categories,
                    selectedCategoryId = state.filter.categoryId,
                    sort = state.filter.sort,
                    onTypeSelected = viewModel::setType,
                    onToggleFavorites = viewModel::toggleFavoritesOnly,
                    onTogglePinned = viewModel::togglePinnedOnly,
                    onCategorySelected = { viewModel.setCategoryFilter(it) },
                    onSortSelected = viewModel::setSort,
                    onClear = viewModel::clearFilters,
                )
                ResultSummary(state.notes.size)
            }

            when {
                state.notes.isEmpty() && (state.filter.query.isNotBlank() || state.filter.isActive) -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Nothing here",
                    message = "No notes match this combination of search and filters.",
                    actionLabel = "Clear filters",
                    onAction = viewModel::clearFilters,
                )
                state.notes.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Notes,
                    title = "No notes yet",
                    message = "Notes you create will show up here.",
                    actionLabel = "Create a note",
                    onAction = onCreateNote,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = {
                                if (state.selecting) viewModel.toggleSelection(note.id) else onOpenNote(note.id)
                            },
                            onLongClick = { viewModel.toggleSelection(note.id) },
                            selected = note.id in state.selection,
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.selection.size} note(s)?") },
            text = { Text("This permanently removes them from this device.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteSelected() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
