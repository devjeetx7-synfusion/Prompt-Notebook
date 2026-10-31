package com.synfusion.snipnote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.data.repository.CategoryWithCount
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.domain.model.SortOrder
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Horizontal, scrollable filter strip shared by search and list screens.
 * Favorites / pinned are filters over the same rows, never separate collections.
 */
@Composable
fun NoteFilterBar(
    selectedType: NoteType?,
    favoritesOnly: Boolean,
    pinnedOnly: Boolean,
    categories: List<CategoryWithCount>,
    selectedCategoryId: Long?,
    sort: SortOrder,
    onTypeSelected: (NoteType?) -> Unit,
    onToggleFavorites: () -> Unit,
    onTogglePinned: () -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var categoryMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val extras = LocalAuroraExtras.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(label = "All types", selected = selectedType == null, onClick = { onTypeSelected(null) })
        NoteType.entries.forEach { type ->
            Pill(
                label = type.label,
                selected = selectedType == type,
                onClick = { onTypeSelected(if (selectedType == type) null else type) },
                leadingIcon = SnipIcons.type(type),
            )
        }
        Pill(
            label = "Favorites",
            selected = favoritesOnly,
            onClick = onToggleFavorites,
            leadingIcon = Icons.Filled.Star,
        )
        Pill(
            label = "Pinned",
            selected = pinnedOnly,
            onClick = onTogglePinned,
            leadingIcon = Icons.Filled.PushPin,
        )
        Box {
            Pill(
                label = categories.firstOrNull { it.category.id == selectedCategoryId }?.category?.name ?: "Any category",
                selected = selectedCategoryId != null,
                onClick = { categoryMenu = true },
                leadingIcon = Icons.Filled.ArrowDropDown,
            )
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Any category") },
                    onClick = { onCategorySelected(null); categoryMenu = false },
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.category.name) },
                        onClick = { onCategorySelected(category.category.id); categoryMenu = false },
                    )
                }
            }
        }
        Box {
            Pill(
                label = sort.label,
                selected = false,
                onClick = { sortMenu = true },
                leadingIcon = Icons.Filled.Sort,
            )
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SortOrder.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { onSortSelected(option); sortMenu = false },
                    )
                }
            }
        }
        if (onClear != null) {
            val shape = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(extras.glassFill, shape)
                    .border(BorderStroke(1.dp, extras.glassBorder), shape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ResultSummary(count: Int, noun: String = "note", modifier: Modifier = Modifier) {
    Text(
        text = "$count ${if (count == 1) noun else "${noun}s"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
