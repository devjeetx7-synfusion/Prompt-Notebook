package com.synfusion.snipnote.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.browse.NoteListScreen
import com.synfusion.snipnote.ui.browse.NoteListViewModel
import com.synfusion.snipnote.ui.categories.CategoriesScreen
import com.synfusion.snipnote.ui.components.AuroraBackground
import com.synfusion.snipnote.ui.detail.NoteDetailScreen
import com.synfusion.snipnote.ui.editor.EditorScreen
import com.synfusion.snipnote.ui.home.HomeScreen
import com.synfusion.snipnote.ui.safe.SafeScreen
import com.synfusion.snipnote.ui.search.SearchScreen
import com.synfusion.snipnote.ui.settings.SettingsScreen

/**
 * Phone layout: glass bottom navigation. Tablet / landscape: navigation rail.
 * The breakpoint mirrors Material guidance (840dp expanded) with a 600dp rail tier.
 */
@Composable
fun SnipNoteRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    AuroraBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 840.dp
            val medium = maxWidth >= 600.dp
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!expanded) {
                        GlassNavigationBar(
                            currentRoute = currentDestination?.route,
                            onSelect = { route -> navController.navigateTopLevel(route) },
                        )
                    }
                },
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (expanded) {
                        GlassNavigationRail(
                            currentRoute = currentDestination?.route,
                            onSelect = { route -> navController.navigateTopLevel(route) },
                            wide = medium,
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        SnipNoteNavHost(
                            navController = navController,
                            container = container,
                            useTwoPane = medium || expanded,
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SnipNoteNavHost(
    navController: NavHostController,
    container: AppContainer,
    useTwoPane: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { it / 24 }) },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { it / 24 }) },
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                container = container,
                twoPane = useTwoPane,
                onOpenNote = { id -> navController.navigate(Destinations.detail(id)) },
                onCreateNote = { type -> navController.navigate(Destinations.editor(type = type)) },
                onQuickCapture = { type -> navController.navigate(Destinations.editor(type = type)) },
                onOpenSearch = { navController.navigate(Destinations.SEARCH) },
                onOpenList = { preset, categoryId, type ->
                    navController.navigate(Destinations.noteList(preset = preset, categoryId = categoryId, type = type))
                },
                onOpenCategories = { navController.navigateTopLevel(Destinations.CATEGORIES) },
                onOpenSafe = { navController.navigateTopLevel(Destinations.SAFE) },
                onOpenSettings = { navController.navigateTopLevel(Destinations.SETTINGS) },
            )
        }

        composable(Destinations.CATEGORIES) {
            CategoriesScreen(
                container = container,
                twoPane = useTwoPane,
                onOpenCategory = { id -> navController.navigate(Destinations.noteList(categoryId = id)) },
                onOpenTag = { id -> navController.navigate(Destinations.noteList(tagId = id)) },
                onOpenAll = { navController.navigate(Destinations.noteList()) },
            )
        }

        composable(Destinations.SAFE) {
            SafeScreen(
                container = container,
                onOpenNote = { id -> navController.navigate(Destinations.detail(id, secure = true)) },
                onCreateNote = { type -> navController.navigate(Destinations.editor(secure = true, type = type)) },
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(container = container)
        }

        composable(Destinations.SEARCH) {
            SearchScreen(
                container = container,
                onOpenNote = { id -> navController.navigate(Destinations.detail(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.NOTE_LIST,
            arguments = listOf(
                navArgument(Destinations.ARG_PRESET) { type = NavType.StringType; defaultValue = "all" },
                navArgument(Destinations.ARG_CATEGORY_ID) { type = NavType.LongType; defaultValue = -1L },
                navArgument(Destinations.ARG_TYPE) { type = NavType.StringType; defaultValue = "-" },
                navArgument(Destinations.ARG_TAG_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val viewModel: NoteListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = SnipViewModelFactories.noteList(container),
            )
            val categoryId = entry.arguments?.getLong(Destinations.ARG_CATEGORY_ID)?.takeIf { it > 0 }
            val tagId = entry.arguments?.getLong(Destinations.ARG_TAG_ID)?.takeIf { it > 0 }
            val typeKey = entry.arguments?.getString(Destinations.ARG_TYPE).takeUnless { it == null || it == "-" }
            NoteListScreen(
                viewModel = viewModel,
                container = container,
                preset = entry.arguments?.getString(Destinations.ARG_PRESET) ?: "all",
                categoryId = categoryId,
                tagId = tagId,
                typeKey = typeKey,
                onBack = { navController.popBackStack() },
                onOpenNote = { id -> navController.navigate(Destinations.detail(id)) },
                onCreateNote = { navController.navigate(Destinations.editor()) },
            )
        }

        composable(
            route = Destinations.EDITOR,
            arguments = listOf(
                navArgument(Destinations.ARG_NOTE_ID) { type = NavType.LongType; defaultValue = -1L },
                navArgument(Destinations.ARG_SECURE) { type = NavType.BoolType; defaultValue = false },
                navArgument(Destinations.ARG_TYPE) { type = NavType.StringType; defaultValue = "-" },
            ),
        ) { entry ->
            EditorScreen(
                container = container,
                secure = entry.arguments?.getBoolean(Destinations.ARG_SECURE) ?: false,
                onBack = { navController.popBackStack() },
                onSaved = { id, isSecure ->
                    if (entry.arguments?.getLong(Destinations.ARG_NOTE_ID)?.let { it > 0 } == true) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Destinations.detail(id, isSecure)) {
                            popUpTo(Destinations.EDITOR) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(
            route = Destinations.DETAIL,
            arguments = listOf(
                navArgument(Destinations.ARG_NOTE_ID) { type = NavType.LongType },
                navArgument(Destinations.ARG_SECURE) { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            NoteDetailScreen(
                container = container,
                secure = entry.arguments?.getBoolean(Destinations.ARG_SECURE) ?: false,
                onBack = { navController.popBackStack() },
                onEdit = { id, isSecure ->
                    navController.navigate(Destinations.editor(noteId = id, secure = isSecure))
                },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}

/** Navigation item model shared by the bottom bar and the rail. */
data class NavItem(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

val navItems: List<NavItem> = listOf(
    NavItem(Destinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Destinations.CATEGORIES, "Categories", Icons.Filled.GridView, Icons.Outlined.GridView),
    NavItem(Destinations.SAFE, "Safe", Icons.Filled.Lock, Icons.Outlined.Lock),
    NavItem(Destinations.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

fun androidx.navigation.NavDestination?.isSelected(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
