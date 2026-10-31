package com.synfusion.snipnote.ui.categories

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.domain.model.CategoryIcons
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.ListPlaceholder
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.navigation.FloatingNavContentPadding
import com.synfusion.snipnote.ui.theme.CategoryPalette
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Categories: compact rows with icon, name and item count. Favorites and pinned remain filters
 * (see Home / search) rather than separate stored lists.
 */
@Composable
fun CategoriesScreen(
    container: AppContainer,
    twoPane: Boolean,
    onOpenCategory: (Long) -> Unit,
    onOpenTag: (Long) -> Unit,
    onOpenAll: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPinned: () -> Unit,
    onAdd: () -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = SnipViewModelFactories.categories(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extras = LocalAuroraExtras.current

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
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Categories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.categories.size} categories · ${state.totalItems} notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .clickable(role = Role.Button, onClick = viewModel::openCreate),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipButton(label = "All notes", icon = SnipIcons.category(CategoryIcons.DEFAULT), onClick = onOpenAll)
                    }
                    item {
                        FilterChipButton(
                            label = "Favorites",
                            icon = Icons.Outlined.StarBorder,
                            onClick = onOpenFavorites,
                        )
                    }
                    item {
                        FilterChipButton(
                            label = "Pinned",
                            icon = Icons.Filled.PushPin,
                            onClick = onOpenPinned,
                        )
                    }
                }
            }

            if (state.categories.isEmpty()) {
                item {
                    ListPlaceholder(
                        icon = Icons.Outlined.Category,
                        title = "No categories yet",
                        message = "Categories keep prompts, code and links apart. Create your first one.",
                        actionLabel = "New category",
                        onAction = viewModel::openCreate,
                    )
                }
            } else {
                items(state.categories, key = { it.category.id }) { item ->
                    CategoryRow(
                        item = item,
                        onClick = { onOpenCategory(item.category.id) },
                        onEdit = { viewModel.openEdit(item) },
                        onDelete = { viewModel.requestDelete(item) },
                    )
                }
            }

            if (state.tags.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text("Tags", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.tags, key = { it.id }) { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(extras.chipFill)
                                        .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(50))
                                        .clickable(role = Role.Button) { onOpenTag(tag.id) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#${tag.name}", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        tag.itemCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Deleting a category never deletes its notes – you always choose whether they are moved or left uncategorized.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    if (state.editor.isOpen) {
        CategoryEditorDialog(
            state = state.editor,
            onNameChange = viewModel::onEditorNameChange,
            onIconChange = viewModel::onEditorIconChange,
            onColorChange = viewModel::onEditorColorChange,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveCategory,
        )
    }

    state.deletion?.let { deletion ->
        DeleteCategoryDialog(
            deletion = deletion,
            categories = state.categories.filter { it.category.id != deletion.category?.category?.id },
            onChooseMove = { viewModel.chooseMoveTarget(it) },
            onChooseUncategorize = viewModel::chooseUncategorize,
            onDismiss = viewModel::cancelDelete,
            onConfirm = viewModel::confirmDelete,
        )
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(extras.chipFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CategoryRow(
    item: CategoryWithCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = Color(item.category.colorArgb)
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(20.dp)
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.glassFill)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = if (extras.isDark) 0.28f else 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                SnipIcons.category(item.category.iconKey),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.category.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                text = "${item.itemCount} " + if (item.itemCount == 1) "note" else "notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Options for ${item.category.name}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename or restyle") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    state: CategoryEditorState,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.editingId == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = { state.error?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryIcons.all) { iconKey ->
                        val selected = state.iconKey == iconKey
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { onIconChange(iconKey) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                SnipIcons.category(iconKey),
                                contentDescription = iconKey,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryPalette.options) { color ->
                        val selected = state.colorArgb == color
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { onColorChange(color) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.saving && state.name.isNotBlank()) {
                Text(if (state.editingId == null) "Create" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DeleteCategoryDialog(
    deletion: DeleteCategoryState,
    categories: List<CategoryWithCount>,
    onChooseMove: (Long?) -> Unit,
    onChooseUncategorize: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val category = deletion.category
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Delete \"${category?.category?.name ?: "category"}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${category?.itemCount ?: 0} note(s) use this category. Choose what happens to them – no note will be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onChooseUncategorize),
                ) {
                    RadioButton(selected = deletion.uncategorize, onClick = onChooseUncategorize)
                    Text("Keep notes, remove category", style = MaterialTheme.typography.bodySmall)
                }
                if (categories.isNotEmpty()) {
                    Text("Move notes to:", style = MaterialTheme.typography.labelLarge)
                    categories.take(8).forEach { target ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onChooseMove(target.category.id) },
                        ) {
                            RadioButton(
                                selected = !deletion.uncategorize && deletion.targetCategoryId == target.category.id,
                                onClick = { onChooseMove(target.category.id) },
                            )
                            Text(target.category.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !deletion.busy && (deletion.uncategorize || deletion.targetCategoryId != null || categories.isEmpty()),
            ) { Text(if (deletion.busy) "Deleting…" else "Delete category") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
