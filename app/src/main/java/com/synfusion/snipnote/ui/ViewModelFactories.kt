package com.synfusion.snipnote.ui

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.synfusion.snipnote.AppContainer
import com.synfusion.snipnote.ui.browse.NoteListViewModel
import com.synfusion.snipnote.ui.categories.CategoriesViewModel
import com.synfusion.snipnote.ui.detail.DetailViewModel
import com.synfusion.snipnote.ui.editor.EditorViewModel
import com.synfusion.snipnote.ui.home.HomeViewModel
import com.synfusion.snipnote.ui.safe.SafeViewModel
import com.synfusion.snipnote.ui.search.SearchViewModel
import com.synfusion.snipnote.ui.settings.SettingsViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Explicit factories keep ViewModels free of Android context types and make every dependency
 * visible at the composition site.
 */
object SnipViewModelFactories {

    fun home(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(container.noteRepository, container.categoryRepository) }
    }

    fun noteList(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            NoteListViewModel(container.noteRepository, createSavedStateHandle(), container.settingsRepository)
        }
    }

    fun search(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { SearchViewModel(container.noteRepository, container.categoryRepository) }
    }

    fun editor(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            EditorViewModel(container.noteRepository, container.categoryRepository, createSavedStateHandle())
        }
    }

    fun detail(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            DetailViewModel(container.noteRepository, container.noteRepository, createSavedStateHandle())
        }
    }

    fun categories(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { CategoriesViewModel(container.categoryRepository, container.tagRepository) }
    }

    fun safe(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { SafeViewModel(container.noteRepository, container.safeSession) }
    }

    fun settings(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(container.settingsRepository, container.backupRepository) }
    }

    /** Exposed so tests can build a factory around an already constructed ViewModel. */
    fun <T : androidx.lifecycle.ViewModel> of(factory: (CreationExtras) -> T): ViewModelProvider.Factory =
        viewModelFactory { initializer { factory(this) } }
}
