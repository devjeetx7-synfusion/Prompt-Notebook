package com.synfusion.snipnote.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.ui.components.DeleteNoteDialog
import com.synfusion.snipnote.ui.components.ListPlaceholder
import com.synfusion.snipnote.ui.components.MoveCategoryDialog
import com.synfusion.snipnote.ui.components.NoteAction
import com.synfusion.snipnote.ui.components.NoteActionSheet
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.navigation.FloatingNavContentPadding
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Filtered list used by Categories (category, tag, favorites, pinned) and by Home shortcuts.
 * Rows expose no permanent actions: tap opens the item, long press or the overflow opens the sheet.
 */
@Composable
fun NoteListScreen(
    container: AppContainer,
    viewModel: NoteListViewModel,
    preset: String,
    categoryId: Long? = null,
    tagId: Long? = null,
    typeKey: String? = null,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onEditNote: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionTarget by remember { mutableStateOf<Note?>(null) }
    var moveTarget by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    // Tag filtering stays client-side: tags are a light annotation and the DAO query is shared.
    val visible = remember(state.notes, tagId) {
        if (tagId == null) state.notes else state.notes.filter { note -> note.tags.any { it.id == tagId } }
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
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 8.dp,
                bottom = FloatingNavContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 2.dp),
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleFor(state, typeKey),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${visible.size} item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(LocalAuroraExtras.current.chipFill)
                                .clickable(role = Role.Button) { sortMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Sort list", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    leadingIcon = {
                                        if (state.filter.sort == order) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        } else {
                                            Spacer(modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSort(order)
                                        sortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                val shape = RoundedCornerShape(18.dp)
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Filter these items", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = shape,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            label = "All",
                            icon = null,
                            selected = state.filter.type == null,
                            onClick = { viewModel.setType(null) },
                        )
                    }
                    items(NoteType.entries) { type ->
                        FilterChip(
                            label = type.label,
                            icon = SnipIcons.type(type),
                            selected = state.filter.type == type,
                            onClick = { viewModel.setType(if (state.filter.type == type) null else type) },
                        )
                    }
                    item {
                        FilterChip(
                            label = "Favorites",
                            icon = null,
                            selected = state.filter.favoritesOnly,
                            onClick = viewModel::toggleFavoritesOnly,
                        )
                    }
                    item {
                        FilterChip(
                            label = "Pinned",
                            icon = null,
                            selected = state.filter.pinnedOnly,
                            onClick = viewModel::togglePinnedOnly,
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    ListPlaceholder(
                        icon = Icons.Outlined.Description,
                        title = if (state.filter.isActive) "No matching items" else "Nothing here yet",
                        message = if (state.filter.isActive) {
                            "Try clearing the filters or searching for something else."
                        } else {
                            "Tap the + button in the navigation bar to add your first item."
                        },
                        actionLabel = if (state.filter.isActive) null else "Create an item",
                        onAction = if (state.filter.isActive) null else onCreateNote,
                    )
                }
            } else {
                items(visible, key = { it.id }) { note ->
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
            categories = state.categories,
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

private fun titleFor(state: NoteListUiState, typeKey: String?): String = when {
    state.preset == NoteListPreset.FAVORITES -> "Favorites"
    state.preset == NoteListPreset.PINNED -> "Pinned"
    state.filter.categoryId != null -> "Category"
    state.filter.type != null -> state.filter.type!!.label + "s"
    typeKey != null -> NoteType.fromStorageKey(typeKey)?.label ?: "Items"
    else -> "All items"
}

@Composable
private fun FilterChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
