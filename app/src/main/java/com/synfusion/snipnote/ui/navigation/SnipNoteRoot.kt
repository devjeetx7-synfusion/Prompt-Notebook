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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
 * Root navigation.
 *
 * Phones get the floating glass pill (only on the four top-level destinations); wider layouts get
 * the glass rail. The single central "+" opens the editor immediately – there is no type chooser,
 * and the editor defaults to a private item while the Safe tab is active.
 */
@Composable
fun SnipNoteRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showChrome = Destinations.isTopLevel(currentRoute)

    fun openEditor(noteId: Long? = null, secure: Boolean = false) {
        navController.navigate(Destinations.editor(noteId = noteId, secure = secure))
    }

    AuroraBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 600dp is the Material compact/medium breakpoint: below it the pill is used, above
            // it the rail replaces the pill so the two never appear together.
            val medium = maxWidth >= 600.dp
            val wide = maxWidth >= 840.dp
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!medium && showChrome) {
                        FloatingNavPill(
                            currentRoute = currentRoute,
                            onSelect = { route -> navController.navigateTopLevel(route) },
                            onAdd = { openEditor(secure = currentRoute == Destinations.SAFE) },
                        )
                    }
                },
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (medium) {
                        GlassNavigationRail(
                            currentRoute = currentRoute,
                            onSelect = { route -> navController.navigateTopLevel(route) },
                            onAdd = { openEditor(secure = currentRoute == Destinations.SAFE) },
                            wide = wide,
                        )
                    }
                    NavHostContent(
                        navController = navController,
                        container = container,
                        useTwoPane = medium,
                        modifier = Modifier.weight(1f),
                    )
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
private fun NavHostContent(
    navController: NavHostController,
    container: AppContainer,
    useTwoPane: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        modifier = modifier,
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
                onOpenSearch = { navController.navigate(Destinations.SEARCH) },
                onAdd = { navController.navigate(Destinations.editor()) },
                onOpenSettings = { navController.navigateTopLevel(Destinations.SETTINGS) },
                onEditNote = { id -> navController.navigate(Destinations.editor(noteId = id)) },
            )
        }

        composable(Destinations.CATEGORIES) {
            CategoriesScreen(
                container = container,
                twoPane = useTwoPane,
                onOpenCategory = { id -> navController.navigate(Destinations.noteList(categoryId = id)) },
                onOpenTag = { id -> navController.navigate(Destinations.noteList(tagId = id)) },
                onOpenAll = { navController.navigate(Destinations.noteList()) },
                onOpenFavorites = { navController.navigate(Destinations.noteList(preset = "favorites")) },
                onOpenPinned = { navController.navigate(Destinations.noteList(preset = "pinned")) },
                onAdd = { navController.navigate(Destinations.editor()) },
            )
        }

        composable(Destinations.SAFE) {
            SafeScreen(
                container = container,
                onOpenNote = { id -> navController.navigate(Destinations.detail(id, secure = true)) },
                onEditNote = { id -> navController.navigate(Destinations.editor(noteId = id, secure = true)) },
                onOpenCreateHint = { navController.navigate(Destinations.editor(secure = true)) },
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
                onEditNote = { id -> navController.navigate(Destinations.editor(noteId = id)) },
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
            val viewModel: NoteListViewModel = viewModel(factory = SnipViewModelFactories.noteList(container))
            NoteListScreen(
                container = container,
                viewModel = viewModel,
                preset = entry.arguments?.getString(Destinations.ARG_PRESET) ?: "all",
                categoryId = entry.arguments?.getLong(Destinations.ARG_CATEGORY_ID)?.takeIf { it > 0 },
                tagId = entry.arguments?.getLong(Destinations.ARG_TAG_ID)?.takeIf { it > 0 },
                typeKey = entry.arguments?.getString(Destinations.ARG_TYPE).takeUnless { it == null || it == "-" },
                onBack = { navController.popBackStack() },
                onOpenNote = { id -> navController.navigate(Destinations.detail(id)) },
                onCreateNote = { navController.navigate(Destinations.editor()) },
                onEditNote = { id -> navController.navigate(Destinations.editor(noteId = id)) },
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
            val editingId = entry.arguments?.getLong(Destinations.ARG_NOTE_ID)?.takeIf { it > 0 }
            EditorScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onSaved = { id, isSecure ->
                    if (editingId != null) {
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
