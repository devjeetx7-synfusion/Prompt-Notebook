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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.components.SearchField
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

@Composable
fun SafeScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onCreateNote: (NoteType?) -> Unit,
    viewModel: SafeViewModel = viewModel(factory = SnipViewModelFactories.safe(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var availability by remember { mutableStateOf(SafeAuthenticator.availability(context)) }
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
        floatingActionButton = {
            if (state.unlocked) {
                ExtendedFloatingActionButton(
                    onClick = { onCreateNote(NoteType.NOTE) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New private item") },
                )
            }
        },
    ) { padding ->
        if (!state.unlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(extras.secure.copy(alpha = 0.28f), MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = extras.secure,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(modifier = Modifier.size(18.dp))
                Text("Safe is locked", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (state.secureCount == 0) {
                        "Private prompts, keys and drafts live here. Each item is encrypted with a key held in this device's Android Keystore."
                    } else {
                        "${state.secureCount} encrypted item(s) are hidden from search, lists and backups until you unlock."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(22.dp))
                when (val current = availability) {
                    is SafeAuthAvailability.Ready -> {
                        Button(onClick = { unlock() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(current.label)
                        }
                    }
                    is SafeAuthAvailability.Unavailable -> {
                        OutlinedButton(onClick = { availability = SafeAuthenticator.availability(context) }) {
                            Text("Re-check device security")
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            current.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    "Encryption keys never leave this device. If the key is lost (factory reset, cleared credentials), Safe items cannot be recovered – there is no cloud copy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Safe", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "${state.notes.size} of ${state.secureCount} private item(s) shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = viewModel::lock) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock now")
                        }
                    }
                }
                item {
                    SearchField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = "Search private items",
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SafeCreateButton(Icons.Filled.AutoAwesome, "Prompt", extras.secure) { onCreateNote(NoteType.PROMPT) }
                        SafeCreateButton(Icons.Filled.EditNote, "Note", extras.secure) { onCreateNote(NoteType.NOTE) }
                        SafeCreateButton(Icons.Filled.Code, "Code", extras.secure) { onCreateNote(NoteType.CODE) }
                    }
                }
                if (state.notes.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Shield,
                            title = if (state.query.isBlank()) "Safe is empty" else "No matches in Safe",
                            message = if (state.query.isBlank()) {
                                "Add a private prompt or key. It is encrypted at rest and hidden everywhere else in the app."
                            } else {
                                "Nothing in your Safe matches this search."
                            },
                        )
                    }
                } else {
                    items(state.notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeCreateButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(extras.glassFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
        Icon(Icons.Filled.Add, contentDescription = "New private $label", modifier = Modifier.size(14.dp))
    }
}
