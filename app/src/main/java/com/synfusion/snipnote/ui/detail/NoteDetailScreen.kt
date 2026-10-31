package com.synfusion.snipnote.ui.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.DeleteNoteDialog
import com.synfusion.snipnote.ui.components.ListPlaceholder
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import com.synfusion.snipnote.ui.theme.typeStyle
import com.synfusion.snipnote.util.TimeFormat

/**
 * Content-first detail screen: a compact action row (Copy / Edit / Favorite / More) followed by the
 * full text. Secondary actions live in the overflow menu so they are never duplicated.
 */
@Composable
fun NoteDetailScreen(
    container: AppContainer,
    secure: Boolean,
    onBack: () -> Unit,
    onEdit: (Long, Boolean) -> Unit,
    onDeleted: () -> Unit,
    onMoveCategory: (Note) -> Unit = {},
    viewModel: DetailViewModel = viewModel(factory = SnipViewModelFactories.detail(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extras = LocalAuroraExtras.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val note = state.note

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    fun copy() {
        val current = note ?: return
        val ok = container.clipboard.copy(
            label = current.displayTitle,
            text = current.content,
            sensitive = current.isSecure,
        )
        viewModel.setMessage(
            if (ok) "Copied ${current.content.length} characters" else "Copy failed – the clipboard is unavailable",
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DetailTopBar(
                onBack = onBack,
                onMenu = if (note != null) ({ menuOpen = true }) else null,
                menuOpen = menuOpen,
                onDismissMenu = { menuOpen = false },
                note = note,
                onTogglePinned = { menuOpen = false; viewModel.togglePinned() },
                onMoveCategory = { menuOpen = false; note?.let(onMoveCategory) },
                onTogglePrivate = { menuOpen = false; viewModel.togglePrivate() },
                onDelete = { menuOpen = false; confirmDelete = true },
            )

            when {
                state.requiresUnlock -> ListPlaceholder(
                    icon = Icons.Filled.Lock,
                    title = "Safe is locked",
                    message = "Unlock the Safe workspace to read this item.",
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                note == null && !state.loading -> ListPlaceholder(
                    icon = Icons.Filled.Lock,
                    title = "Note not found",
                    message = "This note may have been deleted or moved into the Safe.",
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                note == null -> Box(modifier = Modifier.fillMaxSize())
                else -> DetailContent(
                    note = note,
                    onCopy = { copy() },
                    onEdit = { onEdit(note.id, note.isSecure) },
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }

    if (confirmDelete && note != null) {
        DeleteNoteDialog(
            title = note.displayTitle,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    onMenu: (() -> Unit)?,
    menuOpen: Boolean,
    onDismissMenu: () -> Unit,
    note: Note?,
    onTogglePinned: () -> Unit,
    onMoveCategory: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        Spacer(modifier = Modifier.weight(1f))
        if (onMenu != null) {
            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LocalAuroraExtras.current.chipFill)
                        .clickable(role = Role.Button, onClick = onMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More actions", modifier = Modifier.size(19.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(
                        text = { Text(if (note?.isPinned == true) "Unpin" else "Pin to top") },
                        leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                        onClick = onTogglePinned,
                    )
                    DropdownMenuItem(
                        text = { Text("Move to category") },
                        leadingIcon = { Icon(Icons.Filled.DriveFileMove, contentDescription = null) },
                        onClick = onMoveCategory,
                    )
                    DropdownMenuItem(
                        text = { Text(if (note?.isSecure == true) "Move out of Safe" else "Move to Safe") },
                        leadingIcon = {
                            Icon(
                                if (note?.isSecure == true) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                            )
                        },
                        onClick = onTogglePrivate,
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    note: Note,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val style = typeStyle(note.type, extras.isDark)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(style.container)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(SnipIcons.type(note.type), contentDescription = null, tint = style.tint, modifier = Modifier.size(14.dp))
                Text(note.type.label, style = MaterialTheme.typography.labelMedium, color = style.tint)
            }
            if (note.isSecure) {
                Icon(Icons.Filled.Lock, contentDescription = "Private", tint = extras.secure, modifier = Modifier.size(14.dp))
            }
            if (note.isPinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = extras.pin, modifier = Modifier.size(14.dp))
            }
        }

        Text(
            text = note.displayTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = TimeFormat.full(note.updatedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ActionRow(
            onCopy = onCopy,
            onEdit = onEdit,
            onFavorite = onToggleFavorite,
            isFavorite = note.isFavorite,
        )

        if (!note.isReadable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
                    .padding(14.dp),
            ) {
                Text(
                    "The encryption key for this item is missing on this device. The encrypted content is still stored and has not been deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(extras.glassFill)
                    .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                SelectionContainer {
                    Text(
                        text = note.content.ifBlank { "(no content)" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (note.type == NoteType.CODE) FontFamily.Monospace else FontFamily.Default,
                        ),
                        modifier = if (note.type == NoteType.CODE) {
                            Modifier.horizontalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }

        note.category?.let { category ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(category.colorArgb).copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        SnipIcons.category(category.iconKey),
                        contentDescription = null,
                        tint = Color(category.colorArgb),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(category.name, style = MaterialTheme.typography.labelMedium, color = Color(category.colorArgb))
                }
            }
        }

        if (note.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.tags.forEach { tag ->
                    Text(
                        text = "#${tag.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(40.dp))
    }
}

/** Copy / Edit / Favorite / More – the only actions that stay visible on the detail screen. */
@Composable
private fun ActionRow(
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean,
) {
    val extras = LocalAuroraExtras.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(extras.glassFill)
            .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailAction(Icons.Filled.ContentCopy, "Copy", onCopy)
        DetailAction(Icons.Filled.Edit, "Edit", onEdit)
        DetailAction(
            icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            label = "Favorite",
            onClick = onFavorite,
            tint = if (isFavorite) extras.favorite else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailAction(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** Padding helper kept for parity with the floating navigation in other screens. */
internal val DetailBottomPadding = PaddingValues(bottom = 32.dp)
