package com.synfusion.snipnote.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.synfusion.snipnote.ui.navigation.FloatingNavContentPadding
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Home: branding, search, compact type filters and the recent item list.
 * Every creation path goes through the single "+" in the floating navigation pill.
 */
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditNote: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = SnipViewModelFactories.home(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionTarget by remember { mutableStateOf<Note?>(null) }
    var moveTarget by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HomeHeader(
                    onOpenSearch = onOpenSearch,
                    onOpenSettings = onOpenSettings,
                )
            }
            item { HeroHeadline() }
            item { SearchPill(onOpenSearch) }
            item {
                TypeFilterRow(
                    selected = state.filterType,
                    onSelect = viewModel::setFilter,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (state.filterType) {
                            null -> "Recent"
                            NoteType.PROMPT -> "Prompts"
                            NoteType.CODE -> "Code"
                            NoteType.LINK -> "Links"
                            NoteType.NOTE -> "Notes"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.totalCount > state.items.size) {
                        Text(
                            text = "${state.totalCount} total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.items.isEmpty()) {
                item {
                    ListPlaceholder(
                        icon = Icons.Outlined.Notes,
                        title = if (state.filterType == null) "Nothing captured yet" else "Nothing here yet",
                        message = if (state.filterType == null) {
                            "Use the + button below to save a prompt, a note, code or a link. Everything stays on this device."
                        } else {
                            "You have no ${state.filterType?.label?.lowercase()} saved yet. Add one with the + button below."
                        },
                    )
                }
            } else {
                items(state.items, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { actionTarget = note },
                        onOverflow = { actionTarget = note },
                    )
                }
                if (state.totalCount > state.items.size) {
                    item {
                        Text(
                            text = "Showing the ${state.items.size} most recent of ${state.totalCount}. Use search to find the rest.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
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
            onPick = { categoryId ->
                viewModel.moveToCategory(note, categoryId)
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
private fun HomeHeader(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, LocalAuroraExtras.current.pink),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SnipIcons.type(NoteType.PROMPT),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "SnipNote",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalAuroraExtras.current.chipFill)
                .clickable(role = Role.Button, onClick = onOpenSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HeroHeadline() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Text(
            text = "Your ideas,",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "organized.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 40.sp,
                brush = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                ),
            ),
        )
    }
}

@Composable
private fun SearchPill(onOpenSearch: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.glassFill)
            .clickable(role = Role.Button, onClick = onOpenSearch)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Search notes, prompts, code, links…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun TypeFilterRow(selected: NoteType?, onSelect: (NoteType?) -> Unit) {
    val options: List<Pair<String, ImageVector?>> = listOf(
        "All" to null,
        "Prompts" to Icons.Filled.AutoAwesome,
        "Code" to Icons.Filled.Code,
        "Links" to Icons.Filled.Link,
        "Notes" to Icons.Outlined.Notes,
    )
    val types: List<NoteType?> = listOf(null, NoteType.PROMPT, NoteType.CODE, NoteType.LINK, NoteType.NOTE)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options.size) { index ->
            TypeFilterChip(
                label = options[index].first,
                icon = options[index].second,
                selected = selected == types[index],
                onClick = { onSelect(if (selected == types[index]) null else types[index]) },
            )
        }
    }
}

@Composable
private fun TypeFilterChip(
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}
