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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.domain.model.Note
import com.synfusion.snipnote.domain.model.NoteType
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras
import com.synfusion.snipnote.ui.theme.typeStyle
import com.synfusion.snipnote.util.TextUtils
import com.synfusion.snipnote.util.TimeFormat

/**
 * The single list row used everywhere a note is listed.
 *
 * Shows only what the reference design shows: type icon, title, short preview, one label and the
 * updated timestamp. No permanent action buttons – tap opens the note, long press or the trailing
 * overflow opens the contextual action sheet.
 */
@Composable
fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
    now: Long = System.currentTimeMillis(),
) {
    val extras = LocalAuroraExtras.current
    val style = typeStyle(note.type, extras.isDark)
    val shape = RoundedCornerShape(20.dp)
    val label = note.category?.name ?: note.type.label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.glassFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Open ${note.type.label}",
                onLongClickLabel = "Show actions",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
            .semantics { contentDescription = "${note.type.label}: ${note.displayTitle}" },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(style.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SnipIcons.type(note.type),
                contentDescription = null,
                tint = style.tint,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (note.isPinned) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = extras.pin,
                        modifier = Modifier.size(13.dp),
                    )
                }
                if (note.isFavorite) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = extras.favorite,
                        modifier = Modifier.size(13.dp),
                    )
                }
                if (note.isSecure) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Private",
                        tint = extras.secure,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Text(
                text = if (note.isReadable) {
                    TextUtils.preview(note.preview, 120).ifBlank { "No content yet" }
                } else {
                    "Content unavailable – encryption key missing"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (note.type == NoteType.CODE && note.isReadable) FontFamily.Monospace else FontFamily.Default,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                GlassChip(
                    text = label,
                    container = style.container,
                    contentColor = style.tint,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = TimeFormat.relative(note.updatedAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onOverflow != null) {
            IconButton(onClick = onOverflow, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Actions for ${note.displayTitle}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Compact tile used by the category screen to show a category's visual identity. */
@Composable
fun TypeBadge(type: NoteType, modifier: Modifier = Modifier) {
    val extras = LocalAuroraExtras.current
    val style = typeStyle(type, extras.isDark)
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(style.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SnipIcons.type(type), contentDescription = null, tint = style.tint, modifier = Modifier.size(15.dp))
    }
}

/** Shared empty-state block (kept here so every list looks identical). */
@Composable
fun ListPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 180.dp).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Previews the row rendering in isolation (used by Compose previews and UI tests). */
@Composable
internal fun NoteRowDivider(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 1.dp).background(color))
}
