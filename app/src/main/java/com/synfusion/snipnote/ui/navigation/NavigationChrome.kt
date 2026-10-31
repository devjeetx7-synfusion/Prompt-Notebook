package com.synfusion.snipnote.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.ui.components.SnipIcons
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/**
 * Floating glass bottom bar. Kept to a single translucent surface so it stays cheap to draw
 * during list scrolling.
 */
@Composable
fun GlassNavigationBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .background(extras.navFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navItems.forEach { item ->
            NavBarItem(
                item = item,
                selected = currentRoute == item.route,
                onClick = { onSelect(item.route) },
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavBarItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val extras = LocalAuroraExtras.current
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        label = "navItemBg",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navItemTint",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (selected) {
            Spacer(modifier = Modifier.height(1.dp))
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = if (selected) 16.dp else 0.dp, height = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

/** Side rail used from 600dp upwards; labels appear once the rail can afford them. */
@Composable
fun GlassNavigationRail(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    wide: Boolean,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            .width(if (wide) 104.dp else 84.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .clip(shape)
            .background(extras.navFill, shape)
            .border(BorderStroke(1.dp, extras.glassBorder), shape)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Icon(
                imageVector = SnipIcons.type(com.synfusion.snipnote.domain.model.NoteType.PROMPT),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            if (wide) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("SnipNote", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        navItems.forEach { item ->
            RailItem(item = item, selected = currentRoute == item.route, wide = wide) { onSelect(item.route) }
        }
    }
}

@Composable
private fun RailItem(item: NavItem, selected: Boolean, wide: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        label = "railItemBg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (wide) Arrangement.Start else Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        if (wide) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
