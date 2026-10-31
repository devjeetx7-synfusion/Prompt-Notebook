package com.synfusion.snipnote.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import com.synfusion.snipnote.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    container: AppContainer,
    secure: Boolean,
    onBack: () -> Unit,
    onEdit: (Long, Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = SnipViewModelFactories.detail(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extras = LocalAuroraExtras.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val note: Note? = state.note

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    fun copy() {
        val current = note ?: return
        val success = container.clipboard.copy(
            label = current.displayTitle,
            text = current.content,
            sensitive = current.isSecure,
        )
        viewModel.setMessage(if (success) "Copied ${current.content.length} characters" else "Copy failed – the clipboard is unavailable")
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(note?.type?.label ?: "Note", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (note != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (note.isFavorite) extras.favorite else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onEdit(note.id, note.isSecure) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Copy content") },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                    onClick = { menuOpen = false; copy() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) "Unpin" else "Pin to top") },
                                    leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                                    onClick = { menuOpen = false; viewModel.togglePinned() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.requiresUnlock -> EmptyState(
                    icon = Icons.Filled.Lock,
                    title = "Safe is locked",
                    message = "Unlock the Safe workspace to read this item.",
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                note == null && !state.loading -> EmptyState(
                    icon = Icons.Filled.Lock,
                    title = "Note not found",
                    message = "This note may have been deleted or moved into the Safe.",
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                note == null -> Box(modifier = Modifier.fillMaxSize())
                else -> NoteBody(note = note, onCopy = { copy() })
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This permanently removes the note from this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDeleted) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NoteBody(note: Note, onCopy: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val tint = note.category?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SnipIcons.type(note.type), contentDescription = note.type.label, tint = tint, modifier = Modifier.size(17.dp))
            }
            note.category?.let {
                Text(it.name, style = MaterialTheme.typography.labelLarge, color = tint)
            }
            if (note.isSecure) {
                Icon(Icons.Filled.Lock, contentDescription = "Private", tint = extras.secure, modifier = Modifier.size(14.dp))
            }
            if (note.isPinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = extras.pin, modifier = Modifier.size(14.dp))
            }
        }

        Text(note.displayTitle, style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "Created ${TimeFormat.full(note.createdAt)} · Updated ${TimeFormat.full(note.updatedAt)} · ${note.content.length} characters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!note.isReadable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                    .padding(12.dp),
            ) {
                Text(
                    "The encryption key for this item is missing on this device. The encrypted content is still stored and has not been deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            SelectionContainer {
                Text(
                    text = note.content,
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

        if (note.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(extras.glassFill)
                            .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("#${tag.name}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onCopy, enabled = note.isReadable) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy content")
            }
            Text("Copies the exact text, without truncation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.size(40.dp))
    }
}
