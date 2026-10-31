package com.synfusion.snipnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.domain.model.Category
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/** Actions offered by the long-press / overflow context menu. */
enum class NoteAction(val label: String) {
    COPY("Copy"),
    EDIT("Edit"),
    TOGGLE_FAVORITE("Favorite"),
    MOVE_CATEGORY("Move to category"),
    TOGGLE_PRIVATE("Move to Safe"),
    DELETE("Delete"),
}

/**
 * Contextual action sheet for a single note. Opened by long-press on a list row or by the row's
 * overflow button – the row itself never shows these actions permanently.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionSheet(
    note: Note,
    onAction: (NoteAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    available: Set<NoteAction> = NoteAction.entries.toSet(),
) {
    val extras = LocalAuroraExtras.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = note.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = note.type.label + " · " + (note.category?.name ?: "Uncategorized"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (NoteAction.COPY in available) ActionRow(
                icon = Icons.Filled.ContentCopy,
                label = "Copy ${
                    when (note.type) {
                        com.synfusion.snipnote.domain.model.NoteType.LINK -> "link"
                        com.synfusion.snipnote.domain.model.NoteType.CODE -> "code"
                        else -> "content"
                    }
                }",
                enabled = note.isReadable,
            ) { onAction(NoteAction.COPY) }
            if (NoteAction.EDIT in available) {
                ActionRow(icon = Icons.Filled.Edit, label = "Edit") { onAction(NoteAction.EDIT) }
            }
            if (NoteAction.TOGGLE_FAVORITE in available) ActionRow(
                icon = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                label = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (note.isFavorite) extras.favorite else null,
            ) { onAction(NoteAction.TOGGLE_FAVORITE) }
            if (NoteAction.MOVE_CATEGORY in available) {
                ActionRow(icon = Icons.Filled.DriveFileMove, label = "Move to category") {
                    onAction(NoteAction.MOVE_CATEGORY)
                }
            }
            if (NoteAction.TOGGLE_PRIVATE in available) ActionRow(
                icon = if (note.isSecure) Icons.Filled.LockOpen else Icons.Filled.Lock,
                label = if (note.isSecure) "Move out of Safe" else "Move to Safe",
                tint = extras.secure,
            ) { onAction(NoteAction.TOGGLE_PRIVATE) }
            if (NoteAction.DELETE in available) {
                ActionRow(icon = Icons.Filled.Delete, label = "Delete", tint = extras.danger) {
                    onAction(NoteAction.DELETE)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val color = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        tint != null -> tint
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** Category picker shown by [NoteAction.MOVE_CATEGORY]. */
@Composable
fun MoveCategoryDialog(
    categories: List<Category>,
    currentCategoryId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to category") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    CategoryOption(
                        name = "Uncategorized",
                        selected = currentCategoryId == null,
                        onClick = { onPick(null) },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    CategoryOption(
                        name = category.name,
                        selected = currentCategoryId == category.id,
                        color = Color(category.colorArgb),
                        onClick = { onPick(category.id) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CategoryOption(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (color != null) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
        Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Destructive confirmation used before a note is deleted. */
@Composable
fun DeleteNoteDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String = "This permanently removes the note from this device. It cannot be undone.",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Delete \"$title\"?") },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
