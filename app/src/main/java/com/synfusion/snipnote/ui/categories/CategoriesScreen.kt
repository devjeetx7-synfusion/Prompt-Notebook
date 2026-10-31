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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Category
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.domain.model.CategoryIcons
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.CategoryPalette
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

@Composable
fun CategoriesScreen(
    container: AppContainer,
    twoPane: Boolean,
    onOpenCategory: (Long) -> Unit,
    onOpenTag: (Long) -> Unit,
    onOpenAll: () -> Unit,
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

    Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Categories", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${state.categories.size} categories · ${state.totalItems} notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = viewModel::openCreate) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButtonPill("All notes", Icons.Outlined.Category, onOpenAll)
                }
            }

            if (state.categories.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Category,
                        title = "No categories yet",
                        message = "Categories keep prompts, code and links apart. Create your first one.",
                        actionLabel = "New category",
                        onAction = viewModel::openCreate,
                    )
                }
            } else {
                val columns = if (twoPane) 3 else 2
                items(state.categories.chunked(columns).size) { rowIndex ->
                    val row = state.categories.chunked(columns)[rowIndex]
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { index, category ->
                            CategoryCard(
                                item = category,
                                onOpen = { onOpenCategory(category.category.id) },
                                onEdit = { viewModel.openEdit(category) },
                                onDelete = { viewModel.requestDelete(category) },
                                onMoveUp = if (rowIndex * columns + index > 0) {
                                    { viewModel.moveCategory(rowIndex * columns + index, rowIndex * columns + index - 1) }
                                } else null,
                                onMoveDown = if (rowIndex * columns + index < state.categories.lastIndex) {
                                    { viewModel.moveCategory(rowIndex * columns + index, rowIndex * columns + index + 1) }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            if (state.tags.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tags", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap a tag to see every note that uses it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.tags, key = { it.id }) { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(extras.glassFill)
                                        .border(BorderStroke(1.dp, extras.glassBorder), RoundedCornerShape(50))
                                        .clickable { onOpenTag(tag.id) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#${tag.name}", style = MaterialTheme.typography.labelLarge)
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
                    "Deleting a category never deletes its notes – you always choose whether they are moved or left uncategorized.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
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
private fun OutlinedButtonPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(extras.glassFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CategoryCard(
    item: CategoryWithCount,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val color = Color(item.category.colorArgb)
    val shape = RoundedCornerShape(22.dp)
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f), shape)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), shape)
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    SnipIcons.category(item.category.iconKey),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Category options", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    if (onMoveUp != null) {
                        DropdownMenuItem(text = { Text("Move earlier") }, onClick = { menuOpen = false; onMoveUp() })
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(text = { Text("Move later") }, onClick = { menuOpen = false; onMoveDown() })
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
        Text(item.category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(
            "${item.itemCount} note" + if (item.itemCount == 1) "" else "s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.category.isDefault) {
            Text("Default", style = MaterialTheme.typography.labelSmall, color = color)
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
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                .size(40.dp)
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
                                modifier = Modifier.size(20.dp),
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { onColorChange(color) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${category?.itemCount ?: 0} note(s) use this category. Choose what happens to them – no note will be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onChooseUncategorize),
                ) {
                    RadioButton(selected = deletion.uncategorize, onClick = onChooseUncategorize)
                    Text("Keep notes, remove category (uncategorized)", style = MaterialTheme.typography.bodySmall)
                }
                if (categories.isNotEmpty()) {
                    Text("Move notes to:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.take(10).forEach { target ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onChooseMove(target.category.id) },
                            ) {
                                RadioButton(
                                    selected = !deletion.uncategorize && deletion.targetCategoryId == target.category.id,
                                    onClick = { onChooseMove(target.category.id) },
                                )
                                Text(target.category.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${target.itemCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "This is the only category, so its notes will become uncategorized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
