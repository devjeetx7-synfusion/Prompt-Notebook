package com.synfusion.snipnote.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.DeleteNoteDialog
import com.synfusion.snipnote.ui.components.ListPlaceholder
import com.synfusion.snipnote.ui.components.MoveCategoryDialog
import com.synfusion.snipnote.ui.components.NoteAction
import com.synfusion.snipnote.ui.components.NoteActionSheet
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Search over non-private notes only – the repository excludes Safe items, so a locked Safe can
 * never leak titles or content through this screen.
 */
@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit,
    onEditNote: (Long) -> Unit = {},
    viewModel: SearchViewModel = viewModel(factory = SnipViewModelFactories.search(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var actionTarget by remember { mutableStateOf<Note?>(null) }
    var moveTarget by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(Unit) {
        // The field is attached in the same frame; retry once instead of crashing when it is not.
        runCatching { focusRequester.requestFocus() }
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                val shape = RoundedCornerShape(18.dp)
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search notes, prompts, code, links…", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = shape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Chip("All", null, state.filter.type == null) { viewModel.setType(null) }
                    }
                    items(NoteType.entries) { type ->
                        Chip(type.label, SnipIcons.type(type), state.filter.type == type) {
                            viewModel.setType(if (state.filter.type == type) null else type)
                        }
                    }
                    item {
                        Chip("Favorites", Icons.Outlined.StarBorder, state.filter.favoritesOnly) {
                            viewModel.toggleFavoritesOnly()
                        }
                    }
                }
            }
            if (!state.hasQuery) {
                item {
                    Text(
                        text = "Search covers titles, content, tags and categories. Items inside the Safe are never included.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (state.results.isEmpty()) {
                item {
                    ListPlaceholder(
                        icon = Icons.Outlined.SearchOff,
                        title = "No results",
                        message = "Nothing matches \"${state.query}\". Private Safe items are excluded from search by design.",
                    )
                }
            } else {
                items(state.results, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { actionTarget = note },
                        onOverflow = { actionTarget = note },
                    )
                }
            }
        }
    }

    actionTarget?.let { note ->
        NoteActionSheet(
            note = note,
            onDismiss = { actionTarget = null },
            onAction = { action ->
                actionTarget = null
                when (action) {
                    NoteAction.COPY -> viewModel.copy(note)
                    NoteAction.EDIT -> onEditNote(note.id)
                    NoteAction.TOGGLE_FAVORITE -> viewModel.toggleFavorite(note)
                    NoteAction.MOVE_CATEGORY -> moveTarget = note
                    NoteAction.TOGGLE_PRIVATE -> viewModel.togglePrivate(note)
                    NoteAction.DELETE -> deleteTarget = note
                }
            },
        )
    }

    moveTarget?.let { note ->
        MoveCategoryDialog(
            categories = state.categories.map { it.category },
            currentCategoryId = note.category?.id,
            onPick = { targetId ->
                viewModel.moveToCategory(note, targetId)
                moveTarget = null
            },
            onDismiss = { moveTarget = null },
        )
    }

    deleteTarget?.let { note ->
        DeleteNoteDialog(
            title = note.displayTitle,
            onConfirm = {
                viewModel.delete(note)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(50)
    val container = if (selected) MaterialTheme.colorScheme.primary else extras.chipFill
    val content = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clip(shape)
            .background(container, shape)
            .then(if (selected) Modifier else Modifier.border(BorderStroke(1.dp, extras.glassBorder), shape))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}
