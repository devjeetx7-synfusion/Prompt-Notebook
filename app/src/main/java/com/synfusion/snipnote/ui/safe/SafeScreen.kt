package com.synfusion.snipnote.ui.safe

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.DeleteNoteDialog
import com.synfusion.snipnote.ui.components.ListPlaceholder
import com.synfusion.snipnote.ui.components.NoteAction
import com.synfusion.snipnote.ui.components.NoteActionSheet
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.navigation.FloatingNavContentPadding
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Safe workspace. Security is unchanged: the list is only populated while [SafeViewModel.state] is
 * unlocked, nothing about the locked contents is rendered, and private items never appear in Home,
 * Categories, lists or search.
 */
@Composable
fun SafeScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onEditNote: (Long) -> Unit,
    onOpenCreateHint: () -> Unit = {},
    viewModel: SafeViewModel = viewModel(factory = SnipViewModelFactories.safe(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var availability by remember { mutableStateOf(SafeAuthenticator.availability(context)) }
    var actionTarget by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }
    val extras = LocalAuroraExtras.current

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    fun unlock() {
        if (activity == null) {
            viewModel.onUnlockFailed("Safe needs to run inside the app activity")
            return
        }
        SafeAuthenticator.authenticate(
            activity = activity,
            session = container.safeSession,
            onSuccess = viewModel::onUnlockSucceeded,
            onFailure = viewModel::onUnlockFailed,
            onCancelled = viewModel::onUnlockCancelled,
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!state.unlocked) {
            LockedSafe(
                availability = availability,
                onUnlock = { unlock() },
                onRecheck = { availability = SafeAuthenticator.availability(context) },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
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
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(extras.secure.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = extras.secure,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Safe", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${state.notes.size} of ${state.secureCount} encrypted item(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = viewModel::lock) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Lock")
                        }
                    }
                }

                item {
                    val shape = RoundedCornerShape(18.dp)
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search private items", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = shape,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.notes.isEmpty()) {
                    item {
                        ListPlaceholder(
                            icon = Icons.Outlined.Shield,
                            title = if (state.query.isBlank()) "Safe is empty" else "No matches in Safe",
                            message = if (state.query.isBlank()) {
                                "Tap + while you are on this tab to add a private item. It is encrypted at rest and hidden everywhere else in the app."
                            } else {
                                "Nothing in your Safe matches this search."
                            },
                            actionLabel = if (state.query.isBlank()) "Create a private item" else null,
                            onAction = if (state.query.isBlank()) onOpenCreateHint else null,
                        )
                    }
                } else {
                    items(state.notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onLongClick = { actionTarget = note },
                            onOverflow = { actionTarget = note },
                        )
                    }
                }

                item {
                    Text(
                        "Items are encrypted with AES-GCM using a key in this device's Android Keystore. If that key is lost, no copy exists anywhere.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    actionTarget?.let { note ->
        NoteActionSheet(
            note = note,
            onDismiss = { actionTarget = null },
            available = setOf(
                NoteAction.COPY,
                NoteAction.EDIT,
                NoteAction.TOGGLE_FAVORITE,
                NoteAction.TOGGLE_PRIVATE,
                NoteAction.DELETE,
            ),
            onAction = { action ->
                actionTarget = null
                when (action) {
                    NoteAction.COPY -> viewModel.copy(note)
                    NoteAction.EDIT -> onEditNote(note.id)
                    NoteAction.TOGGLE_FAVORITE -> viewModel.toggleFavorite(note)
                    NoteAction.TOGGLE_PRIVATE -> viewModel.togglePrivate(note)
                    NoteAction.DELETE -> deleteTarget = note
                    NoteAction.MOVE_CATEGORY -> Unit
                }
            },
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
private fun LockedSafe(
    availability: SafeAuthAvailability,
    onUnlock: () -> Unit,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAuroraExtras.current
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(extras.secure.copy(alpha = 0.26f), MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = extras.secure, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text("Safe is locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = "Private prompts, keys and drafts are encrypted on this device and hidden from search, lists and backups until you unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(20.dp))
        when (availability) {
            is SafeAuthAvailability.Ready -> Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(availability.label)
            }
            is SafeAuthAvailability.Unavailable -> {
                OutlinedButton(onClick = onRecheck) { Text("Re-check device security") }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    availability.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.size(18.dp))
        Text(
            "Encryption keys never leave this device. If the key is lost (factory reset, cleared credentials), Safe items cannot be recovered – there is no cloud copy.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
