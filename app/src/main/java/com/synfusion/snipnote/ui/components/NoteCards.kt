@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.synfusion.snipnote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import com.synfusion.snipnote.util.TextUtils
import com.synfusion.snipnote.util.TimeFormat

/**
 * Full-width row used in lists and search results.
 * Long-press starts bulk selection; a single tap opens the note.
 */
@Composable
fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showFavoriteButton: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
    now: Long = System.currentTimeMillis(),
) {
    val extras = LocalAuroraExtras.current
    val tint = note.category?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            extras.glassFill
        },
        label = "noteRowFill",
    )
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container, shape)
            .border(BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else extras.glassBorder), shape)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SnipIcons.type(note.type),
                    contentDescription = note.type.label,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = note.displayTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (note.isPinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = extras.pin,
                    modifier = Modifier.size(15.dp),
                )
            }
            if (note.isSecure) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "In Safe",
                    tint = extras.secure,
                    modifier = Modifier.size(15.dp),
                )
            }
            if (showFavoriteButton && onToggleFavorite != null) {
                androidx.compose.material3.IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (note.isFavorite) extras.favorite else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (note.preview.isNotBlank()) {
            Text(
                text = if (note.isReadable) TextUtils.preview(note.preview, 220) else "Content unavailable – the encryption key for this item is missing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (note.type == com.synfusion.snipnote.domain.model.NoteType.CODE && note.isReadable) {
                    FontFamily.Monospace
                } else {
                    FontFamily.Default
                },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            note.category?.let { category ->
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                )
            }
            note.tags.take(2).forEach { tag ->
                Text(
                    text = "#${tag.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = TimeFormat.relative(note.updatedAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Compact tinted tile for the Home masonry-ish grid, echoing the reference design.
 * Height is driven by content with a sensible minimum so rows stay balanced.
 */
@Composable
fun NoteTile(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val tint = note.category?.colorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .heightIn(min = 148.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = 0.34f),
                        tint.copy(alpha = 0.16f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.28f)), shape)
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
            .semantics { contentDescription = "${note.type.label}: ${note.displayTitle}" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = SnipIcons.type(note.type),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (note.isPinned) {
                Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = tint, modifier = Modifier.size(14.dp))
            }
            if (note.isFavorite) {
                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = tint, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            text = note.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.preview.isNotBlank()) {
            Text(
                text = if (note.isReadable) TextUtils.preview(note.preview, 90) else "Locked content",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.weight(1f, fill = true))
        Text(
            text = TimeFormat.dayMonthYear(note.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

/** Small leading checkbox shown while bulk selection is active. */
@Composable
fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = if (selected) "Selected" else "Not selected",
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = modifier.size(22.dp),
    )
}
