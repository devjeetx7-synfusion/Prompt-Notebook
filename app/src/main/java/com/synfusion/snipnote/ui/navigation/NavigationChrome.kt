package com.synfusion.snipnote.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.ui.theme.LocalAuroraExtras

/** Vertical space every scrollable screen must reserve so the floating pill never hides content. */
val FloatingNavContentPadding = 118.dp

/**
 * Floating glass navigation pill for phones.
 *
 * The pill itself is a translucent rounded surface with side margins and a soft shadow; the parent
 * area stays fully transparent. The central "+" is the single creation entry point in the app and
 * is drawn slightly above the pill so it reads as elevated.
 */
@Composable
fun FloatingNavPill(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = extras.shadow,
                    spotColor = extras.shadow,
                )
                .clip(shape)
                .background(extras.navFill)
                .border(BorderStroke(1.dp, extras.navBorder), shape)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingNavItem(
                item = navItems[0],
                selected = currentRoute == navItems[0].route,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(navItems[0].route) },
            )
            FloatingNavItem(
                item = navItems[1],
                selected = currentRoute == navItems[1].route,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(navItems[1].route) },
            )
            // Reserved slot so the floating "+" sits in the optical centre of the four items.
            Spacer(modifier = Modifier.width(66.dp))
            FloatingNavItem(
                item = navItems[2],
                selected = currentRoute == navItems[2].route,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(navItems[2].route) },
            )
            FloatingNavItem(
                item = navItems[3],
                selected = currentRoute == navItems[3].route,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(navItems[3].route) },
            )
        }

        AddButton(
            onAdd = onAdd,
            modifier = Modifier.offset(y = (-18).dp),
        )
    }
}

/** The single, universal create button. */
@Composable
fun AddButton(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 62.dp,
) {
    val extras = LocalAuroraExtras.current
    val scale by animateFloatAsState(targetValue = 1f, label = "addScale")
    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(18.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)),
                ),
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = if (extras.isDark) 0.14f else 0.55f)), CircleShape)
            .clickable(role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = "Create new item" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size / 2.2f),
        )
    }
}

@Composable
private fun FloatingNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(width = if (selected) 14.dp else 0.dp, height = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

/**
 * Navigation rail for tablets / landscape. Shares the same four destinations plus the universal
 * add button, so no screen ever needs a second creation control.
 */
@Composable
fun GlassNavigationRail(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = LocalAuroraExtras.current
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
            .width(if (wide) 104.dp else 84.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .shadow(18.dp, shape, ambientColor = extras.shadow, spotColor = extras.shadow)
            .clip(shape)
            .background(extras.navFill)
            .border(BorderStroke(1.dp, extras.navBorder), shape)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(
                imageVector = SnipIconsLogo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            if (wide) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("SnipNote", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        AddButton(onAdd = onAdd, size = 46.dp, modifier = Modifier.padding(bottom = 6.dp))
        navItems.forEach { item ->
            RailItem(item = item, selected = currentRoute == item.route, wide = wide) { onSelect(item.route) }
        }
    }
}

private val SnipIconsLogo: ImageVector
    get() = Icons.Filled.GridView

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

data class NavItem(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val navItems: List<NavItem> = listOf(
    NavItem(Destinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Destinations.CATEGORIES, "Categories", Icons.Filled.GridView, Icons.Outlined.GridView),
    NavItem(Destinations.SAFE, "Safe", Icons.Filled.Lock, Icons.Outlined.Lock),
    NavItem(Destinations.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)
