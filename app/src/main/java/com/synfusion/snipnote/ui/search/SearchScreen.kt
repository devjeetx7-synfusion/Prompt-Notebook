package com.synfusion.snipnote.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.ui.SnipViewModelFactories
import com.synfusion.snipnote.ui.components.EmptyState
import com.synfusion.snipnote.ui.components.NoteFilterBar
import com.synfusion.snipnote.ui.components.NoteRow
import com.synfusion.snipnote.ui.components.ResultSummary
import com.synfusion.snipnote.ui.components.SearchField

/**
 * Searches titles and content across ordinary notes only. Safe rows are filtered out in SQL,
 * so they cannot appear here even while the Safe workspace is unlocked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SnipViewModelFactories.search(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                onSearch = { keyboard?.hide() },
                modifier = Modifier.focusRequester(focusRequester),
            )
            Column(modifier = Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteFilterBar(
                    selectedType = state.filter.type,
                    favoritesOnly = state.filter.favoritesOnly,
                    pinnedOnly = state.filter.pinnedOnly,
                    categories = state.categories,
                    selectedCategoryId = state.filter.categoryId,
                    sort = state.filter.sort,
                    onTypeSelected = viewModel::setType,
                    onToggleFavorites = viewModel::toggleFavoritesOnly,
                    onTogglePinned = viewModel::togglePinnedOnly,
                    onCategorySelected = viewModel::setCategory,
                    onSortSelected = viewModel::setSort,
                    onClear = {
                        viewModel.clearFilters()
                        viewModel.onQueryChange("")
                    },
                )
                if (state.hasQuery) ResultSummary(state.results.size, "result")
            }

            when {
                !state.hasQuery -> EmptyState(
                    icon = Icons.Outlined.Tune,
                    title = "Search everything you saved",
                    message = "Type a word from a title or the body of a note. Combine with type, category, favorite and pinned filters.",
                )
                state.results.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No matches",
                    message = "Nothing matches \"${state.query.ifBlank { "these filters" }}\". Try a shorter word or clear the filters.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.results, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                        )
                    }
                }
            }
        }
    }
}
