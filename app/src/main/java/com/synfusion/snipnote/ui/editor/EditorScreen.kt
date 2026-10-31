@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.synfusion.snipnote.ui.editor

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import com.synfusion.snipnote.ui.theme.typeStyle

/**
 * Universal editor. The "+" button opens this screen directly – there is no type-picker sheet.
 * Type is inferred while typing, shown as an advisory chip and always overridable.
 */
@Composable
fun EditorScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = SnipViewModelFactories.editor(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extras = LocalAuroraExtras.current
    var showDiscard by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var tagInputVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedId) {
        state.savedId?.let { id -> onSaved(id, state.isSecure) }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val requestBack: () -> Unit = {
        if (state.isDirty && state.savedId == null) showDiscard = true else onBack()
    }
    BackHandler(enabled = true) { requestBack() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            EditorTopBar(
                title = if (state.isNew) "Create New Item" else "Edit Item",
                saving = state.saving,
                canSave = state.canSave,
                onBack = requestBack,
                onSave = viewModel::save,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.sessionExpired) {
                    InlineNotice("The Safe locked before this item could be opened. Go back and unlock again.")
                }
                if (state.unreadable) {
                    InlineNotice("This item's encryption key is unavailable, so its content cannot be shown. Saving will overwrite it.")
                }

                FieldLabel("Title")
                EditorField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    placeholder = "Build a complete Android app",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldLabel("Content", modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.OpenInFull,
                        contentDescription = "Content supports multiple lines",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                EditorField(
                    value = state.content,
                    onValueChange = viewModel::onContentChange,
                    placeholder = "You are a professional Android developer. Build a complete Android application…",
                    minLines = 8,
                    modifier = Modifier.heightIn(min = 220.dp),
                    monospace = state.type == NoteType.CODE,
                )

                DetectedTypeRow(
                    type = state.type,
                    detectionHint = state.detection?.takeIf { it.suggestedType != state.type }?.let {
                        "Auto-detected: ${it.suggestedType.label}"
                    },
                    onOpenTypeMenu = { typeMenu = true },
                    typeMenuExpanded = typeMenu,
                    onDismissTypeMenu = { typeMenu = false },
                    onPickType = { type ->
                        viewModel.onTypeChange(type)
                        typeMenu = false
                    },
                )

                FieldLabel("Category")
                Box {
                    val current = state.categories.firstOrNull { it.id == state.categoryId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(extras.glassFill)
                            .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { categoryMenu = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = current?.let { SnipIcons.category(it.iconKey) } ?: Icons.Filled.Star,
                            contentDescription = null,
                            tint = current?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = current?.name ?: "Uncategorized",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ExpandMore, contentDescription = "Choose category", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Uncategorized") },
                            onClick = {
                                viewModel.onCategoryChange(null)
                                categoryMenu = false
                            },
                        )
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        SnipIcons.category(category.iconKey),
                                        contentDescription = null,
                                        tint = Color(category.colorArgb),
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    viewModel.onCategoryChange(category.id)
                                    categoryMenu = false
                                },
                            )
                        }
                    }
                }

                // Tags stay available but collapsed, so the primary form matches the reference.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { tagInputVisible = !tagInputVisible }) {
                        Text(if (tagInputVisible) "Hide tags" else "Add tags")
                    }
                    if (!tagInputVisible && state.tags.isNotEmpty()) {
                        Text(
                            text = state.tags.joinToString(", ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (tagInputVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.tagInput,
                            onValueChange = viewModel::onTagInputChange,
                            label = { Text("Tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = viewModel::addTag, enabled = state.tagInput.isNotBlank()) { Text("Add") }
                    }
                    if (state.tags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.tags) { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(extras.chipFill)
                                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#$tag", style = MaterialTheme.typography.labelMedium)
                                    IconButton(onClick = { viewModel.removeTag(tag) }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove tag $tag",
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (state.isFavorite) extras.favorite else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favorite", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    KSwitch(checked = state.isFavorite, onCheckedChange = { viewModel.toggleFavorite() })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Private item", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = if (state.isSecure) "In Safe" else "Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    KSwitch(checked = state.isSecure, onCheckedChange = { viewModel.toggleSecure() })
                }
                if (state.isSecure) {
                    InlineNotice(
                        "Encrypted with an AES-GCM key held in the Android Keystore and hidden from normal lists, search and backups.",
                        tint = extras.secure,
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.navigationBarsPadding().size(20.dp))
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Leaving now will lose them.") },
            confirmButton = { TextButton(onClick = { showDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    saving: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Button(
            onClick = onSave,
            enabled = canSave && !saving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(if (saving) "…" else "Save", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        modifier = modifier,
    )
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(18.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        },
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        ),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = extras.glassFillStrong,
            unfocusedContainerColor = extras.glassFill,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = extras.glassBorder,
        ),
    )
}

@Composable
private fun DetectedTypeRow(
    type: NoteType,
    detectionHint: String?,
    onOpenTypeMenu: () -> Unit,
    typeMenuExpanded: Boolean,
    onDismissTypeMenu: () -> Unit,
    onPickType: (NoteType) -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val style = typeStyle(type, extras.isDark)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(extras.glassFill)
                .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(18.dp))
                .clickable(role = Role.Button, onClick = onOpenTypeMenu)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(SnipIcons.type(type), contentDescription = null, tint = style.tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detectionHint ?: "Content type: ${type.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (detectionHint != null) style.tint else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${type.label} · tap to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = "Change content type", modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = onDismissTypeMenu) {
            NoteType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.label}${if (option == type) "  ✓" else ""}") },
                    leadingIcon = {
                        val optionStyle = typeStyle(option, extras.isDark)
                        Icon(SnipIcons.type(option), contentDescription = null, tint = optionStyle.tint)
                    },
                    onClick = { onPickType(option) },
                )
            }
        }
    }
}

@Composable
private fun InlineNotice(message: String, tint: Color? = null) {
    val extras = LocalAuroraExtras.current
    val accent = tint ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.13f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

/** Minimal, accessible switch rendered as a pill to match the glass aesthetic. */
@Composable
private fun KSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val extras = LocalAuroraExtras.current
    val trackColor = if (checked) extras.secure else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
